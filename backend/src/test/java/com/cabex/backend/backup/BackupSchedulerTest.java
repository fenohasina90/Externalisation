package com.cabex.backend.backup;

import com.cabex.backend.backup.dto.BackupFileResponse;
import com.cabex.backend.backup.scheduler.BackupScheduler;
import com.cabex.backend.backup.service.BackupService;
import com.cabex.backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** L'heure de sauvegarde est lue depuis la configuration à chaque vérification. */
@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock
    private BackupService backupService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BackupScheduler scheduler;

    private BackupFileResponse fichier() {
        return new BackupFileResponse("backup_2026-10-21_021500_auto.sql.gz",
                1024, LocalDateTime.of(2026, 10, 21, 2, 15), "auto", false);
    }

    @Test
    void sauvegardeALHeureConfiguree() {
        when(backupService.heureSauvegarde()).thenReturn(LocalTime.of(2, 15));
        when(backupService.creerSauvegarde("auto")).thenReturn(fichier());

        scheduler.verifierHeureSauvegarde(LocalTime.of(2, 15), LocalDate.of(2026, 10, 21));

        verify(backupService).creerSauvegarde("auto");
        verify(backupService).purger();
        verify(notificationService).notifierAlerteSysteme(
                eq("Sauvegarde automatique terminée"), anyString(), eq("BACKUP"), isNull());
    }

    @Test
    void pasDeSauvegardeHorsHeure() {
        when(backupService.heureSauvegarde()).thenReturn(LocalTime.of(2, 15));

        scheduler.verifierHeureSauvegarde(LocalTime.of(2, 16), LocalDate.of(2026, 10, 21));

        verify(backupService, never()).creerSauvegarde(anyString());
    }

    @Test
    void uneSeuleSauvegardeParJour() {
        when(backupService.heureSauvegarde()).thenReturn(LocalTime.of(2, 15));
        when(backupService.creerSauvegarde("auto")).thenReturn(fichier());
        LocalDate jour = LocalDate.of(2026, 10, 21);

        scheduler.verifierHeureSauvegarde(LocalTime.of(2, 15), jour);
        scheduler.verifierHeureSauvegarde(LocalTime.of(2, 15), jour);

        verify(backupService, times(1)).creerSauvegarde("auto");
    }
}
