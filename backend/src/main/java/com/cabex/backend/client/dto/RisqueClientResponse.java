package com.cabex.backend.client.dto;

import java.math.BigDecimal;

public record RisqueClientResponse(
        String risque,
        String note,
        BigDecimal montantTotalDu,
        BigDecimal montantPaye,
        BigDecimal montantEnRetard,
        long nombreEcheancesEnRetard,
        BigDecimal tauxPaiementATemps
) {
}
