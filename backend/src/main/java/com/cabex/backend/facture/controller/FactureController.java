package com.cabex.backend.facture.controller;

import com.cabex.backend.facture.dto.FactureResponse;
import com.cabex.backend.facture.dto.GenererFacturesRequest;
import com.cabex.backend.facture.dto.ValiderFactureRequest;
import com.cabex.backend.facture.service.FactureService;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Factures", description = "Factures proforma et validation")
@SecurityRequirement(name = "bearerAuth")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping("/demandes/{demandeId}/factures")
    @PreAuthorize("hasAuthority('FACTURE_VIEW')")
    @Operation(summary = "Lister les factures d'une demande")
    public ResponseEntity<List<FactureResponse>> listerParDemande(@PathVariable Integer demandeId) {
        return ResponseEntity.ok(factureService.listerParDemande(demandeId));
    }

    @GetMapping("/factures/{id}")
    @PreAuthorize("hasAuthority('FACTURE_VIEW')")
    @Operation(summary = "Consulter une facture")
    public ResponseEntity<FactureResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(factureService.getById(id));
    }

    @PostMapping("/demandes/{demandeId}/factures")
    @PreAuthorize("hasAuthority('FACTURE_CREATE')")
    @Operation(summary = "Générer les factures proforma d'une demande EN_ATTENTE")
    public ResponseEntity<List<FactureResponse>> generer(@PathVariable Integer demandeId,
                                                         @Valid @RequestBody GenererFacturesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(factureService.genererPourDemande(demandeId, request.modeFacturation()));
    }

    @PutMapping("/factures/{id}/valider")
    @PreAuthorize("hasAuthority('FACTURE_VALIDATE')")
    @Operation(summary = "Valider une facture proforma")
    public ResponseEntity<FactureResponse> valider(@PathVariable Integer id,
                                                   @RequestBody(required = false) ValiderFactureRequest request) {
        return ResponseEntity.ok(factureService.valider(
                id, request != null ? request.commentaire() : null));
    }
}
