package com.cabex.backend.echeance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheancePlanRequest(
        @NotNull(message = "La date d'échéance est obligatoire")
        LocalDate dateEcheance,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal montantAttendu
) {
}
