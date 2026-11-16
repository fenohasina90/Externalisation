package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ValiderFactureRequest(
        String commentaire,
        /** Montant validé global (optionnel — défaut : montant proforma). */
        BigDecimal montant,
        /**
         * Montants validés par ligne (une ligne par service non débours de la facture).
         * Prend la priorité sur {@link #montant}.
         */
        @Size(max = 100, message = "Trop de lignes à valider")
        List<ValiderFactureLigneRequest> lignes
) {
}
