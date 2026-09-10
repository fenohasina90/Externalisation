package com.cabex.backend.utilisateur.mapper;

import com.cabex.backend.utilisateur.dto.RoleDetailResponse;
import com.cabex.backend.utilisateur.dto.RoleResponse;
import com.cabex.backend.utilisateur.dto.UtilisateurResponse;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.entity.Utilisateur;

import java.util.stream.Collectors;

public class UtilisateurMapper {

    private UtilisateurMapper() {
    }

    public static RoleResponse toRoleResponse(RoleUtilisateur role) {
        if (role == null) {
            return null;
        }
        return new RoleResponse(
                role.getId(),
                role.getNomRole(),
                role.getDescription(),
                role.getActif()
        );
    }

    public static RoleDetailResponse toRoleDetailResponse(RoleUtilisateur role) {
        if (role == null) {
            return null;
        }
        return new RoleDetailResponse(
                role.getId(),
                role.getNomRole(),
                role.getDescription(),
                role.getActif(),
                role.getPermissions().stream()
                        .map(PermissionMapper::toResponse)
                        .collect(Collectors.toList())
        );
    }

    public static UtilisateurResponse toResponse(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return new UtilisateurResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getUsername(),
                utilisateur.getEmail(),
                utilisateur.getActif(),
                utilisateur.getDerniereConnexion(),
                utilisateur.getCreatedAt(),
                toRoleResponse(utilisateur.getRole()),
                utilisateur.getRole() == null ? java.util.List.of()
                        : utilisateur.getRole().getPermissions().stream()
                                .map(p -> p.getCodePermission())
                                .collect(Collectors.toList())
        );
    }
}
