package com.cabex.backend.client.dto;

public record ServiceUtiliseResponse(
        Integer serviceId,
        String nomService,
        Integer domaineId,
        String nomDomaine,
        long nombreDemandes
) {
}
