package com.cabex.backend.echeance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePlanPaiementRequest(
        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @NotNull(message = "Le montant total est obligatoire")
        @DecimalMin(value = "0.0", message = "Le montant doit être positif ou nul")
        BigDecimal montantTotal,

        String commentaire
) {
}
