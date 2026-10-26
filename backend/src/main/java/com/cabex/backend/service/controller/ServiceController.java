package com.cabex.backend.service.controller;

import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.dto.UpdateServiceRequest;
import com.cabex.backend.service.service.ServiceGestionService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Gestion des services et de leurs tarifs")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {

    private final ServiceGestionService serviceService;

    public ServiceController(ServiceGestionService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SERVICE_VIEW')")
    @Operation(summary = "Lister / rechercher / filtrer les services",
            description = "Pagination, recherche par nom ou code, filtres par actif et par type de service")
    public ResponseEntity<Page<ServiceResponse>> getAll(
            @RequestParam(value = "recherche", required = false) String recherche,
            @RequestParam(value = "actif", required = false) Boolean actif,
            @RequestParam(value = "typeService", required = false) String typeService,
            Pageable pageable) {
        return ResponseEntity.ok(serviceService.getAll(recherche, actif, typeService, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_VIEW')")
    @Operation(summary = "Consulter un service")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(serviceService.getById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('SERVICE_VIEW')")
    @Operation(summary = "Consulter un service par son code (ex. SRV-0001)")
    public ResponseEntity<ServiceResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(serviceService.getByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SERVICE_CREATE')")
    @Operation(summary = "Créer un service avec son tarif initial")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_UPDATE')")
    @Operation(summary = "Modifier un service (nom, description)")
    public ResponseEntity<ServiceResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(serviceService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('SERVICE_UPDATE')")
    @Operation(summary = "Archiver / désarchiver un service")
    public ResponseEntity<ServiceResponse> archive(@PathVariable Integer id,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean archive = Boolean.TRUE.equals(body.get("archive"));
        return ResponseEntity.ok(serviceService.archive(id, archive));
    }
}
