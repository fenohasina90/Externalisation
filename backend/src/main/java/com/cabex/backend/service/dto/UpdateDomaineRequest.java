package com.cabex.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDomaineRequest(
        @NotBlank(message = "Le nom du domaine est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nomDomaine,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
        String description
) {
}
