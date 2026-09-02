package com.cabex.backend.demande.dto;

import java.time.LocalDateTime;

public record HistoriqueDemandeResponse(
        Integer id,
        Integer statutId,
        String statut,
        String commentaire,
        Integer utilisateurId,
        String nomUtilisateur,
        LocalDateTime createdAt
) {
}
