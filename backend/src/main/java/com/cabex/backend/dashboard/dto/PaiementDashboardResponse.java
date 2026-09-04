package com.cabex.backend.dashboard.dto;

import java.util.List;

public record PaiementDashboardResponse(
        List<ModeMontantResponse> parMode,
        List<MoisMontantResponse> parMois
) {
}
