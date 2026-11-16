package com.cabex.backend.notification.service;

import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Génère des notifications de retard de paiement pour les factures validées :
 * <ul>
 *   <li><b>Prestation et débours</b> : facture validée non soldée après
 *       {@code JOURS_RETARD_PRESTATION_DEBOURS} jours (défaut 5) ;</li>
 *   <li><b>Abonnement mensuel</b> : facture mensuelle non soldée après
 *       {@code JOURS_RETARD_ABONNEMENT} jours (défaut 7) depuis la date
 *       d'émission de la facture ;</li>
 *   <li><b>Rappel de création mensuelle</b> : J-R avant
 *       {@code JOUR_CREATION_FACTURE_MENSUELLE} (défaut 15) le jour J, un
 *       rappel est également émis. Rappel uniquement — la facture mensuelle
 *       peut toujours être créée à n'importe quelle date.</li>
 * </ul>
 *
 * <p>Dédupliqué par utilisateur et par facture (ou par rappel mensuel) :
 * l'alerte est émise une fois, jusqu'à résolution du retard.</p>
 */
@Component
public class PaiementAlertesScheduler {

    private final FactureValideeRepository factureValideeRepository;
    private final PaiementRepository paiementRepository;
    private final NotificationService notificationService;
    private final ConfigurationService configurationService;

    public PaiementAlertesScheduler(FactureValideeRepository factureValideeRepository,
                                    PaiementRepository paiementRepository,
                                    NotificationService notificationService,
                                    ConfigurationService configurationService) {
        this.factureValideeRepository = factureValideeRepository;
        this.paiementRepository = paiementRepository;
        this.notificationService = notificationService;
        this.configurationService = configurationService;
    }

    @Scheduled(
            initialDelayString = "${app.notification.alerte-initial-delay-ms:30000}",
            fixedDelayString = "${app.notification.alerte-check-delay-ms:3600000}")
    @Transactional
    public void verifierAlertes() {
        LocalDate aujourdHui = LocalDate.now();
        verifierPrestationsEtDebours(aujourdHui);
        verifierAbonnements(aujourdHui);
        verifierRappelCreationMensuelle(aujourdHui);
    }

    /** Prestation et débours : alerte après validation de la facture validée. */
    private void verifierPrestationsEtDebours(LocalDate aujourdHui) {
        int jours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS,
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS_DEFAUT));
        List<FactureValidee> factures = factureValideeRepository.findFacturesPourAlertesPaiement();
        for (FactureValidee f : factures) {
            if (f.getValideLe() == null) {
                continue;
            }
            LocalDate dateValidation = f.getValideLe().toLocalDate();
            if (aujourdHui.isBefore(dateValidation.plusDays(jours))) {
                // le retard n'est pas encore déclenche pour cette facture
                continue;
            }
            notifierSiRetard(f, jours, designationLigne(f));
        }
    }

    /** Abonnement mensuel : alerte après la date d'émission de la facture. */
    private void verifierAbonnements(LocalDate aujourdHui) {
        int jours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT,
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT_DEFAUT));
        List<FactureValidee> factures = factureValideeRepository.findFacturesMensuellesPourAlerte();
        for (FactureValidee f : factures) {
            if (f.getValideLe() == null) {
                continue;
            }
            LocalDate dateEmission = f.getValideLe().toLocalDate();
            if (aujourdHui.isBefore(dateEmission.plusDays(jours))) {
                continue;
            }
            notifierSiRetard(f, jours, designationLigne(f));
        }
    }

    /** Rappel de création de la facture mensuelle (pré-rappel et jour J). */
    private void verifierRappelCreationMensuelle(LocalDate aujourdHui) {
        int jourCible = Math.max(1, Math.min(31, configurationService.lireEntier(
                ConfigurationKeys.JOUR_CREATION_FACTURE_MENSUELLE,
                ConfigurationKeys.JOUR_CREATION_FACTURE_MENSUELLE_DEFAUT)));
        int joursRappel = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RAPPEL_AVANT_CREATION_MENSUELLE,
                ConfigurationKeys.JOURS_RAPPEL_AVANT_CREATION_MENSUELLE_DEFAUT));

        YearMonth moisCourant = YearMonth.from(aujourdHui);
        LocalDate cible = moisCourant.atDay(Math.min(jourCible, moisCourant.lengthOfMonth()));
        if (cible.isBefore(aujourdHui)) {
            // le mois courant est déjà dépassé — viser le mois prochain
            YearMonth suivant = moisCourant.plusMonths(1);
            cible = suivant.atDay(Math.min(jourCible, suivant.lengthOfMonth()));
        }
        long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(aujourdHui, cible);
        if (joursRappel > 0 && joursRestants == joursRappel) {
            notificationService.notifierRappelCreationMensuelle(cible, false);
        }
        // Rappel également émis le jour J, qu'il y ait pré-rappel ou non.
        if (joursRestants == 0) {
            notificationService.notifierRappelCreationMensuelle(cible, true);
        }
    }

    private void notifierSiRetard(FactureValidee f, int jours, String designation) {
        BigDecimal paye = f.getId() != null
                ? paiementRepository.sommePayeeParFactureValidee(f.getId())
                : BigDecimal.ZERO;
        if (paye == null) {
            paye = BigDecimal.ZERO;
        }
        BigDecimal montant = f.getMontant() != null ? f.getMontant() : BigDecimal.ZERO;
        BigDecimal reste = montant.subtract(paye);
        if (reste.signum() <= 0) {
            return; // soldée, pas de retard
        }
        notificationService.notifierRetardPaiement(f, designation + " (" + reste + " MGA restants)", reste);
    }

    private String designationLigne(FactureValidee f) {
        if (f.getLigneService() == null) {
            return "Facture " + (f.getReferenceFactureValidee() != null
                    ? f.getReferenceFactureValidee() : "");
        }
        var l = f.getLigneService();
        if (com.cabex.backend.demande.service.LigneServiceService.estDebours(l)) {
            return "Débours — " + (l.getDesignationDebours() != null
                    ? l.getDesignationDebours() : "n° " + l.getId());
        }
        if (l.getService() != null && l.getService().getNomService() != null) {
            return l.getService().getNomService();
        }
        return "Service " + l.getId();
    }
}
