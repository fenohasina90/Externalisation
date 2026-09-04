package com.cabex.backend.client.dto;

import java.time.LocalDateTime;

public record PieceJointeFicheResponse(
        Integer id,
        String nomOriginal,
        String typeFichier,
        Long tailleFichier,
        String description,
        Integer demandeId,
        String referenceDemande,
        LocalDateTime createdAt
) {
}
