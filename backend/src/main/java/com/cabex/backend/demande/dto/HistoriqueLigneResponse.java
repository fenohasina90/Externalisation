package com.cabex.backend.demande.dto;

import java.time.LocalDateTime;

public record HistoriqueLigneResponse(
        Integer id,
        Integer ligneServiceId,
        String statut,
        String ancienStatut,
        String commentaire,
        Integer utilisateurId,
        String nomUtilisateur,
        String origine,
        LocalDateTime createdAt
) {
}
