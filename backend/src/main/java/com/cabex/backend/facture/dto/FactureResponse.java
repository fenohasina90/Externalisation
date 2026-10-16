package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FactureResponse(
        Integer id,
        String referenceFacture,
        Integer demandeId,
        String referenceDemande,
        String modeFacturation,
        BigDecimal montant,
        String statut,
        Integer valideParId,
        String valideParNom,
        LocalDateTime valideLe,
        List<FactureLigneResponse> lignes
) {
}
