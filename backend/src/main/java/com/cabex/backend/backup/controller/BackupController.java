package com.cabex.backend.backup.controller;

import com.cabex.backend.backup.dto.BackupFileResponse;
import com.cabex.backend.backup.dto.BackupInfosResponse;
import com.cabex.backend.backup.dto.RestoreResponse;
import com.cabex.backend.backup.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * API de sauvegarde et de restauration de la base de données.
 * L'accès est réservé aux profils disposant des permissions BACKUP_*.
 */
@RestController
@RequestMapping("/api/admin/backup")
@Tag(name = "Sauvegarde", description = "Sauvegarde hors ligne, téléchargement et restauration de la base")
@SecurityRequirement(name = "bearerAuth")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BACKUP_VIEW')")
    @Operation(summary = "Lister les sauvegardes disponibles sur le serveur")
    public ResponseEntity<List<BackupFileResponse>> lister() {
        return ResponseEntity.ok(service.lister());
    }

    @GetMapping("/infos")
    @PreAuthorize("hasAuthority('BACKUP_VIEW')")
    @Operation(summary = "Informations de configuration (base, rétention, planification)")
    public ResponseEntity<BackupInfosResponse> infos() {
        return ResponseEntity.ok(service.infos());
    }

    @GetMapping("/download")
    @PreAuthorize("hasAuthority('BACKUP_CREATE')")
    @Operation(summary = "Télécharger une sauvegarde ponctuelle (générée à la volée)")
    public ResponseEntity<StreamingResponseBody> telecharger(
            @RequestParam(name = "chiffre", defaultValue = "false") boolean chiffre,
            @RequestHeader(name = "X-Backup-Passphrase", required = false) String phrase) {
        BackupService.SauvegardeEphemere ephemere = service.preparerTelechargement(chiffre, phrase);
        StreamingResponseBody corps = sortie -> {
            try {
                Files.copy(ephemere.chemin(), sortie);
            } finally {
                try {
                    Files.deleteIfExists(ephemere.chemin());
                } catch (IOException ignore) {
                    // fichier temporaire ; nettoyage best-effort
                }
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(ephemere.nomFichier()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(corps);
    }

    @GetMapping("/{nom}")
    @PreAuthorize("hasAuthority('BACKUP_VIEW')")
    @Operation(summary = "Télécharger une sauvegarde conservée sur le serveur")
    public ResponseEntity<Resource> charger(@PathVariable String nom) {
        Resource ressource = service.charger(nom);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(nom))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(ressource);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BACKUP_CREATE')")
    @Operation(summary = "Créer et conserver une sauvegarde manuelle sur le serveur")
    public ResponseEntity<BackupFileResponse> creer() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerSauvegarde("manuel"));
    }

    @DeleteMapping("/{nom}")
    @PreAuthorize("hasAuthority('BACKUP_DELETE')")
    @Operation(summary = "Supprimer une sauvegarde du serveur")
    public ResponseEntity<Void> supprimer(@PathVariable String nom) {
        service.supprimer(nom);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BACKUP_RESTORE')")
    @Operation(summary = "Restaurer la base à partir d'un fichier de sauvegarde")
    public ResponseEntity<RestoreResponse> restaurer(
            @RequestPart("file") MultipartFile fichier,
            @RequestParam(name = "phrase", required = false) String phrase) {
        return ResponseEntity.ok(service.restaurer(fichier, phrase));
    }

    private String disposition(String nom) {
        return ContentDisposition.attachment()
                .filename(nom, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
