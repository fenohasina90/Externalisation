package com.cabex.backend.utilisateur.dto;

import java.util.List;

public record RoleDetailResponse(
        Integer id,
        String nomRole,
        String description,
        Boolean actif,
        List<PermissionResponse> permissions
) {
}
