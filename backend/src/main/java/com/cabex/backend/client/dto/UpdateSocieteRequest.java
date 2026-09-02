package com.cabex.backend.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSocieteRequest(
        @NotBlank(message = "Le nom de la société est obligatoire")
        @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
        String nomSociete,

        @Size(max = 255, message = "L'activité ne doit pas dépasser 255 caractères")
        String activite,

        @Size(max = 100, message = "Le contact ne doit pas dépasser 100 caractères")
        String contact,

        @Email(message = "L'email doit être valide")
        String email,

        @Size(max = 255, message = "Le lien Facebook ne doit pas dépasser 255 caractères")
        String fb,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresse,

        LocalDate dateCreation,

        @Positive(message = "Le nombre de personnel doit être positif")
        Integer nombrePersonnel,

        @PositiveOrZero(message = "Le chiffre d'affaire doit être positif ou nul")
        BigDecimal chiffreAffaireMensuel
) {
}
