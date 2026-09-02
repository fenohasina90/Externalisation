package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateDemandeRequest(
        @NotNull(message = "La date de début est obligatoire")
        LocalDate debutService,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate finService,

        Integer commercialId,

        String commentaire
) {
}
