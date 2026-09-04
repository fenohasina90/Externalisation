package com.cabex.backend.notification.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Génère quotidiennement des alertes de notifications pour les échéances
 * proches (sous 3 jours) et les échéances en retard.
 */
@Component
public class NotificationScheduler {

    private static final int JOURS_ALERTE = 3;

    private final EcheancePaiementRepository echeanceRepository;
    private final NotificationService notificationService;

    public NotificationScheduler(EcheancePaiementRepository echeanceRepository,
                                 NotificationService notificationService) {
        this.echeanceRepository = echeanceRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void verifierEcheances() {
        LocalDate aujourdHui = LocalDate.now();

        for (EcheancePaiement echeance : echeanceRepository.findEnRetard(aujourdHui)) {
            DemandeService demande = echeance.getProposPaiement().getDemande();
            if (demande != null) {
                notificationService.notifierEcheanceEnRetard(demande);
            }
        }

        List<EcheancePaiement> aVenir = echeanceRepository.findAVenir(aujourdHui);
        LocalDate limite = aujourdHui.plusDays(JOURS_ALERTE);
        for (EcheancePaiement echeance : aVenir) {
            if (!echeance.getDateEcheance().isAfter(limite)) {
                DemandeService demande = echeance.getProposPaiement().getDemande();
                if (demande != null) {
                    int jours = (int) ChronoUnit.DAYS.between(aujourdHui, echeance.getDateEcheance());
                    notificationService.notifierEcheanceProche(demande, Math.max(jours, 1));
                }
            }
        }
    }
}
