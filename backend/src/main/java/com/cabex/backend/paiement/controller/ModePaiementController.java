package com.cabex.backend.paiement.controller;

import com.cabex.backend.paiement.dto.CreateModePaiementRequest;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.UpdateModePaiementRequest;
import com.cabex.backend.paiement.service.ModePaiementService;
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
@RequestMapping("/api/modes-paiement")
@Tag(name = "Modes de paiement", description = "Gestion des modes de paiement (administration)")
@SecurityRequirement(name = "bearerAuth")
public class ModePaiementController {

    private final ModePaiementService modePaiementService;

    public ModePaiementController(ModePaiementService modePaiementService) {
        this.modePaiementService = modePaiementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_VIEW')")
    @Operation(summary = "Lister / filtrer les modes de paiement", description = "Pagination, filtre par actif")
    public ResponseEntity<Page<ModePaiementResponse>> getAll(
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(modePaiementService.getAll(actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_VIEW')")
    @Operation(summary = "Consulter un mode de paiement")
    public ResponseEntity<ModePaiementResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(modePaiementService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_UPDATE')")
    @Operation(summary = "Créer un mode de paiement")
    public ResponseEntity<ModePaiementResponse> create(@Valid @RequestBody CreateModePaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modePaiementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_UPDATE')")
    @Operation(summary = "Modifier un mode de paiement")
    public ResponseEntity<ModePaiementResponse> update(@PathVariable Integer id,
                                                       @Valid @RequestBody UpdateModePaiementRequest request) {
        return ResponseEntity.ok(modePaiementService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_UPDATE')")
    @Operation(summary = "Activer / désactiver un mode de paiement")
    public ResponseEntity<ModePaiementResponse> toggleActivation(@PathVariable Integer id,
                                                                 @RequestBody Map<String, Boolean> body) {
        boolean actif = Boolean.TRUE.equals(body.get("actif"));
        return ResponseEntity.ok(modePaiementService.toggleActivation(id, actif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODE_PAIEMENT_UPDATE')")
    @Operation(summary = "Désactiver (supprimer) un mode de paiement")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        modePaiementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
