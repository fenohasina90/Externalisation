package com.cabex.backend.demande.controller;

import com.cabex.backend.demande.dto.CreateStatutRequest;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.dto.UpdateStatutRequest;
import com.cabex.backend.demande.service.StatutService;
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
@RequestMapping("/api/statuts")
@Tag(name = "Statuts", description = "Gestion des statuts de demande (administration)")
@SecurityRequirement(name = "bearerAuth")
public class StatutController {

    private final StatutService statutService;

    public StatutController(StatutService statutService) {
        this.statutService = statutService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    @Operation(summary = "Lister / filtrer les statuts", description = "Pagination, filtre par actif")
    public ResponseEntity<Page<StatutResponse>> getAll(
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(statutService.getAll(actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    @Operation(summary = "Consulter un statut")
    public ResponseEntity<StatutResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(statutService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_UPDATE')")
    @Operation(summary = "Créer un statut")
    public ResponseEntity<StatutResponse> create(@Valid @RequestBody CreateStatutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statutService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE')")
    @Operation(summary = "Modifier un statut")
    public ResponseEntity<StatutResponse> update(@PathVariable Integer id,
                                                 @Valid @RequestBody UpdateStatutRequest request) {
        return ResponseEntity.ok(statutService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE')")
    @Operation(summary = "Activer / désactiver un statut")
    public ResponseEntity<StatutResponse> toggleActivation(@PathVariable Integer id,
                                                           @RequestBody Map<String, Boolean> body) {
        boolean actif = Boolean.TRUE.equals(body.get("actif"));
        return ResponseEntity.ok(statutService.toggleActivation(id, actif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE')")
    @Operation(summary = "Désactiver (supprimer) un statut")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        statutService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
