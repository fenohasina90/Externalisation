package com.cabex.backend.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception de validation métier / règles d'intégrité.
 * Utilisée quand les données passent la validation Bean (@Valid)
 * mais violent une règle métier (dates incohérentes, référence invalide, etc.).
 */
@Getter
public class ValidationException extends RuntimeException {

    private final transient Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = null;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public static ValidationException forField(String field, String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        return new ValidationException("Erreur de validation métier", errors);
    }
}
