package com.cabex.backend.client.controller;

import com.cabex.backend.client.dto.CreateSocieteRequest;
import com.cabex.backend.client.dto.RisqueClientResponse;
import com.cabex.backend.client.dto.SocieteDetailResponse;
import com.cabex.backend.client.dto.SocieteResponse;
import com.cabex.backend.client.dto.UpdateSocieteRequest;
import com.cabex.backend.client.service.RisqueService;
import com.cabex.backend.client.service.SocieteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients (Sociétés)", description = "Gestion des sociétés clientes et fiche 360°")
@SecurityRequirement(name = "bearerAuth")
public class SocieteController {

    private final SocieteService societeService;
    private final RisqueService risqueService;

    public SocieteController(SocieteService societeService, RisqueService risqueService) {
        this.societeService = societeService;
        this.risqueService = risqueService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    @Operation(summary = "Lister / rechercher / filtrer les sociétés", description = "Pagination, recherche par nom/activité/contact, filtre par actif")
    public ResponseEntity<Page<SocieteResponse>> getAll(
            @RequestParam(value = "recherche", required = false) String recherche,
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(societeService.getAll(recherche, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    @Operation(summary = "Fiche client 360°",
            description = "Informations générales + responsables + commerciaux + demandes + services utilisés + paiements + échéances + pièces jointes + synthèse + niveau de risque")
    public ResponseEntity<SocieteDetailResponse> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(societeService.getDetail(id));
    }

    @GetMapping("/{id}/risque")
    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    @Operation(summary = "Score de risque de paiement",
            description = "Niveau de vigilance (FAIBLE / MOYEN / ELEVE) selon l'historique de paiement — indicateur d'aide à la décision")
    public ResponseEntity<RisqueClientResponse> risque(@PathVariable Integer id) {
        return ResponseEntity.ok(risqueService.calculer(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE')")
    @Operation(summary = "Créer une société")
    public ResponseEntity<SocieteResponse> create(@Valid @RequestBody CreateSocieteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(societeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    @Operation(summary = "Modifier une société")
    public ResponseEntity<SocieteResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody UpdateSocieteRequest request) {
        return ResponseEntity.ok(societeService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE')")
    @Operation(summary = "Archiver / désarchiver une société")
    public ResponseEntity<SocieteResponse> archive(@PathVariable Integer id,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean archive = Boolean.TRUE.equals(body.get("archive"));
        return ResponseEntity.ok(societeService.archive(id, archive));
    }
}
