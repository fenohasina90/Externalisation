package com.cabex.backend.utilisateur.dto;

public record PermissionResponse(
        Integer id,
        String codePermission,
        String nomPermission,
        String description,
        String module,
        Boolean actif
) {
}
