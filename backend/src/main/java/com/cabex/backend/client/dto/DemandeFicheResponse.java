package com.cabex.backend.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DemandeFicheResponse(
        Integer id,
        String referenceDemande,
        LocalDate dateDemande,
        LocalDate debutService,
        LocalDate finService,
        String serviceNom,
        String statutActuel,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        LocalDate createdAt
) {
}
