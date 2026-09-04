package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;

public record RapportGeneralResponse(
        long nombreClients,
        long nombreDemandes,
        long nombreServices,
        BigDecimal montantTotalPrevu,
        BigDecimal montantEncaisse,
        BigDecimal resteAPayer,
        BigDecimal montantEnRetard,
        long echeancesEnRetard
) {
}
