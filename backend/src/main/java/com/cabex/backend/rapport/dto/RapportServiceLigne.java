package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;

public record RapportServiceLigne(
        Integer serviceId,
        String nomService,
        String nomDomaine,
        long nombreDemandes,
        BigDecimal revenu
) {
}
