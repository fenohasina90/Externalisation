package com.cabex.backend.notification.service;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Génère quotidiennement des alertes de notifications pour les échéances
 * proches (seuil lu depuis la configuration JOURS_AVANCE_ALERTE_ECHEANCE,
 * 3 jours par défaut) et les échéances en retard.
 */
@Component
public class NotificationScheduler {

    private static final int JOURS_ALERTE_DEFAUT = 3;

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

    @Scheduled(cron = "0 0 7 * * *")
    public void verifierEcheances() {
        LocalDate aujourdHui = LocalDate.now();

        for (EcheancePaiement echeance : echeanceRepository.findEnRetard(aujourdHui)) {
            notificationService.notifierEcheanceEnRetard(echeance);
        }

        List<EcheancePaiement> aVenir = echeanceRepository.findAVenir(aujourdHui);
        int joursAlerte = Math.max(configurationService.lireEntier(
                "JOURS_AVANCE_ALERTE_ECHEANCE", JOURS_ALERTE_DEFAUT), 1);
        LocalDate limite = aujourdHui.plusDays(joursAlerte);
        for (EcheancePaiement echeance : aVenir) {
            if (!echeance.getDateEcheance().isAfter(limite)) {
                int jours = (int) ChronoUnit.DAYS.between(aujourdHui, echeance.getDateEcheance());
                notificationService.notifierEcheanceProche(echeance, Math.max(jours, 1));
            }
        }
    }
}
