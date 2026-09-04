package com.cabex.backend.configuration.controller;

import com.cabex.backend.configuration.dto.ConfigurationResponse;
import com.cabex.backend.configuration.dto.UpdateConfigurationRequest;
import com.cabex.backend.configuration.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/configuration")
@Tag(name = "Configuration", description = "Paramètres généraux et configuration de l'application")
@SecurityRequirement(name = "bearerAuth")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    @Operation(summary = "Lister tous les paramètres de configuration")
    public ResponseEntity<List<ConfigurationResponse>> getAll() {
        return ResponseEntity.ok(configurationService.getAll());
    }

    @GetMapping("/{cle}")
    @PreAuthorize("hasAuthority('CONFIG_VIEW')")
    @Operation(summary = "Consulter un paramètre de configuration par sa clé")
    public ResponseEntity<ConfigurationResponse> getByCle(@PathVariable String cle) {
        return ResponseEntity.ok(configurationService.getByCle(cle));
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE')")
    @Operation(summary = "Modifier la valeur d'un paramètre de configuration")
    public ResponseEntity<ConfigurationResponse> update(@PathVariable String cle,
                                                        @Valid @RequestBody UpdateConfigurationRequest request) {
        return ResponseEntity.ok(configurationService.update(cle, request));
    }
}
