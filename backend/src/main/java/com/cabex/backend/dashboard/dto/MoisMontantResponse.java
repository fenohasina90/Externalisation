package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record MoisMontantResponse(
        String mois,
        BigDecimal montant
) {
}
