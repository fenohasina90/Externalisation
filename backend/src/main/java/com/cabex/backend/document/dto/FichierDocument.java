package com.cabex.backend.document.dto;

public record FichierDocument(
        Integer id,
        String nomOriginal,
        String typeFichier,
        String cheminFichier,
        byte[] contenu
) {
}
