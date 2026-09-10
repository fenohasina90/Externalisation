package com.cabex.backend.document.controller;

import com.cabex.backend.document.dto.DocumentResponse;
import com.cabex.backend.document.dto.FichierDocument;
import com.cabex.backend.document.service.DocumentGestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Documents", description = "Pièces jointes et génération de documents PDF")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentGestionService documentService;

    public DocumentController(DocumentGestionService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/demandes/{id}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_VIEW')")
    @Operation(summary = "Lister les documents d'une demande")
    public ResponseEntity<List<DocumentResponse>> listByDemande(@PathVariable Integer id) {
        return ResponseEntity.ok(documentService.listByDemande(id));
    }

    @PostMapping(value = "/demandes/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_UPLOAD')")
    @Operation(summary = "Uploader un document pour une demande",
            description = "Types acceptés : pdf, images, doc(x), xls(x), txt. Taille max : 10 Mo.")
    public ResponseEntity<DocumentResponse> upload(@PathVariable Integer id,
                                                   @RequestPart("fichier") MultipartFile fichier,
                                                   @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(id, fichier, description));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_VIEW')")
    @Operation(summary = "Télécharger un document")
    public ResponseEntity<byte[]> download(@PathVariable Integer id) {
        FichierDocument f = documentService.telecharger(id);
        String contentDisposition = "attachment; filename*=UTF-8''"
                + URLEncoder.encode(f.nomOriginal(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(f.contenu());
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_DELETE')")
    @Operation(summary = "Supprimer un document (réservé SUPER_ADMIN)",
            description = "Supprime le fichier disque et l'enregistrement")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/demandes/{id}/documents/recapitulatif")
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_VIEW')")
    @Operation(summary = "Générer le récapitulatif de demande (PDF)",
            description = "Par défaut envoie le PDF en téléchargement. Avec sauvegarder=true, l'enregistre comme document.")
    public ResponseEntity<?> recapitulatif(@PathVariable Integer id,
                                           @RequestParam(defaultValue = "false") boolean sauvegarder) {
        if (sauvegarder) {
            return ResponseEntity.ok(Map.of("document", documentService.sauvegarderRecapitulatif(id)));
        }
        return pdfResponse(documentService.recapitulatifDemande(id), "recapitulatif-demande-" + id + ".pdf");
    }

    @GetMapping("/paiements/{id}/document/recepisse")
    @PreAuthorize("hasAuthority('DOCUMENT_PAIEMENT_VIEW')")
    @Operation(summary = "Générer le reçu de paiement (PDF) — SUPER_ADMIN + ECONOMAT",
            description = "Par défaut envoie le PDF en téléchargement. Avec sauvegarder=true, l'enregistre comme document.")
    public ResponseEntity<?> recuPaiement(@PathVariable Integer id,
                                          @RequestParam(defaultValue = "false") boolean sauvegarder) {
        if (sauvegarder) {
            return ResponseEntity.ok(Map.of("document", documentService.sauvegarderRecu(id)));
        }
        return pdfResponse(documentService.recuPaiement(id), "recu-paiement-" + id + ".pdf");
    }

    @GetMapping("/demandes/{id}/plan-paiement/document")
    @PreAuthorize("hasAuthority('DOCUMENT_DEMANDE_VIEW')")
    @Operation(summary = "Générer le plan de paiement (PDF)",
            description = "Par défaut envoie le PDF en téléchargement. Avec sauvegarder=true, l'enregistre comme document.")
    public ResponseEntity<?> planPaiement(@PathVariable Integer id,
                                          @RequestParam(defaultValue = "false") boolean sauvegarder) {
        if (sauvegarder) {
            return ResponseEntity.ok(Map.of("document", documentService.sauvegarderPlan(id)));
        }
        return pdfResponse(documentService.planPaiement(id), "plan-paiement-demande-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] contenu, String nomFichier) {
        String contentDisposition = "inline; filename*=UTF-8''"
                + URLEncoder.encode(nomFichier, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenu);
    }
}
