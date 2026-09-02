package com.cabex.backend.utilisateur.dto;

public record RoleResponse(
        Integer id,
        String nomRole,
        String description,
        Boolean actif
) {
}
