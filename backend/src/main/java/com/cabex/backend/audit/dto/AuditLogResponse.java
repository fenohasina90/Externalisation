package com.cabex.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Integer utilisateurId,
        String nomUtilisateur,
        String action,
        String module,
        String entite,
        Long entiteId,
        String ancienneValeur,
        String nouvelleValeur,
        String adresseIp,
        String userAgent,
        LocalDateTime createdAt
) {
}
