package com.cabex.backend.backup.scheduler;

import com.cabex.backend.backup.service.BackupService;
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

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Scheduled(cron = "${backup.cron:0 0 2 * * *}")
    public void sauvegarderAutomatiquement() {
        try {
            var fichier = backupService.creerSauvegarde("auto");
            log.info("Sauvegarde automatique générée : {} ({} octets)",
                    fichier.nomFichier(), fichier.tailleOctets());
            backupService.purger();
        } catch (Exception e) {
            log.error("Échec de la sauvegarde automatique : {}", e.getMessage(), e);
        }
    }
}
