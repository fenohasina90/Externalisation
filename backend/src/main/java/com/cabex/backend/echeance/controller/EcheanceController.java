package com.cabex.backend.echeance.controller;

import com.cabex.backend.echeance.dto.CreateEcheanceRequest;
import com.cabex.backend.echeance.dto.EcheanceResponse;
import com.cabex.backend.echeance.dto.UpdateEcheanceRequest;
import com.cabex.backend.echeance.service.EcheanceGestionService;
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
@Tag(name = "Échéances", description = "Suivi des échéances d'un plan de paiement")
@SecurityRequirement(name = "bearerAuth")
public class EcheanceController {

    private final EcheanceGestionService echeanceService;

    public EcheanceController(EcheanceGestionService echeanceService) {
        this.echeanceService = echeanceService;
    }

    @GetMapping("/plans-paiement/{id}/echeances")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Lister les échéances d'un plan de paiement",
            description = "Statut calculé automatiquement (PAYEE / PARTIELLE / PROCHE / A_VENIR / EN_RETARD) et reste à payer")
    public ResponseEntity<List<EcheanceResponse>> getByPlan(@PathVariable Integer id) {
        return ResponseEntity.ok(echeanceService.getByPlan(id));
    }

    @PostMapping("/plans-paiement/{id}/echeances")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Ajouter une échéance à un plan de paiement")
    public ResponseEntity<EcheanceResponse> create(@PathVariable Integer id,
                                                   @Valid @RequestBody CreateEcheanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeanceService.create(id, request));
    }

    @PutMapping("/echeances/{id}")
    @PreAuthorize("hasAuthority('DEMANDE_UPDATE')")
    @Operation(summary = "Modifier une échéance (date, montant attendu)",
            description = "Recalcule le montant total / reste à payer du plan")
    public ResponseEntity<EcheanceResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateEcheanceRequest request) {
        return ResponseEntity.ok(echeanceService.update(id, request));
    }

    @GetMapping("/echeances/en-retard")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Liste des échéances en retard",
            description = "Date limite dépassée et montant restant à payer")
    public ResponseEntity<List<EcheanceResponse>> getEnRetard() {
        return ResponseEntity.ok(echeanceService.getEnRetard());
    }

    @GetMapping("/echeances/a-venir")
    @PreAuthorize("hasAuthority('DEMANDE_VIEW')")
    @Operation(summary = "Liste des échéances à venir",
            description = "Non échues et non entièrement payées")
    public ResponseEntity<List<EcheanceResponse>> getAVenir() {
        return ResponseEntity.ok(echeanceService.getAVenir());
    }
}
