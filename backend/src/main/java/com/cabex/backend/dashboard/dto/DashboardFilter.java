package com.cabex.backend.dashboard.dto;

import java.time.LocalDate;

/** Filtres communs du tableau de bord : période, client (société) et service. */
public record DashboardFilter(
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer clientId,
        Integer serviceId
) {
    public static DashboardFilter vide() {
        return new DashboardFilter(null, null, null, null);
    }

    public boolean aDesFiltres() {
        return dateDebut != null || dateFin != null || clientId != null || serviceId != null;
    }
}
