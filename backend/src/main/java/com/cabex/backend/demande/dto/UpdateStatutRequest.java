package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStatutRequest(
        @NotBlank(message = "Le statut est obligatoire")
        @Size(max = 50, message = "Le statut ne doit pas dépasser 50 caractères")
        String statut,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
        String description,

        Integer ordre
) {
}
