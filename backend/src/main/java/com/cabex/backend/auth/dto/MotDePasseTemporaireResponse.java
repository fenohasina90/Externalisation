package com.cabex.backend.auth.dto;

import java.time.LocalDateTime;

/**
 * Résultat de la génération d'un mot de passe temporaire.
 * Le mot de passe en clair n'est retourné qu'une seule fois, à l'administrateur
 * qui a généré la réinitialisation.
 */
public record MotDePasseTemporaireResponse(
        String motDePasseTemporaire,
        LocalDateTime expiration,
        String message,
        PasswordResetRequestResponse demande
) {
}