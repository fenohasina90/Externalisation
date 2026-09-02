package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long nombreClients,
        long nombreDemandes,
        long nombreServices,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        long echeancesEnRetard,
        long echeancesProches,
        long demandesEnAttente,
        long demandesEnCours
) {
}
