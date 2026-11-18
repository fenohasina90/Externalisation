package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Facture validée non soldée au-delà du délai d'alerte applicable
 * (prestation/débours ou mensuelle selon le type de la facture).
 */
public record PaiementRetardResponse(
        Long factureValideeId,
        Integer societeId,
        String nomSociete,
        Integer demandeId,
        String referenceDemande,
        LocalDate dateEmission,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal reste,
        String statut
) {
}
