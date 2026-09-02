package com.cabex.backend.paiement.dto;

public record ModePaiementResponse(
        Integer id,
        String code,
        String libelle,
        Boolean actif
) {
}
