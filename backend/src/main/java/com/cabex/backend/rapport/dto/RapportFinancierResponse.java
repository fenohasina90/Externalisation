package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;
import java.util.List;

public record RapportFinancierResponse(
        BigDecimal montantTotalPrevu,
        BigDecimal montantEncaisse,
        BigDecimal resteAPayer,
        BigDecimal montantEnRetard,
        List<RapportFinancierMode> paiementsParMode
) {
}
