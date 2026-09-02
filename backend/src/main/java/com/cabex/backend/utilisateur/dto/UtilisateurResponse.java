package com.cabex.backend.utilisateur.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UtilisateurResponse(
        Integer id,
        String nom,
        String prenom,
        String email,
        Boolean actif,
        LocalDateTime derniereConnexion,
        LocalDateTime createdAt,
        RoleResponse role,
        List<String> permissions
) {
}
