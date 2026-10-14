package com.cabex.backend.commercial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommercialRequest(
        @NotBlank(message = "Le nom du commercial est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nomCommercial,

        @Email(message = "L'email doit être valide")
        String email,

        @Size(max = 100, message = "Le contact ne doit pas dépasser 100 caractères")
        String contact
) {
    // L'immatricule (C001, C002, …) est générée automatiquement et garantie
    // unique côté serveur : elle ne fait pas partie du contrat de création.
}
