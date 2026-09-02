package com.cabex.backend.document.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Integer id,
        Integer demandeId,
        String nomFichier,
        String nomOriginal,
        String typeFichier,
        Long tailleFichier,
        String description,
        Integer uploadedById,
        String uploadedByName,
        LocalDateTime createdAt
) {
}
