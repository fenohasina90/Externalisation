package com.cabex.backend.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "Le nom du service est obligatoire")
        @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
        String nomService,

        @NotNull(message = "Le tarif est obligatoire")
        @DecimalMin(value = "0.0", message = "Le tarif doit être positif ou nul")
        BigDecimal tarifService,

        String description,

        @NotNull(message = "Le domaine est obligatoire")
        Integer domaineId
) {
}
