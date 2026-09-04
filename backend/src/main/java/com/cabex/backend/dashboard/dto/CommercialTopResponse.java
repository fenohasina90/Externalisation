package com.cabex.backend.dashboard.dto;

public record CommercialTopResponse(
        Integer commercialId,
        String nomCommercial,
        long nombreDemandes
) {
}
