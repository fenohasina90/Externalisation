package com.cabex.backend.demande.dto;

public record StatutResponse(
        Integer id,
        String statut,
        String description,
        Integer ordre,
        Boolean actif
) {
}
