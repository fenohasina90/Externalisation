package com.cabex.backend.echeance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheanceResponse(
        Integer id,
        Integer proposPaiementId,
        Integer numeroEcheance,
        LocalDate dateEcheance,
        BigDecimal montantAttendu,
        BigDecimal montantPaye,
        BigDecimal reste,
        String statut,
        String commentaire
) {
}
