package com.cabex.backend.utilisateur.dto;

import java.util.List;

public record UpdateRoleRequest(
        String description,
        Boolean actif,
        List<Integer> permissionIds
) {
}
