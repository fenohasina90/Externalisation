package com.cabex.backend.service.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Integer id,
        String nomService,
        BigDecimal tarifService,
        String description,
        Boolean actif
) {
}
