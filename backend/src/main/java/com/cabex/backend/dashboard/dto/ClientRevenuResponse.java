package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record ClientRevenuResponse(
        Integer clientId,
        String nomSociete,
        long nombreDemandes,
        BigDecimal montantTotal,
        BigDecimal montantPaye
) {
}
