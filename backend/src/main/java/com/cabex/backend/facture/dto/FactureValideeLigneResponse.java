package com.cabex.backend.facture.dto;

import java.math.BigDecimal;

/** Ligne d'une facture validée (montant et désignation figés à la création). */
public record FactureValideeLigneResponse(
        Integer id,
        Integer ligneServiceId,
        /** Référence de la demande d'où provient la ligne. */
        String referenceDemande,
        String designation,
        BigDecimal montant
) {
}
