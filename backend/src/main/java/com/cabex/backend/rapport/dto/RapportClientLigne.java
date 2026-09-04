package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;

public record RapportClientLigne(
        Integer clientId,
        String nomSociete,
        long nombreDemandes,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal reste
) {
}
