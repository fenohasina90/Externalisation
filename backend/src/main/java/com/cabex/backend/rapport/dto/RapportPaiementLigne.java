package com.cabex.backend.rapport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RapportPaiementLigne(
        Integer id,
        String referencePaiement,
        String referenceDemande,
        String nomSociete,
        String modePaiement,
        LocalDate datePaiement,
        BigDecimal montant
) {
}
