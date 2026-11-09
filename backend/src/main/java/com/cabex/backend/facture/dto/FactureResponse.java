package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FactureResponse(
        Integer id,
        String referenceFacture,
        String referenceFactureValidee,
        Integer demandeId,
        String referenceDemande,
        String modeFacturation,
        /** Montant proforma (modifiable tant que PROFORMA, conservé après validation). */
        BigDecimal montantProforma,
        /** Montant validé (null tant que PROFORMA). */
        BigDecimal montantValidee,
        String statut,
        Integer valideParId,
        String valideParNom,
        LocalDateTime valideLe,
        List<FactureLigneResponse> lignes
) {
}
