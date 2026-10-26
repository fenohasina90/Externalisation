package com.cabex.backend.dossier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssocieRequest(
        @NotBlank(message = "Le nom de l'associé est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @Size(max = 50, message = "La part sociale ne doit pas dépasser 50 caractères")
        String partSociale,

        @Email(message = "L'email doit être valide")
        @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 50, message = "Le contact ne doit pas dépasser 50 caractères")
        String contact
) {
}
