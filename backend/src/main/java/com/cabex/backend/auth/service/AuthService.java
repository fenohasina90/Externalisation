package com.cabex.backend.auth.service;

import com.cabex.backend.auth.dto.AuthResponse;
import com.cabex.backend.auth.dto.ChangePasswordRequest;
import com.cabex.backend.auth.dto.ForgotPasswordRequest;
import com.cabex.backend.auth.dto.LoginRequest;
import com.cabex.backend.auth.dto.ResetPasswordRequest;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.exception.UnauthorizedException;
import com.cabex.backend.security.JwtService;
import com.cabex.backend.security.UserPrincipal;
import com.cabex.backend.security.CustomUserDetailsService;
import com.cabex.backend.utilisateur.entity.PasswordResetToken;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.mapper.UtilisateurMapper;
import com.cabex.backend.utilisateur.repository.PasswordResetTokenRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_VALIDITY_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UtilisateurRepository utilisateurRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur")));

            utilisateur.setDerniereConnexion(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);

            auditService.log(utilisateur, AuditActions.LOGIN, "AUTH", "Utilisateur",
                    utilisateur.getId().longValue(), null, null);

            String token = jwtService.generateToken(principal);
            return new AuthResponse(token, "Bearer", UtilisateurMapper.toResponse(utilisateur));
        } catch (DisabledException ex) {
            throw new UnauthorizedException(ErrorMessages.COMPTE_DESACTIVE);
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(ErrorMessages.IDENTIFIANTS_INCORECTS);
        }
    }

    public AuthResponse me(UserPrincipal principal) {
        Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur", principal.getId())));
        return new AuthResponse(null, "Bearer", UtilisateurMapper.toResponse(utilisateur));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email().trim()).orElse(null);
        // Ne pas révéler si l'email existe : réponse identique dans tous les cas
        if (utilisateur == null) {
            log.info("Demande de réinitialisation pour un email inconnu : {}", request.email());
            return;
        }

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .utilisateur(utilisateur)
                .tokenHash(tokenHash)
                .expiration(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES))
                .utilise(false)
                .build();
        resetTokenRepository.save(resetToken);

        // En production : envoyer un email. Ici on journalise le lien de réinitialisation.
        log.info("Lien de réinitialisation pour {} : {}", utilisateur.getEmail(), rawToken);
        // TODO: brancher un MailSender / Notification pour l'envoi réel du lien.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException(ErrorMessages.TOKEN_INVALIDE));

        if (Boolean.TRUE.equals(resetToken.getUtilise())) {
            throw new BadRequestException(ErrorMessages.TOKEN_INVALIDE);
        }
        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(ErrorMessages.TOKEN_INVALIDE);
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setMdpHash(passwordEncoder.encode(request.newPassword()));
        utilisateurRepository.save(utilisateur);

        resetToken.setUtilise(true);
        resetTokenRepository.save(resetToken);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur", principal.getId())));

        if (!passwordEncoder.matches(request.currentPassword(), utilisateur.getMdpHash())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }

        utilisateur.setMdpHash(passwordEncoder.encode(request.newPassword()));
        utilisateurRepository.save(utilisateur);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors du hachage du token", e);
        }
    }
}
