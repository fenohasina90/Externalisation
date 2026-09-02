package com.cabex.backend.client.dto;

public record ResponsableResponse(
        Integer id,
        Integer societeId,
        String nomSociete,
        String nomClient,
        String contact,
        String email,
        String fonction,
        Boolean actif
) {
}
