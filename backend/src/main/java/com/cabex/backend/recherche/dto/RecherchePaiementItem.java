package com.cabex.backend.recherche.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecherchePaiementItem(
        Integer id,
        String referencePaiement,
        String referenceDemande,
        String nomSociete,
        String modePaiement,
        LocalDate datePaiement,
        BigDecimal montant
) {
}
