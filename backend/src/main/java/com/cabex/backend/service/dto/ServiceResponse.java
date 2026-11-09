package com.cabex.backend.service.dto;

public record ServiceResponse(
        Integer id,
        String nomService,
        String codeService,
        String typeService,
        String description,
        Boolean actif,
        Integer domaineId,
        String codeDomaine
) {
}
