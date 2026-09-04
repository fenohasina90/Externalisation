package com.cabex.backend.dashboard.dto;

import java.util.List;

public record EcheanceDashboardResponse(
        List<EcheanceAlerteResponse> enRetard,
        List<EcheanceAlerteResponse> aVenir
) {
}
