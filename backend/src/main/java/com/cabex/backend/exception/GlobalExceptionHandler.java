package com.cabex.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationBusiness(ValidationException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                ex.getFieldErrors()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return new ResponseEntity<>(
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        "Données obligatoires manquantes ou invalides",
                        request.getDescription(false).replace("uri=", ""),
                        fieldErrors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                fieldErrors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return new ResponseEntity<>(
                new ApiError(
                        HttpStatus.BAD_REQUEST,
                        "Erreur de validation des paramètres",
                        request.getDescription(false).replace("uri=", ""),
                        fieldErrors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Format de la requête invalide", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Paramètre manquant : " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Type de paramètre invalide : " + ex.getName(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Ce compte est désactivé", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentification requise", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Vous n'avez pas les permissions nécessaires pour effectuer cette action", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "Conflit de données : cette référence est probablement déjà utilisée", request);
    }

    @ExceptionHandler(BackupException.class)
    public ResponseEntity<ApiError> handleBackup(BackupException ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue. Veuillez réessayer.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request) {
        ApiError error = new ApiError(status, message, request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, status);
    }
}
