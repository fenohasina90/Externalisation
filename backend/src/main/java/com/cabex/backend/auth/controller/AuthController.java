package com.cabex.backend.auth.controller;

import com.cabex.backend.auth.dto.AuthResponse;
import com.cabex.backend.auth.dto.ChangePasswordRequest;
import com.cabex.backend.auth.dto.ForgotPasswordRequest;
import com.cabex.backend.auth.dto.LoginRequest;
import com.cabex.backend.auth.dto.ResetPasswordRequest;
import com.cabex.backend.auth.service.AuthService;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Connexion, mot de passe et session utilisateur (JWT)")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Email/mot de passe incorrect ou compte désactivé"),
            @ApiResponse(responseCode = "400", description = "Données de connexion invalides")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil connecté", description = "Retourne les informations de l'utilisateur actuellement authentifié")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil retourné"),
            @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
    })
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Mot de passe oublié", description = "Génère un lien de réinitialisation pour l'email fourni (réponse identique si l'email n'existe pas)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande traitée"),
            @ApiResponse(responseCode = "400", description = "Email invalide")
    })
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Si cet email existe, un lien de réinitialisation a été envoyé."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialisation du mot de passe", description = "Définit un nouveau mot de passe à l'aide du token reçu par email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Votre mot de passe a été réinitialisé avec succès."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changement de mot de passe", description = "Change le mot de passe de l'utilisateur connecté après vérification de l'ancien")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe changé"),
            @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return ResponseEntity.ok(Map.of("message", "Votre mot de passe a été changé avec succès."));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Endpoint de déconnexion (le client doit supprimer son token ; le JWT est sans état)")
    public ResponseEntity<HttpStatus.Series> logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            auditService.log(AuditActions.LOGOUT, "AUTH", "Utilisateur",
                    principal.getId().longValue(), null, null);
        }
        return ResponseEntity.noContent().build();
    }
}
