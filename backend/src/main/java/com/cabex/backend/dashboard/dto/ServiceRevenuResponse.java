package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record ServiceRevenuResponse(
        Integer serviceId,
        String nomService,
        long nombreDemandes,
        BigDecimal revenu
) {
}
