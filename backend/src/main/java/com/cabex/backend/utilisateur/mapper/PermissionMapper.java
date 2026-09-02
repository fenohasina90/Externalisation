package com.cabex.backend.utilisateur.mapper;

import com.cabex.backend.utilisateur.dto.PermissionResponse;
import com.cabex.backend.utilisateur.entity.Permission;

public class PermissionMapper {

    private PermissionMapper() {
    }

    public static PermissionResponse toResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        return new PermissionResponse(
                permission.getId(),
                permission.getCodePermission(),
                permission.getNomPermission(),
                permission.getDescription(),
                permission.getModule(),
                permission.getActif()
        );
    }
}
