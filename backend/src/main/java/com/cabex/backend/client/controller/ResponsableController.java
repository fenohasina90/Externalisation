package com.cabex.backend.client.controller;

import com.cabex.backend.client.dto.CreateResponsableRequest;
import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.dto.UpdateResponsableRequest;
import com.cabex.backend.client.service.ResponsableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/responsables")
@Tag(name = "Responsables clients", description = "Gestion des responsables des sociétés clientes")
@SecurityRequirement(name = "bearerAuth")
public class ResponsableController {

    private final ResponsableService responsableService;

    public ResponsableController(ResponsableService responsableService) {
        this.responsableService = responsableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RESPONSABLE_VIEW')")
    @Operation(summary = "Lister les responsables d'une société")
    public ResponseEntity<List<ResponsableResponse>> getBySociete(
            @RequestParam Integer societeId) {
        return ResponseEntity.ok(responsableService.getBySociete(societeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPONSABLE_VIEW')")
    @Operation(summary = "Consulter un responsable")
    public ResponseEntity<ResponsableResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(responsableService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESPONSABLE_CREATE')")
    @Operation(summary = "Créer un responsable")
    public ResponseEntity<ResponsableResponse> create(@Valid @RequestBody CreateResponsableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responsableService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPONSABLE_UPDATE')")
    @Operation(summary = "Modifier un responsable")
    public ResponseEntity<ResponsableResponse> update(@PathVariable Integer id,
                                                      @Valid @RequestBody UpdateResponsableRequest request) {
        return ResponseEntity.ok(responsableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPONSABLE_DELETE')")
    @Operation(summary = "Désactiver un responsable")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        responsableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
