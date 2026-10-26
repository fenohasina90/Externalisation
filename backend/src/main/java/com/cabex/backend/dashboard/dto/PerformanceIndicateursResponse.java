package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record PerformanceIndicateursResponse(
        BigDecimal delaiMoyenTraitementJours,
        Long demandesTerminees,
        BigDecimal tauxDemandesTerminees,
        BigDecimal montantMoyenParDemande,
        BigDecimal tauxPaiementATemps,
        ServiceTopResponse servicePlusDemande,
        CommercialTopResponse commercialTop,
        List<DemandeTermineeMoisResponse> demandesTermineesParMois
) {
}
