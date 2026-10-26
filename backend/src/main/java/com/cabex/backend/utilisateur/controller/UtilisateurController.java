package com.cabex.backend.utilisateur.controller;

import com.cabex.backend.utilisateur.dto.CreateUtilisateurRequest;
import com.cabex.backend.utilisateur.dto.UpdateUtilisateurRequest;
import com.cabex.backend.utilisateur.dto.UtilisateurResponse;
import com.cabex.backend.utilisateur.service.UtilisateurService;
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
@RequestMapping("/api/utilisateurs")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs (réservé au SUPER_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Lister les utilisateurs", description = "Liste paginée, filtrable par recherche, rôle et statut")
    public ResponseEntity<Page<UtilisateurResponse>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @RequestParam(value = "actif", required = false) Boolean actif,
            Pageable pageable) {
        return ResponseEntity.ok(utilisateurService.getAll(search, roleId, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Consulter un utilisateur")
    public ResponseEntity<UtilisateurResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(utilisateurService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<UtilisateurResponse> create(@Valid @RequestBody CreateUtilisateurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<UtilisateurResponse> update(@PathVariable Integer id,
                                                      @Valid @RequestBody UpdateUtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.update(id, request));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Activer / désactiver un utilisateur")
    public ResponseEntity<UtilisateurResponse> toggleActivation(@PathVariable Integer id,
                                                                @RequestBody Map<String, Boolean> body) {
        boolean actif = Boolean.TRUE.equals(body.get("actif"));
        return ResponseEntity.ok(utilisateurService.toggleActivation(id, actif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        utilisateurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
