package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        Long nombreClients,
        Long nombreDemandes,
        Long nombreServices,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        Long echeancesEnRetard,
        Long echeancesProches,
        Long demandesEnAttente,
        Long demandesEnCours
) {
}
