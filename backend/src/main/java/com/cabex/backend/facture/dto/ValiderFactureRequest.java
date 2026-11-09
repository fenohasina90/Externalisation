package com.cabex.backend.facture.dto;

import java.math.BigDecimal;

public record ValiderFactureRequest(
        String commentaire,
        /** Montant validé (optionnel — défaut : montant proforma). */
        BigDecimal montant
) {
}
