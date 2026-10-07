package com.cabex.backend.auth.service;

import com.cabex.backend.auth.dto.AuthResponse;
import com.cabex.backend.auth.dto.ChangePasswordRequest;
import com.cabex.backend.auth.dto.LoginRequest;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.exception.UnauthorizedException;
import com.cabex.backend.security.JwtService;
import com.cabex.backend.security.UserPrincipal;
import com.cabex.backend.security.CustomUserDetailsService;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.mapper.UtilisateurMapper;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur")));

            if (Boolean.TRUE.equals(utilisateur.getDoitChangerMdp())
                    && utilisateur.getDateExpirationMdpTemporaire() != null
                    && utilisateur.getDateExpirationMdpTemporaire().isBefore(LocalDateTime.now())) {
                throw new UnauthorizedException(ErrorMessages.MDP_TEMPORAIRE_EXPIRE);
            }

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
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur", principal.getId())));

        if (!passwordEncoder.matches(request.currentPassword(), utilisateur.getMdpHash())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }

        boolean changementForce = Boolean.TRUE.equals(utilisateur.getDoitChangerMdp());

        utilisateur.setMdpHash(passwordEncoder.encode(request.newPassword()));
        utilisateur.setDoitChangerMdp(false);
        utilisateur.setDateExpirationMdpTemporaire(null);
        utilisateurRepository.save(utilisateur);

        if (changementForce) {
            passwordResetService.terminerDemandesApresChangement(utilisateur);
        }
    }
}
