package com.cabex.backend.audit.mapper;

import com.cabex.backend.audit.dto.AuditLogResponse;
import com.cabex.backend.audit.entity.AuditLog;

public class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new AuditLogResponse(
                log.getId(),
                log.getUtilisateur() != null ? log.getUtilisateur().getId() : null,
                log.getUtilisateur() != null
                        ? log.getUtilisateur().getPrenom() + " " + log.getUtilisateur().getNom()
                        : null,
                log.getAction(),
                log.getModule(),
                log.getEntite(),
                log.getEntiteId(),
                log.getAncienneValeur(),
                log.getNouvelleValeur(),
                log.getAdresseIp(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
