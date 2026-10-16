package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLigneRequest(
        @NotNull(message = "Le service est obligatoire")
        Integer serviceId,

        @NotNull(message = "Le tarif est obligatoire")
        BigDecimal tarif,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate debutService,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate finService
) {
}
