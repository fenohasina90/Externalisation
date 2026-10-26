package com.cabex.backend.utilisateur.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UtilisateurResponse(
        Integer id,
        String nom,
        String prenom,
        String username,
        String email,
        Boolean actif,
        Boolean doitChangerMotDePasse,
        LocalDateTime derniereConnexion,
        LocalDateTime createdAt,
        RoleResponse role,
        List<String> permissions
) {
}
