package com.cabex.backend.auth.dto;

import java.time.LocalDateTime;

public record PasswordResetRequestResponse(
        Long id,
        Integer utilisateurId,
        String nomComplet,
        String username,
        String email,
        String statut,
        String motifRefus,
        LocalDateTime dateDemande,
        LocalDateTime dateTraitement,
        String traiteePar,
        LocalDateTime dateGenerationMdp,
        LocalDateTime dateExpirationMdp,
        String genereePar
) {
}