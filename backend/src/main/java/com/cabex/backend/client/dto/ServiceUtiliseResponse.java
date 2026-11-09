package com.cabex.backend.client.dto;

public record ServiceUtiliseResponse(
        Integer serviceId,
        String nomService,
        long nombreDemandes
) {
}
