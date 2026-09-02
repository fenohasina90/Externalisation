package com.cabex.backend.utilisateur.dto;

import java.time.LocalDateTime;

public record UpdateProfilRequest(
        String nom,
        String prenom
) {
}
