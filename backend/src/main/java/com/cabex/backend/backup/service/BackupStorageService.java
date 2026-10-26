package com.cabex.backend.backup.service;

import com.cabex.backend.backup.config.BackupProperties;
import com.cabex.backend.backup.dto.BackupFileResponse;
import com.cabex.backend.exception.BackupException;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Accès au dossier des sauvegardes : génération des noms, résolution sécurisée
 * (protection contre la traversée de répertoire), lecture et purge par rétention.
 */
@Service
public class BackupStorageService {

    private static final Pattern NOM_VALIDE = Pattern.compile(
            "^backup_(\\d{4}-\\d{2}-\\d{2}_\\d{4,6})(?:_(auto|manuel|securite))?\\.sql\\.gz(\\.enc)?$");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final DateTimeFormatter HORODATAGE_COURT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final Path repertoire;

    public BackupStorageService(BackupProperties proprietes) {
        this.repertoire = Paths.get(proprietes.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(repertoire);
        } catch (IOException e) {
            throw new BackupException("Impossible de créer le dossier de sauvegardes : " + repertoire, e);
        }
    }

    public Path repertoire() {
        return repertoire;
    }

    public String nomSauvegarde(String type, boolean chiffre) {
        return "backup_" + LocalDateTime.now().format(HORODATAGE)
                + "_" + type + ".sql.gz" + (chiffre ? ".enc" : "");
    }

    public String nomTelechargement(boolean chiffre) {
        return "backup_" + LocalDateTime.now().format(HORODATAGE_COURT)
                + ".sql.gz" + (chiffre ? ".enc" : "");
    }

    /** Résout un nom de fichier en vérifiant son format et son appartenance au dossier. */
    public Path resoudre(String nom) {
        if (nom == null || !NOM_VALIDE.matcher(nom).matches()) {
            throw new BadRequestException("Nom de sauvegarde invalide");
        }
        Path chemin = repertoire.resolve(nom).normalize();
        if (!chemin.startsWith(repertoire)) {
            throw new BadRequestException("Nom de sauvegarde invalide");
        }
        return chemin;
    }

    public Path exiger(String nom) {
        Path chemin = resoudre(nom);
        if (!Files.isRegularFile(chemin)) {
            throw new ResourceNotFoundException("Sauvegarde introuvable : " + nom);
        }
        return chemin;
    }

    public BackupFileResponse decrire(String nom) {
        Path chemin = exiger(nom);
        try {
            Matcher correspondance = NOM_VALIDE.matcher(nom);
            correspondance.matches();
            String type = correspondance.group(2) == null ? "manuel" : correspondance.group(2);
            boolean chiffre = correspondance.group(3) != null;
            return new BackupFileResponse(
                    nom,
                    Files.size(chemin),
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(chemin).toInstant(), ZoneId.systemDefault()),
                    type,
                    chiffre);
        } catch (IOException e) {
            throw new BackupException("Impossible de lire les informations de la sauvegarde " + nom, e);
        }
    }

    public List<BackupFileResponse> lister() {
        try (Stream<Path> flux = Files.list(repertoire)) {
            return flux
                    .filter(Files::isRegularFile)
                    .map(chemin -> chemin.getFileName().toString())
                    .filter(nom -> NOM_VALIDE.matcher(nom).matches())
                    .map(this::decrire)
                    .sorted(Comparator.comparing(BackupFileResponse::dateCreation).reversed())
                    .toList();
        } catch (IOException e) {
            throw new BackupException("Impossible de lister les sauvegardes", e);
        }
    }

    public void supprimer(String nom) {
        Path chemin = exiger(nom);
        try {
            Files.delete(chemin);
        } catch (IOException e) {
            throw new BackupException("Impossible de supprimer la sauvegarde " + nom, e);
        }
    }

    /** Supprime les sauvegardes dont la date dépasse la durée de rétention. Retourne le nombre supprimé. */
    public int purger(int retentionJours) {
        if (retentionJours <= 0) {
            return 0;
        }
        Instant limite = LocalDateTime.now().minusDays(retentionJours)
                .atZone(ZoneId.systemDefault()).toInstant();
        int supprimees = 0;
        for (BackupFileResponse fichier : lister()) {
            if (fichier.dateCreation().atZone(ZoneId.systemDefault()).toInstant().isBefore(limite)) {
                try {
                    Files.deleteIfExists(repertoire.resolve(fichier.nomFichier()));
                    supprimees++;
                } catch (IOException e) {
                    throw new BackupException("Impossible de purger la sauvegarde " + fichier.nomFichier(), e);
                }
            }
        }
        return supprimees;
    }

    /** Crée un fichier temporaire hors du dossier de sauvegardes (usage restauration). */
    public Path fichierTemporaire(String suffixe) {
        try {
            return Files.createTempFile("cabex_restauration_", suffixe);
        } catch (IOException e) {
            throw new BackupException("Impossible de créer un fichier temporaire", e);
        }
    }
}
