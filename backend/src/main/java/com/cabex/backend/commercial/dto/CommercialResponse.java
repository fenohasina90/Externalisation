package com.cabex.backend.commercial.dto;

public record CommercialResponse(
        Integer id,
        String nomCommercial,
        String immatricule,
        String email,
        String contact,
        Boolean actif
) {
}
