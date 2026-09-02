package com.cabex.backend.utilisateur.dto;

import java.util.List;

public record CreateRoleRequest(
        String nomRole,
        String description,
        List<Integer> permissionIds
) {
}
