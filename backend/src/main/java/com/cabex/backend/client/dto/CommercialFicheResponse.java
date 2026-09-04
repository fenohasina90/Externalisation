package com.cabex.backend.client.dto;

public record CommercialFicheResponse(
        Integer id,
        String nomCommercial,
        String immatricule,
        String email,
        String contact
) {
}
