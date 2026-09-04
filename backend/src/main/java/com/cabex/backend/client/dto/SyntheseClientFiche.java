package com.cabex.backend.client.dto;

import java.math.BigDecimal;

public record SyntheseClientFiche(
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        long nombreEcheancesEnRetard,
        BigDecimal tauxPaiementATemps,
        String risque
) {
}
