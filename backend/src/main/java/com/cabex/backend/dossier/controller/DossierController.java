package com.cabex.backend.dossier.controller;

import com.cabex.backend.dossier.dto.CreateDossierRequest;
import com.cabex.backend.dossier.dto.DossierResponse;
import com.cabex.backend.dossier.dto.UpdateDossierRequest;
import com.cabex.backend.dossier.service.DossierService;
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
@RequestMapping("/api/demandes/{demandeId}/dossier")
@Tag(name = "Dossier de création", description = "Fiche de création d'entreprise ou de société liée à une demande")
@SecurityRequirement(name = "bearerAuth")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOSSIER_VIEW')")
    @Operation(summary = "Consulter le dossier de création d'une demande")
    public ResponseEntity<DossierResponse> getByDemande(@PathVariable Integer demandeId) {
        return ResponseEntity.ok(dossierService.getByDemandeId(demandeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOSSIER_CREATE')")
    @Operation(summary = "Créer le dossier de création d'une demande",
            description = "Une seule fiche par demande ; associés obligatoires pour une société")
    public ResponseEntity<DossierResponse> create(@PathVariable Integer demandeId,
                                                  @Valid @RequestBody CreateDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.create(demandeId, request));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('DOSSIER_UPDATE')")
    @Operation(summary = "Modifier le dossier de création d'une demande")
    public ResponseEntity<DossierResponse> update(@PathVariable Integer demandeId,
                                                  @Valid @RequestBody UpdateDossierRequest request) {
        return ResponseEntity.ok(dossierService.update(demandeId, request));
    }
}
