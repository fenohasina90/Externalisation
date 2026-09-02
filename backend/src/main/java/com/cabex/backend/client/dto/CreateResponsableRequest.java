package com.cabex.backend.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResponsableRequest(
        @NotNull(message = "La société est obligatoire")
        Integer societeId,

        @NotBlank(message = "Le nom du responsable est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nomClient,

        @Size(max = 100, message = "Le contact ne doit pas dépasser 100 caractères")
        String contact,

        @Email(message = "L'email doit être valide")
        String email,

        @Size(max = 100, message = "La fonction ne doit pas dépasser 100 caractères")
        String fonction
) {
}
