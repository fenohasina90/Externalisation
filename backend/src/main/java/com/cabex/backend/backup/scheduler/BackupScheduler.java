package com.cabex.backend.backup.scheduler;

import com.cabex.backend.backup.service.BackupService;
import com.cabex.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sauvegarde automatique planifiée de la base de données, suivie de la purge
 * des archives au-delà de la durée de rétention configurée.
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;
    private final NotificationService notificationService;

    public BackupScheduler(BackupService backupService, NotificationService notificationService) {
        this.backupService = backupService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${backup.cron:0 0 2 * * *}")
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
