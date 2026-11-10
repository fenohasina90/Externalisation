package com.cabex.backend.facture.dto;

import java.math.BigDecimal;

/**
 * Élément impayé pouvant être repris dans la facture du mois
 * (prestation ou débours d'une demande en cours de la même société).
 */
public record InclusionDisponibleResponse(
        Integer ligneId,
        Integer demandeId,
        String referenceDemande,
        String designation,
        /** PRESTATION ou DEBOURS. */
        String type,
        BigDecimal reste
) {
}
