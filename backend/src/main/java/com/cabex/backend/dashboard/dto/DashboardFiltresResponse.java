package com.cabex.backend.dashboard.dto;

import java.util.List;

public record DashboardFiltresResponse(
        List<DashboardFiltreOption> clients,
        List<DashboardFiltreOption> services
) {
}
