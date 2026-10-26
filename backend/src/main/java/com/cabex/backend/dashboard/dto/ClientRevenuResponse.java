package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record ClientRevenuResponse(
        Integer clientId,
        String nomSociete,
        Long nombreDemandes,
        BigDecimal montantTotal,
        BigDecimal montantPaye
) {
}
