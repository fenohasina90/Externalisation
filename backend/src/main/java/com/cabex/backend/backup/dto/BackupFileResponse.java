package com.cabex.backend.backup.dto;

import java.time.LocalDateTime;

public record BackupFileResponse(
        String nomFichier,
        long tailleOctets,
        LocalDateTime dateCreation,
        String type,
        boolean chiffre
) {
}
