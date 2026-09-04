package com.cabex.backend.recherche.controller;

import com.cabex.backend.recherche.dto.RechercheGlobaleResponse;
import com.cabex.backend.recherche.service.RechercheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recherche")
@Tag(name = "Recherche globale", description = "Recherche globale sur les clients, services, demandes et paiements")
@SecurityRequirement(name = "bearerAuth")
public class RechercheController {

    private final RechercheService rechercheService;

    public RechercheController(RechercheService rechercheService) {
        this.rechercheService = rechercheService;
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Recherche globale (client, service, demande, paiement)")
    public ResponseEntity<RechercheGlobaleResponse> rechercher(
            @RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(rechercheService.rechercher(q));
    }
}
