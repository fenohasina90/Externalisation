package com.cabex.backend.echeance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePlanPaiementRequest(
        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @NotNull(message = "Le montant total est obligatoire")
        @DecimalMin(value = "0.0", message = "Le montant doit être positif ou nul")
        BigDecimal montantTotal,

        @NotNull(message = "Le nombre d'échéances est obligatoire")
        @Positive(message = "Le nombre d'échéances doit être positif")
        Integer nombreEcheances,

        String commentaire,

        /**
         * Échéances saisies manuellement (date + montant chacune). Si absent,
         * les échéances sont réparties automatiquement. Si présent, la taille
         * doit égaler nombreEcheances et la somme égaler montantTotal.
         */
        @Valid
        List<EcheancePlanRequest> echeances
) {
}
