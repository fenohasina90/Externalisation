package com.cabex.backend.demande.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLigneRequest(
        BigDecimal tarif,
        LocalDate debutService,
        LocalDate finService,
        /** Désignation (DEBOURS en EN_ATTENTE uniquement). */
        String designationDebours
) {
}
