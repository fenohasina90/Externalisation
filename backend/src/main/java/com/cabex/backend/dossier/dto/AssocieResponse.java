package com.cabex.backend.dossier.dto;

public record AssocieResponse(
        Integer id,
        Integer ordre,
        String nom,
        String partSociale,
        String email,
        String contact
) {
}
