package com.cabex.backend.utilisateur.service;

import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.dto.CreateUtilisateurRequest;
import com.cabex.backend.utilisateur.dto.UpdateUtilisateurRequest;
import com.cabex.backend.utilisateur.dto.UtilisateurResponse;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.mapper.UtilisateurMapper;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleUtilisateurRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              RoleUtilisateurRepository roleRepository,
                              PasswordEncoder passwordEncoder,
                              AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UtilisateurResponse> getAll(String search, Integer roleId, Boolean actif,
                                            Pageable pageable) {
        return utilisateurRepository.findWithFilters(search, roleId, actif, pageable)
                .map(UtilisateurMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getById(Integer id) {
        return UtilisateurMapper.toResponse(getEntity(id));
    }

    @Transactional
    public UtilisateurResponse create(CreateUtilisateurRequest request) {
        String username = request.username().trim();
        if (utilisateurRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Un utilisateur avec l'username '" + username + "' existe déjà");
        }
        if (utilisateurRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new DuplicateResourceException("Un utilisateur avec l'email '" + request.email() + "' existe déjà");
        }
        RoleUtilisateur role = getRole(request.roleId());

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.nom().trim())
                .prenom(request.prenom().trim())
                .username(username)
                .email(request.email().trim().toLowerCase())
                .mdpHash(passwordEncoder.encode(request.password()))
                .role(role)
                .actif(request.actif() == null || request.actif())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        auditService.log(AuditActions.CREATE, "UTILISATEUR", "Utilisateur",
                Long.valueOf(saved.getId()), null, "username=" + saved.getUsername());
        return UtilisateurMapper.toResponse(saved);
    }

    @Transactional
    public UtilisateurResponse update(Integer id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = getEntity(id);

        String username = request.username().trim();
        if (!utilisateur.getUsername().equalsIgnoreCase(username)
                && utilisateurRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Un utilisateur avec l'username '" + username + "' existe déjà");
        }

        String email = request.email().trim().toLowerCase();
        if (!utilisateur.getEmail().equalsIgnoreCase(email)
                && utilisateurRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Un utilisateur avec l'email '" + request.email() + "' existe déjà");
        }

        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setUsername(username);
        utilisateur.setEmail(email);
        if (request.roleId() != null) {
            utilisateur.setRole(getRole(request.roleId()));
        }
        utilisateur.setUpdatedAt(LocalDateTime.now());
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        auditService.log(AuditActions.UPDATE, "UTILISATEUR", "Utilisateur",
                Long.valueOf(saved.getId()), null, "username=" + saved.getUsername());
        return UtilisateurMapper.toResponse(saved);
    }

    @Transactional
    public UtilisateurResponse toggleActivation(Integer id, boolean actif) {
        Utilisateur utilisateur = getEntity(id);
        if (SecurityUtils.currentUserId().equals(id)) {
            throw new BadRequestException("Vous ne pouvez pas modifier votre propre compte");
        }
        boolean previousActif = utilisateur.getActif();
        utilisateur.setActif(actif);
        utilisateur.setUpdatedAt(LocalDateTime.now());
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        auditService.log(AuditActions.UPDATE, "UTILISATEUR", "Utilisateur",
                Long.valueOf(saved.getId()), "actif=" + previousActif, "actif=" + actif);
        return UtilisateurMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Utilisateur utilisateur = getEntity(id);
        if (SecurityUtils.currentUserId().equals(id)) {
            throw new BadRequestException("Vous ne pouvez pas supprimer votre propre compte");
        }
        String username = utilisateur.getUsername();
        utilisateurRepository.delete(utilisateur);
        auditService.log(AuditActions.DELETE, "UTILISATEUR", "Utilisateur",
                Long.valueOf(id), "username=" + username, null);
    }

    private Utilisateur getEntity(Integer id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Utilisateur", id)));
    }

    private RoleUtilisateur getRole(Integer roleId) {
        if (roleId == null) {
            throw new BadRequestException(ErrorMessages.champObligatoire("roleId"));
        }
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Rôle", roleId)));
    }
}
