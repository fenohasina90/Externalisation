package com.cabex.backend.configuration.dto;

public record ConfigurationResponse(
        Integer id,
        String cle,
        String valeur,
        String typeValeur,
        String description,
        Boolean modifiable
) {
}
