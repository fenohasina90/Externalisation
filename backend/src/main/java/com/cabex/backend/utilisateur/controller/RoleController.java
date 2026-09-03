package com.cabex.backend.utilisateur.controller;

import com.cabex.backend.utilisateur.dto.CreateRoleRequest;
import com.cabex.backend.utilisateur.dto.RoleDetailResponse;
import com.cabex.backend.utilisateur.dto.RoleResponse;
import com.cabex.backend.utilisateur.dto.UpdateRoleRequest;
import com.cabex.backend.utilisateur.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/roles")
@Tag(name = "Rôles", description = "Gestion des rôles et de leurs permissions (création/modification réservées au SUPER_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Lister les rôles")
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Consulter un rôle avec ses permissions")
    public ResponseEntity<RoleDetailResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Créer un rôle")
    public ResponseEntity<RoleDetailResponse> create(@RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Modifier un rôle (description, actif, permissions)")
    public ResponseEntity<RoleDetailResponse> update(@PathVariable Integer id,
                                                     @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }
}
