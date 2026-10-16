package com.cabex.backend.demande.controller;

import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.HistoriqueLigneResponse;
import com.cabex.backend.demande.dto.LigneServiceResponse;
import com.cabex.backend.demande.dto.UpdateLigneRequest;
import com.cabex.backend.demande.service.LigneServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demandes/{demandeId}/services")
@Tag(name = "Lignes de service", description = "Services d'une demande (multi-services)")
@SecurityRequirement(name = "bearerAuth")
public class LigneServiceController {

    private final LigneServiceService ligneService;

    public LigneServiceController(LigneServiceService ligneService) {
        this.ligneService = ligneService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Lister les lignes de service d'une demande")
    public ResponseEntity<List<LigneServiceResponse>> lister(@PathVariable Integer demandeId) {
        return ResponseEntity.ok(ligneService.listerParDemande(demandeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Ajouter un service à une demande EN_ATTENTE")
    public ResponseEntity<LigneServiceResponse> ajouter(@PathVariable Integer demandeId,
                                                        @Valid @RequestBody CreateLigneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ligneService.ajouter(demandeId, request));
    }

    @PutMapping("/{ligneId}")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Modifier une ligne de service d'une demande EN_ATTENTE")
    public ResponseEntity<LigneServiceResponse> modifier(@PathVariable Integer demandeId,
                                                         @PathVariable Integer ligneId,
                                                         @Valid @RequestBody UpdateLigneRequest request) {
        return ResponseEntity.ok(ligneService.modifier(ligneId, request));
    }

    @DeleteMapping("/{ligneId}")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Retirer une ligne de service d'une demande EN_ATTENTE")
    public ResponseEntity<Void> retirer(@PathVariable Integer demandeId,
                                        @PathVariable Integer ligneId) {
        ligneService.retirer(ligneId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{ligneId}/historique")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Consulter l'historique d'une ligne de service")
    public ResponseEntity<List<HistoriqueLigneResponse>> historique(@PathVariable Integer demandeId,
                                                                    @PathVariable Integer ligneId) {
        return ResponseEntity.ok(ligneService.historiqueParLigne(ligneId));
    }
}
