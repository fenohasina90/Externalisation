package com.cabex.backend.notification.service;

import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Génère régulièrement des alertes de notifications pour les échéances
 * proches (seuil lu depuis la configuration JOURS_AVANCE_ALERTE_ECHEANCE,
 * 3 jours par défaut) et les échéances en retard.
 *
 * <p>La vérification est exécutée après le démarrage puis toutes les heures :
 * elle ne dépend donc pas du fait que l'application soit allumée à une heure
 * précise. Les notifications sont dédupliquées par utilisateur, type et
 * échéance.</p>
 */
@Component
public class NotificationScheduler {

    private final EcheancePaiementRepository echeanceRepository;
    private final NotificationService notificationService;
    private final ConfigurationService configurationService;

    public NotificationScheduler(EcheancePaiementRepository echeanceRepository,
                                 NotificationService notificationService,
                                 ConfigurationService configurationService) {
        this.echeanceRepository = echeanceRepository;
        this.notificationService = notificationService;
        this.configurationService = configurationService;
    }

    @Scheduled(
            initialDelayString = "${app.notification.echeance-initial-delay-ms:30000}",
            fixedDelayString = "${app.notification.echeance-check-delay-ms:3600000}")
    @Transactional
    public void verifierEcheances() {
        LocalDate aujourdHui = LocalDate.now();

        for (EcheancePaiement echeance : echeanceRepository.findEnRetard(aujourdHui)) {
            notificationService.notifierEcheanceEnRetard(echeance);
        }

        List<EcheancePaiement> aVenir = echeanceRepository.findAVenir(aujourdHui);
        int joursAlerte = Math.max(configurationService.lireEntier(
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE,
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE_DEFAUT), 1);
        LocalDate limite = aujourdHui.plusDays(joursAlerte);
        for (EcheancePaiement echeance : aVenir) {
            if (!echeance.getDateEcheance().isAfter(limite)) {
                int jours = (int) ChronoUnit.DAYS.between(aujourdHui, echeance.getDateEcheance());
                notificationService.notifierEcheanceProche(echeance, jours);
            }
        }
    }
}
