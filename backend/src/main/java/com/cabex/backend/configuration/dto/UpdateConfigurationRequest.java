package com.cabex.backend.configuration.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConfigurationRequest(
        @NotBlank(message = "La valeur est obligatoire")
        String valeur
) {
}
