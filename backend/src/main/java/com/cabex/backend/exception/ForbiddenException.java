package com.cabex.backend.exception;

/**
 * Utilisateur authentifié mais sans la permission requise.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
