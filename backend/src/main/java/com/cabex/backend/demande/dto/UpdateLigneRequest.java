package com.cabex.backend.demande.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLigneRequest(
        BigDecimal tarif,
        LocalDate debutService,
        LocalDate finService
) {
}
