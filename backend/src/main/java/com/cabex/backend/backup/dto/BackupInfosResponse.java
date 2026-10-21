package com.cabex.backend.backup.dto;

public record BackupInfosResponse(
        String baseDeDonnees,
        String repertoire,
        int retentionJours,
        String heureSauvegarde,
        long tailleMaxUpload
) {
}
