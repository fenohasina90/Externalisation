package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;

public record RapportFinancierMode(
        String mode,
        BigDecimal montant
) {
}
