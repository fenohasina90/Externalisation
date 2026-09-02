package com.cabex.backend.auth.dto;

import com.cabex.backend.utilisateur.dto.UtilisateurResponse;

public record AuthResponse(
        String token,
        String type,
        UtilisateurResponse utilisateur
) {
}
