package com.cabex.backend.echeance.controller;

import com.cabex.backend.echeance.dto.CreatePlanPaiementRequest;
import com.cabex.backend.echeance.dto.PlanPaiementResponse;
import com.cabex.backend.echeance.dto.UpdatePlanPaiementRequest;
import com.cabex.backend.echeance.service.PlanPaiementService;
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
@RequestMapping("/api")
@Tag(name = "Plan de paiement", description = "Plan de paiement et échéances d'une demande")
@SecurityRequirement(name = "bearerAuth")
public class PlanPaiementController {

    private final PlanPaiementService planService;

    public PlanPaiementController(PlanPaiementService planService) {
        this.planService = planService;
    }

    @GetMapping("/demandes/{id}/plan-paiement")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Consulter le plan de paiement d'une demande",
            description = "Plan + échéances générées, montant total, reste à payer")
    public ResponseEntity<PlanPaiementResponse> getByDemande(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.getByDemande(id));
    }

    @PostMapping("/demandes/{id}/plan-paiement")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Créer un plan de paiement pour une demande",
            description = "Demandes à ligne unique : crée le plan de la ligne. Demandes multi-services : utilisez POST /lignes/{ligneId}/plan-paiement")
    public ResponseEntity<PlanPaiementResponse> create(@PathVariable Integer id,
                                                       @Valid @RequestBody CreatePlanPaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(id, request));
    }

    @GetMapping("/lignes/{ligneId}/plan-paiement")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Consulter le plan de paiement d'une ligne de service")
    public ResponseEntity<PlanPaiementResponse> getByLigne(@PathVariable Integer ligneId) {
        return ResponseEntity.ok(planService.getByLigne(ligneId));
    }

    @PostMapping("/lignes/{ligneId}/plan-paiement")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Créer le plan de paiement d'une ligne de service",
            description = "Chaque échéance doit être comprise dans la période du service ; la ligne passe EN_ATTENTE")
    public ResponseEntity<PlanPaiementResponse> createForLigne(@PathVariable Integer ligneId,
                                                               @Valid @RequestBody CreatePlanPaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createForLigne(ligneId, request));
    }

    @PutMapping("/plans-paiement/{id}")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Modifier un plan de paiement",
            description = "Met à jour dates/montant total et recalcule le reste à payer")
    public ResponseEntity<PlanPaiementResponse> update(@PathVariable Integer id,
                                                       @Valid @RequestBody UpdatePlanPaiementRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }
}
