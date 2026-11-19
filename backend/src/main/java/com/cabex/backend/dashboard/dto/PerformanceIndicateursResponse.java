package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record PerformanceIndicateursResponse(
        Long demandesTerminees,
        BigDecimal tauxDemandesTerminees,
        ServiceTopResponse servicePlusDemande,
        CommercialTopResponse commercialTop,
        List<DemandeTermineeMoisResponse> demandesTermineesParMois
) {
}
