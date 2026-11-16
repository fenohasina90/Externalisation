package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValiderMontantDeboursRequest(
        @NotNull(message = "Le montant du débours est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant du débours doit être supérieur à 0")
        BigDecimal montantDebours
) {
}
