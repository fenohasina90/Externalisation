package com.cabex.backend.client.dto;

import java.math.BigDecimal;

public record ServiceUtiliseResponse(
        Integer serviceId,
        String nomService,
        Integer domaineId,
        String nomDomaine,
        BigDecimal tarifCourant,
        long nombreDemandes
) {
}
