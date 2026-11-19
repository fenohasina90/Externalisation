package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        Long nombreClients,
        Long nombreDemandes,
        Long nombreServices,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        /** Nombre de factures validées non soldées au-delà des délais d'alerte. */
        Long paiementsEnRetard,
        Long demandesEnAttente,
        Long demandesEnCours
) {
}
