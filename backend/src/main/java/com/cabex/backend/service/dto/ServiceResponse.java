package com.cabex.backend.service.dto;

public record ServiceResponse(
        Integer id,
        String nomService,
        String codeService,
        String description,
        Boolean actif
) {
}
