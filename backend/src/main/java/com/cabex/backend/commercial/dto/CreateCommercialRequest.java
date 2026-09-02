package com.cabex.backend.commercial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommercialRequest(
        @NotBlank(message = "Le nom du commercial est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nomCommercial,

        @NotBlank(message = "L'immatricule est obligatoire")
        @Size(max = 100, message = "L'immatricule ne doit pas dépasser 100 caractères")
        String immatricule,

        @Email(message = "L'email doit être valide")
        String email,

        @Size(max = 100, message = "Le contact ne doit pas dépasser 100 caractères")
        String contact
) {
}
