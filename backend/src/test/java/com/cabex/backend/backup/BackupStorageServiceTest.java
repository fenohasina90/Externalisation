package com.cabex.backend.backup;

import com.cabex.backend.backup.config.BackupProperties;
import com.cabex.backend.backup.service.BackupStorageService;
import com.cabex.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Noms de fichiers, protection contre la traversée de répertoire et purge par rétention.
 */
class BackupStorageServiceTest {

    private BackupStorageService stockage(Path dossier) {
        BackupProperties proprietes = new BackupProperties(
                dossier.toString(), 14, "0 0 2 * * *", "localhost", 3306,
                "gestion_externalisation", "root", "", "mysqldump", "mysql", 1024);
        return new BackupStorageService(proprietes);
    }

    @Test
    void nomGenereAuBonFormat(@TempDir Path dossier) {
        BackupStorageService stockage = stockage(dossier);
        String nom = stockage.nomSauvegarde("manuel", false);
        assertTrue(nom.matches("^backup_\\d{4}-\\d{2}-\\d{2}_\\d{6}_manuel\\.sql\\.gz$"));
        assertTrue(stockage.nomTelechargement(true).matches("^backup_\\d{4}-\\d{2}-\\d{2}_\\d{4}\\.sql\\.gz\\.enc$"));
    }

    @Test
    void nomsDangereuxRefuses(@TempDir Path dossier) {
        BackupStorageService stockage = stockage(dossier);
        assertThrows(BadRequestException.class, () -> stockage.resoudre("../application.properties"));
        assertThrows(BadRequestException.class, () -> stockage.resoudre("backup_2026-01-01_000000.sql"));
        assertThrows(BadRequestException.class, () -> stockage.resoudre("fichier.txt"));
        assertThrows(BadRequestException.class, () -> stockage.resoudre(null));
    }

    @Test
    void listeEtPurge(@TempDir Path dossier) throws IOException {
        BackupStorageService stockage = stockage(dossier);
        Path recente = Files.createFile(dossier.resolve("backup_2026-10-19_213000_manuel.sql.gz"));
        Path ancienne = Files.createFile(dossier.resolve("backup_2020-01-01_000000_auto.sql.gz"));
        Files.setLastModifiedTime(ancienne, java.nio.file.attribute.FileTime.from(
                Instant.now().minus(40, ChronoUnit.DAYS)));

        List<String> noms = stockage.lister().stream().map(f -> f.nomFichier()).toList();
        assertEquals(2, noms.size());
        assertEquals(recente.getFileName().toString(), noms.get(0));

        int supprimees = stockage.purger(14);

        assertEquals(1, supprimees);
        assertEquals(1, stockage.lister().size());
        assertTrue(Files.exists(recente));
    }
}
