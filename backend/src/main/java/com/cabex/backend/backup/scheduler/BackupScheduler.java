package com.cabex.backend.backup.scheduler;

import com.cabex.backend.backup.service.BackupService;
import com.cabex.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Sauvegarde automatique planifiée de la base de données, suivie de la purge
 * des archives au-delà de la durée de rétention configurée.
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;
    private final NotificationService notificationService;
    private LocalDate derniereExecution;

    public BackupScheduler(BackupService backupService, NotificationService notificationService) {
        this.backupService = backupService;
        this.notificationService = notificationService;
    }

    /**
     * Vérifie chaque minute si l'heure configurée dans l'administration
     * (HEURE_SAUVEGARDE_AUTOMATIQUE) est atteinte. La valeur est relue à chaque
     * passage : une modification s'applique donc sans redémarrage.
     */
    @Scheduled(cron = "0 * * * * *")
    public void verifierHeureSauvegarde() {
        verifierHeureSauvegarde(
                LocalTime.now().withSecond(0).withNano(0),
                LocalDate.now());
    }

    public void verifierHeureSauvegarde(LocalTime maintenant, LocalDate jour) {
        if (jour.equals(derniereExecution)) {
            return;
        }
        if (!maintenant.equals(backupService.heureSauvegarde())) {
            return;
        }
        derniereExecution = jour;
        sauvegarderAutomatiquement();
    }

    public void sauvegarderAutomatiquement() {
        try {
            var fichier = backupService.creerSauvegarde("auto");
            log.info("Sauvegarde automatique générée : {} ({} octets)",
                    fichier.nomFichier(), fichier.tailleOctets());
            notifier("Sauvegarde automatique terminée",
                    "La sauvegarde " + fichier.nomFichier() + " a été générée avec succès ("
                            + fichier.tailleOctets() + " octets).");
            backupService.purger();
        } catch (Exception e) {
            log.error("Échec de la sauvegarde automatique : {}", e.getMessage(), e);
            notifier("Échec de la sauvegarde automatique",
                    "La sauvegarde automatique a échoué : " + e.getMessage());
        }
    }

    private void notifier(String titre, String message) {
        try {
            notificationService.notifierAlerteSysteme(titre, message, "BACKUP", null);
        } catch (Exception e) {
            log.warn("Notification de sauvegarde non créée : {}", e.getMessage());
        }
    }
}
