package com.cabex.backend.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateTarifRequest(
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.0", message = "Le montant doit être positif ou nul")
        BigDecimal montant,

        String commentaire
) {
}
