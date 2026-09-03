package com.cabex.backend.commercial.controller;

import com.cabex.backend.commercial.dto.CommercialResponse;
import com.cabex.backend.commercial.dto.CreateCommercialRequest;
import com.cabex.backend.commercial.dto.UpdateCommercialRequest;
import com.cabex.backend.commercial.service.CommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/commerciaux")
@Tag(name = "Commerciaux", description = "Gestion des commerciaux associés aux demandes")
@SecurityRequirement(name = "bearerAuth")
public class CommercialController {

    private final CommercialService commercialService;

    public CommercialController(CommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COMMERCIAL_VIEW')")
    @Operation(summary = "Lister / rechercher les commerciaux",
            description = "Pagination, recherche par nom/immatricule, filtre par actif")
    public ResponseEntity<Page<CommercialResponse>> getAll(
            @RequestParam(value = "recherche", required = false) String recherche,
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(commercialService.getAll(recherche, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMERCIAL_VIEW')")
    @Operation(summary = "Consulter un commercial")
    public ResponseEntity<CommercialResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(commercialService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMMERCIAL_CREATE')")
    @Operation(summary = "Créer un commercial")
    public ResponseEntity<CommercialResponse> create(@Valid @RequestBody CreateCommercialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commercialService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMERCIAL_UPDATE')")
    @Operation(summary = "Modifier un commercial")
    public ResponseEntity<CommercialResponse> update(@PathVariable Integer id,
                                                     @Valid @RequestBody UpdateCommercialRequest request) {
        return ResponseEntity.ok(commercialService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('COMMERCIAL_UPDATE')")
    @Operation(summary = "Activer / désactiver un commercial")
    public ResponseEntity<CommercialResponse> toggleActivation(@PathVariable Integer id,
                                                               @RequestBody Map<String, Boolean> body) {
        boolean actif = Boolean.TRUE.equals(body.get("actif"));
        return ResponseEntity.ok(commercialService.toggleActivation(id, actif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMERCIAL_DELETE')")
    @Operation(summary = "Désactiver (supprimer) un commercial")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        commercialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
