package com.cabex.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDomaineRequest(
        @NotBlank(message = "Le nom du domaine est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nomDomaine,

        @NotBlank(message = "Le code domaine est obligatoire")
        @Size(max = 50, message = "Le code domaine ne doit pas dépasser 50 caractères")
        String codeDomaine,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
        String description
) {
}
