package com.cabex.backend.reinitialisation.controller;

import com.cabex.backend.reinitialisation.dto.ReinitialisationRequest;
import com.cabex.backend.reinitialisation.dto.ReinitialisationResponse;
import com.cabex.backend.reinitialisation.service.ReinitialisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Suppression définitive de données métier sélectionnées depuis
 * l'administration (« Réinitialisation de données »).
 * Réservé aux profils disposant de la permission {@code REINITIALISER_DONNEES}.
 */
@RestController
@RequestMapping("/api/admin/reinitialisation")
@Tag(name = "Réinitialisation", description = "Suppression définitive de données métier choisies — administration")
@SecurityRequirement(name = "bearerAuth")
public class ReinitialisationController {

    private final ReinitialisationService service;

    public ReinitialisationController(ReinitialisationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REINITIALISER_DONNEES')")
    @Operation(summary = "Supprimer définitivement les données sélectionnées (irréversible)")
    public ResponseEntity<ReinitialisationResponse> reinitialiser(
            @RequestBody ReinitialisationRequest request) {
        return ResponseEntity.ok(service.reinitialiser(request));
    }
}
