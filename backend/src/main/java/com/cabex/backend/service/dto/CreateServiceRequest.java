package com.cabex.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequest(
        @NotBlank(message = "Le nom du service est obligatoire")
        @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
        String nomService,

        String description
) {
}
