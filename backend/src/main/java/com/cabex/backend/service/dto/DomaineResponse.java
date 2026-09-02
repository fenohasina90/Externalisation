package com.cabex.backend.service.dto;

public record DomaineResponse(
        Integer id,
        String nomDomaine,
        String description,
        Boolean actif,
        long nombreServices
) {
}
