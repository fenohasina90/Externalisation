package com.cabex.backend.backup.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.backup.config.BackupProperties;
import com.cabex.backend.backup.crypto.BackupCryptoService;
import com.cabex.backend.backup.dto.BackupFileResponse;
import com.cabex.backend.backup.dto.BackupInfosResponse;
import com.cabex.backend.backup.dto.RestoreResponse;
import com.cabex.backend.exception.BackupException;
import com.cabex.backend.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Orchestration des sauvegardes : génération manuelle ou planifiée, téléchargement
 * (stocké ou éphémère), suppression, purge par rétention et restauration.
 * Chaque action sensible est tracée dans le journal d'audit.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String MODULE = "Sauvegarde";
    private static final String ENTITE = "Sauvegarde";

    private final BackupProperties proprietes;
    private final BackupStorageService stockage;
    private final MysqlProcessRunner runner;
    private final BackupCryptoService crypto;
    private final AuditService audit;

    public BackupService(BackupProperties proprietes,
                         BackupStorageService stockage,
                         MysqlProcessRunner runner,
                         BackupCryptoService crypto,
                         AuditService audit) {
        this.proprietes = proprietes;
        this.stockage = stockage;
        this.runner = runner;
        this.crypto = crypto;
        this.audit = audit;
    }

    public record SauvegardeEphemere(Path chemin, String nomFichier) {
    }

    public List<BackupFileResponse> lister() {
        return stockage.lister();
    }

    public BackupInfosResponse infos() {
        return new BackupInfosResponse(
                proprietes.getDbName(),
                stockage.repertoire().toString(),
                proprietes.getRetentionDays(),
                proprietes.getCron(),
                proprietes.getUploadMaxSize());
    }

    /** Génère une sauvegarde conservée sur le serveur (types : manuel, auto, securite). */
    public BackupFileResponse creerSauvegarde(String type) {
        String nom = stockage.nomSauvegarde(type, false);
        Path destination = stockage.resoudre(nom);
        try (OutputStream sortie = Files.newOutputStream(destination)) {
            runner.dump(sortie, false, null);
        } catch (IOException e) {
            supprimerSilencieusement(destination);
            throw new BackupException("Impossible d'écrire la sauvegarde : " + e.getMessage(), e);
        }
        BackupFileResponse fichier = stockage.decrire(nom);
        audit.log(action(type), MODULE, ENTITE, null, null,
                nom + " (" + fichier.tailleOctets() + " octets)");
        return fichier;
    }

    /** Prépare un téléchargement ponctuel : le dump est généré dans un fichier temporaire. */
    public SauvegardeEphemere preparerTelechargement(boolean chiffre, String phrase) {
        char[] secretaire = validerPhrase(chiffre, phrase);
        String nom = stockage.nomTelechargement(chiffre);
        Path temporaire = stockage.fichierTemporaire(chiffre ? ".sql.gz.enc" : ".sql.gz");
        try (OutputStream sortie = Files.newOutputStream(temporaire)) {
            runner.dump(sortie, chiffre, secretaire);
        } catch (IOException e) {
            supprimerSilencieusement(temporaire);
            throw new BackupException("Impossible de générer la sauvegarde : " + e.getMessage(), e);
        } catch (RuntimeException e) {
            supprimerSilencieusement(temporaire);
            throw e;
        }
        audit.log(AuditActions.BACKUP_DOWNLOAD, MODULE, ENTITE, null, null, nom);
        return new SauvegardeEphemere(temporaire, nom);
    }

    public Resource charger(String nom) {
        Path chemin = stockage.exiger(nom);
        audit.log(AuditActions.BACKUP_DOWNLOAD, MODULE, ENTITE, null, null, nom);
        return new FileSystemResource(chemin);
    }

    public void supprimer(String nom) {
        stockage.supprimer(nom);
        audit.log(AuditActions.BACKUP_DELETE, MODULE, ENTITE, null, nom, null);
    }

    public int purger() {
        int supprimees = stockage.purger(proprietes.getRetentionDays());
        if (supprimees > 0) {
            log.info("Purge des sauvegardes : {} fichier(s) au-delà de {} jours",
                    supprimees, proprietes.getRetentionDays());
        }
        return supprimees;
    }

    public RestoreResponse restaurer(MultipartFile fichier, String phrase) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BadRequestException("Aucun fichier de sauvegarde fourni");
        }
        String nomOrigine = fichier.getOriginalFilename() == null
                ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean chiffre = nomOrigine.endsWith(".enc");
        boolean compresse = nomOrigine.endsWith(".sql.gz") || nomOrigine.endsWith(".sql.gz.enc");
        boolean sql = nomOrigine.endsWith(".sql");
        if (!chiffre && !compresse && !sql) {
            throw new BadRequestException("Format non reconnu (attendu .sql, .sql.gz ou .sql.gz.enc)");
        }

        Path brut = stockage.fichierTemporaire(".upload");
        Path intermediaire = null;
        Path script = null;
        try {
            try (InputStream entree = fichier.getInputStream()) {
                Files.copy(entree, brut, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            Path source = brut;
            if (chiffre) {
                char[] secretaire = validerPhrase(true, phrase);
                intermediaire = stockage.fichierTemporaire(".sql.gz");
                try (InputStream entree = new BufferedInputStream(Files.newInputStream(brut));
                     OutputStream sortie = Files.newOutputStream(intermediaire)) {
                    crypto.dechiffrer(entree, sortie, secretaire);
                }
                source = intermediaire;
            }
            if (compresse) {
                script = stockage.fichierTemporaire(".sql");
                decompresser(source, script);
            } else {
                script = source;
            }

            BackupFileResponse securite = creerSauvegarde("securite");
            runner.restaurer(script);

            audit.log(AuditActions.BACKUP_RESTORE, MODULE, ENTITE, null, null,
                    "Restauration depuis " + (nomOrigine.isBlank() ? "un fichier" : nomOrigine)
                            + " ; sauvegarde de sécurité : " + securite.nomFichier());
            return new RestoreResponse(
                    "Restauration effectuée avec succès. Redémarrez l'application si des données restent en cache.",
                    securite.nomFichier(),
                    LocalDateTime.now());
        } catch (IOException e) {
            throw new BackupException("Impossible de lire le fichier fourni : " + e.getMessage(), e);
        } finally {
            supprimerSilencieusement(brut);
            supprimerSilencieusement(intermediaire);
            if (script != null && !script.equals(brut) && !script.equals(intermediaire)) {
                supprimerSilencieusement(script);
            }
        }
    }

    private void decompresser(Path compresse, Path destination) {
        try (InputStream entree = new BufferedInputStream(Files.newInputStream(compresse));
             GZIPInputStream gzip = new GZIPInputStream(entree);
             OutputStream sortie = Files.newOutputStream(destination)) {
            gzip.transferTo(sortie);
        } catch (IOException e) {
            throw new BadRequestException("La sauvegarde compressée est illisible ou corrompue");
        }
    }

    private char[] validerPhrase(boolean chiffre, String phrase) {
        if (!chiffre) {
            return null;
        }
        if (phrase == null || phrase.length() < 8) {
            throw new BadRequestException("La phrase secrète doit contenir au moins 8 caractères");
        }
        return phrase.toCharArray();
    }

    private String action(String type) {
        return "auto".equals(type) ? AuditActions.BACKUP_AUTO : AuditActions.BACKUP_CREATE;
    }

    private void supprimerSilencieusement(Path chemin) {
        if (chemin == null) {
            return;
        }
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            log.debug("Suppression impossible de {} : {}", chemin, e.getMessage());
        }
    }
}
