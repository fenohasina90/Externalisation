package com.cabex.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateServiceRequest(
        @NotBlank(message = "Le nom du service est obligatoire")
        @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
        String nomService,

        @Size(max = 30, message = "Le type ne doit pas dépasser 30 caractères")
        String typeService,

        String description,

        Integer domaineId
) {
}
