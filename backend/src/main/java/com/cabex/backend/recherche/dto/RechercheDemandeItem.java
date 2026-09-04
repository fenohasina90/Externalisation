package com.cabex.backend.recherche.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RechercheDemandeItem(
        Integer id,
        String referenceDemande,
        String nomSociete,
        String nomService,
        String statutActuel,
        LocalDate dateDemande,
        BigDecimal tarif
) {
}
