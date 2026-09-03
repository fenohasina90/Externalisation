package com.cabex.backend.exception;

/**
 * Utilisateur non authentifié ou token invalide / expiré.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
