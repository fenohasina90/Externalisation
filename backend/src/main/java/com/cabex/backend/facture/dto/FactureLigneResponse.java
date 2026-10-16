package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureLigneResponse(
        Integer ligneServiceId,
        Integer serviceId,
        String nomService,
        BigDecimal tarif,
        LocalDate debutService,
        LocalDate finService,
        String statutLigne
) {
}
