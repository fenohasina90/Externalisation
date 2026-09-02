package com.cabex.backend.paiement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaiementRequest(
        @NotNull(message = "La demande est obligatoire")
        Integer demandeId,

        Integer echeanceId,

        @NotNull(message = "Le mode de paiement est obligatoire")
        Integer modePaiementId,

        @NotNull(message = "La date de paiement est obligatoire")
        LocalDate datePaiement,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal montantPaye,

        @Size(max = 150, message = "La référence de virement ne doit pas dépasser 150 caractères")
        String referenceVirement,

        String commentaire,

        String pieceJustificative
) {
}
