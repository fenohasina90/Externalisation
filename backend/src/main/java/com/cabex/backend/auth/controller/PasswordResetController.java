package com.cabex.backend.auth.controller;

import com.cabex.backend.auth.dto.MotDePasseTemporaireResponse;
import com.cabex.backend.auth.dto.PasswordResetRequestResponse;
import com.cabex.backend.auth.dto.RefuserDemandeRequest;
import com.cabex.backend.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password-resets")
@Tag(name = "Réinitialisation mot de passe",
        description = "Validation par un administrateur des demandes de réinitialisation "
                + "et génération d'un mot de passe temporaire")
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
                    + "(EN_ATTENTE, APPROUVEE, REFUSEE, EXPIREE, TERMINEE)")
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

    @PostMapping("/{id}/approuver")
    @PreAuthorize("hasAuthority('PASSWORD_RESET_MANAGE')")
    @Operation(summary = "Approuver la demande",
            description = "Marque la demande comme approuvée. L'administrateur peut ensuite "
                    + "générer un mot de passe temporaire.")
    public ResponseEntity<PasswordResetRequestResponse> approuver(@PathVariable Long id) {
        return ResponseEntity.ok(passwordResetService.approuver(id));
    }

    @PostMapping("/{id}/refuser")
    @PreAuthorize("hasAuthority('PASSWORD_RESET_MANAGE')")
    @Operation(summary = "Refuser la demande",
            description = "Refuse une demande de réinitialisation en attente ; le motif est obligatoire")
    public ResponseEntity<PasswordResetRequestResponse> refuser(
            @PathVariable Long id,
            @Valid @RequestBody RefuserDemandeRequest request) {
        return ResponseEntity.ok(passwordResetService.refuser(id, request.motif().trim()));
    }

    @PostMapping("/{id}/reinitialiser")
    @PreAuthorize("hasAuthority('PASSWORD_RESET_MANAGE')")
    @Operation(summary = "Générer un mot de passe temporaire",
            description = "Génère un mot de passe temporaire pour la demande approuvée. Le mot de passe "
                    + "en clair n'est retourné qu'une seule fois ; l'utilisateur devra le changer "
                    + "à sa prochaine connexion.")
    public ResponseEntity<MotDePasseTemporaireResponse> reinitialiser(@PathVariable Long id) {
        return ResponseEntity.ok(passwordResetService.reinitialiser(id));
    }
}