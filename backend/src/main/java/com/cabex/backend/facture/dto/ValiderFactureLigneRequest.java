package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Montant de validation d'une ligne de service lors de la validation d'une facture. */
public record ValiderFactureLigneRequest(
        @NotNull(message = "La ligne de service est obligatoire")
        Integer ligneServiceId,

        @NotNull(message = "Le montant validé est obligatoire")
        @DecimalMin(value = "0.00", message = "Le montant validé doit être supérieur ou nul")
        BigDecimal montant
) {
}
