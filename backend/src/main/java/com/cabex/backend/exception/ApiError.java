package com.cabex.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class ApiError {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Map<String, String> fieldErrors;

    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, path, null);
    }

    public ApiError(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public ApiError(HttpStatus status, String message, String path) {
        this(status.value(), status.name(), message, path, null);
    }

    public ApiError(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        this(status.value(), status.name(), message, path, fieldErrors);
    }

    private static String codeFromStatus(HttpStatus status) {
        return status.name().replace("_", "_");
    }
}
