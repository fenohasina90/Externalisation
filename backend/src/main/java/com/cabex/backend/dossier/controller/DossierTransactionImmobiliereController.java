package com.cabex.backend.dossier.controller;

import com.cabex.backend.dossier.dto.CreateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.dto.DossierTransactionImmobiliereResponse;
import com.cabex.backend.dossier.dto.UpdateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.service.DossierTransactionImmobiliereService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demandes/{demandeId}/dossier-transaction-immobiliere")
@Tag(name = "Dossier de transaction immobilière",
        description = "Fiche de collecte des informations d'une transaction immobilière")
@SecurityRequirement(name = "bearerAuth")
public class DossierTransactionImmobiliereController {

    private final DossierTransactionImmobiliereService dossierService;

    public DossierTransactionImmobiliereController(
            DossierTransactionImmobiliereService dossierService) {
        this.dossierService = dossierService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOSSIER_VIEW')")
    @Operation(summary = "Consulter le dossier de transaction immobilière d'une demande")
    public ResponseEntity<DossierTransactionImmobiliereResponse> getByDemande(
            @PathVariable Integer demandeId) {
        return ResponseEntity.ok(dossierService.getByDemandeId(demandeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOSSIER_CREATE')")
    @Operation(summary = "Créer le dossier de transaction immobilière d'une demande",
            description = "Une seule fiche par demande ; la demande doit comporter un service de type TRANSACTION_IMMOBILIERE")
    public ResponseEntity<DossierTransactionImmobiliereResponse> create(
            @PathVariable Integer demandeId,
            @Valid @RequestBody CreateDossierTransactionImmobiliereRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.create(demandeId, request));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('DOSSIER_UPDATE')")
    @Operation(summary = "Modifier le dossier de transaction immobilière d'une demande")
    public ResponseEntity<DossierTransactionImmobiliereResponse> update(
            @PathVariable Integer demandeId,
            @Valid @RequestBody UpdateDossierTransactionImmobiliereRequest request) {
        return ResponseEntity.ok(dossierService.update(demandeId, request));
    }
}
