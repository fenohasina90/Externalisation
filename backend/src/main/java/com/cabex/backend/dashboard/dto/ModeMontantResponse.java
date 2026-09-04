package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;

public record ModeMontantResponse(
        String mode,
        BigDecimal montant
) {
}
