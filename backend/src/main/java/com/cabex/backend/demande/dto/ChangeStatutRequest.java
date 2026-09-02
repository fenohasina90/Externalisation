package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatutRequest(
        @NotBlank(message = "Le nouveau statut est obligatoire")
        String statut,

        String commentaire
) {
}
