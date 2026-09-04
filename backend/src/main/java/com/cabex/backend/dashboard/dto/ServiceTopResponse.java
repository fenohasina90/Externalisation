package com.cabex.backend.dashboard.dto;

public record ServiceTopResponse(
        Integer serviceId,
        String nomService,
        long nombreDemandes
) {
}
