package com.cabex.backend.facture.dto;

import java.math.BigDecimal;

/** Ligne d'une facture validée (montant et désignation figés à la création). */
public record FactureValideeLigneResponse(
        Integer id,
        Integer ligneServiceId,
        String designation,
        BigDecimal montant
) {
}
