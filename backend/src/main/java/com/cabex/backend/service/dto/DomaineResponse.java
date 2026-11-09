package com.cabex.backend.service.dto;

public record DomaineResponse(
        Integer id,
        String nomDomaine,
        String codeDomaine,
        String description,
        Boolean actif
) {
}
