package com.cabex.backend.auth.controller;

import com.cabex.backend.auth.dto.PasswordResetRequestResponse;
import com.cabex.backend.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password-resets")
@Tag(name = "Réinitialisation mot de passe",
        description = "Consultation des demandes. Le mot de passe temporaire est généré et envoyé "
                + "directement à l'utilisateur par e-mail : il n'y a plus d'approbation ni de refus "
                + "côté administrateur.")
@SecurityRequirement(name = "bearerAuth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PASSWORD_RESET_MANAGE')")
    @Operation(summary = "Lister les demandes de réinitialisation",
            description = "Liste paginée des demandes, filtrable par statut "
                    + "(ENVOYEE, TERMINEE, EXPIREE, et EN_ATTENTE/APPROUVEE/REFUSEE pour l'historique)")
    public ResponseEntity<Page<PasswordResetRequestResponse>> lister(
            @RequestParam(required = false) String statut,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(passwordResetService.lister(statut, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PASSWORD_RESET_MANAGE')")
    @Operation(summary = "Consulter une demande de réinitialisation")
    public ResponseEntity<PasswordResetRequestResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(passwordResetService.detail(id));
    }
}
