package com.cabex.backend.service.controller;

import com.cabex.backend.service.dto.CreateDomaineRequest;
import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.dto.UpdateDomaineRequest;
import com.cabex.backend.service.service.DomaineGestionService;
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
@RequestMapping("/api/domaines")
@Tag(name = "Domaines", description = "Gestion des domaines de services")
@SecurityRequirement(name = "bearerAuth")
public class DomaineController {

    private final DomaineGestionService domaineService;

    public DomaineController(DomaineGestionService domaineService) {
        this.domaineService = domaineService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOMAINE_VIEW')")
    @Operation(summary = "Lister / rechercher les domaines", description = "Pagination, recherche par nom, filtre par actif")
    public ResponseEntity<Page<DomaineResponse>> getAll(
            @RequestParam(value = "recherche", required = false) String recherche,
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(domaineService.getAll(recherche, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMAINE_VIEW')")
    @Operation(summary = "Consulter un domaine")
    public ResponseEntity<DomaineResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(domaineService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOMAINE_CREATE')")
    @Operation(summary = "Créer un domaine")
    public ResponseEntity<DomaineResponse> create(@Valid @RequestBody CreateDomaineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(domaineService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMAINE_UPDATE')")
    @Operation(summary = "Modifier un domaine")
    public ResponseEntity<DomaineResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody UpdateDomaineRequest request) {
        return ResponseEntity.ok(domaineService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('DOMAINE_UPDATE')")
    @Operation(summary = "Activer / désactiver un domaine")
    public ResponseEntity<DomaineResponse> toggleActivation(@PathVariable Integer id,
                                                            @RequestBody Map<String, Boolean> body) {
        boolean actif = Boolean.TRUE.equals(body.get("actif"));
        return ResponseEntity.ok(domaineService.toggleActivation(id, actif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOMAINE_DELETE')")
    @Operation(summary = "Désactiver (supprimer) un domaine")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        domaineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
