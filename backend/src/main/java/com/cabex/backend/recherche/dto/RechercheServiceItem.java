package com.cabex.backend.recherche.dto;

public record RechercheServiceItem(
        Integer id,
        String nomService,
        java.math.BigDecimal tarif
) {
}
