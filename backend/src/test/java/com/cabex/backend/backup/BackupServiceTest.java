package com.cabex.backend.backup;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.backup.config.BackupProperties;
import com.cabex.backend.backup.crypto.BackupCryptoService;
import com.cabex.backend.backup.service.BackupService;
import com.cabex.backend.backup.service.BackupStorageService;
import com.cabex.backend.backup.service.MysqlProcessRunner;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.exception.BackupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupProperties proprietes;
    @Mock
    private BackupStorageService stockage;
    @Mock
    private MysqlProcessRunner runner;
    @Mock
    private BackupCryptoService crypto;
    @Mock
    private AuditService audit;
    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private BackupService service;

    @TempDir
    private Path dossier;

    @Test
    void echecDuDumpSupprimeLeFichierPartiel() throws Exception {
        Path destination = dossier.resolve("backup_2026-10-20_160000_manuel.sql.gz");
        when(stockage.nomSauvegarde("manuel", false)).thenReturn(destination.getFileName().toString());
        when(stockage.resoudre(destination.getFileName().toString())).thenReturn(destination);
        doAnswer(invocation -> {
            OutputStream sortie = invocation.getArgument(0);
            sortie.write(new byte[] {1, 2, 3});
            throw new BackupException("La sauvegarde a échoué (code 2)");
        }).when(runner).dump(any(OutputStream.class), eq(false), isNull());

        assertThrows(BackupException.class, () -> service.creerSauvegarde("manuel"));
        assertFalse(Files.exists(destination), "le fichier partiel doit être supprimé");
    }
}
