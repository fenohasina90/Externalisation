package com.cabex.backend.utilisateur.service;

import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.utilisateur.dto.CreateRoleRequest;
import com.cabex.backend.utilisateur.dto.RoleDetailResponse;
import com.cabex.backend.utilisateur.dto.RoleResponse;
import com.cabex.backend.utilisateur.dto.UpdateRoleRequest;
import com.cabex.backend.utilisateur.entity.Permission;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.mapper.UtilisateurMapper;
import com.cabex.backend.utilisateur.repository.PermissionRepository;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleUtilisateurRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public RoleService(RoleUtilisateurRepository roleRepository,
                       PermissionRepository permissionRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream()
                .map(UtilisateurMapper::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getById(Integer id) {
        return UtilisateurMapper.toRoleDetailResponse(getEntity(id));
    }

    @Transactional
    public RoleDetailResponse create(CreateRoleRequest request) {
        if (request.nomRole() == null || request.nomRole().isBlank()) {
            throw new BadRequestException(ErrorMessages.champObligatoire("nomRole"));
        }
        String nom = request.nomRole().trim().toUpperCase();
        if (roleRepository.existsByNomRole(nom)) {
            throw new DuplicateResourceException("Un rôle avec le nom '" + nom + "' existe déjà");
        }

        RoleUtilisateur role = new RoleUtilisateur();
        role.setNomRole(nom);
        role.setDescription(request.description());
        role.setActif(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setPermissions(resolvePermissions(request.permissionIds()));

        RoleUtilisateur saved = roleRepository.save(role);
        auditService.log(AuditActions.CREATE, "UTILISATEUR", "Role",
                Long.valueOf(saved.getId()), null, "role=" + saved.getNomRole());
        return UtilisateurMapper.toRoleDetailResponse(saved);
    }

    @Transactional
    public RoleDetailResponse update(Integer id, UpdateRoleRequest request) {
        RoleUtilisateur role = getEntity(id);
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.actif() != null) {
            role.setActif(request.actif());
        }
        if (request.permissionIds() != null) {
            role.setPermissions(resolvePermissions(request.permissionIds()));
        }
        role.setUpdatedAt(LocalDateTime.now());
        RoleUtilisateur saved = roleRepository.save(role);
        auditService.log(AuditActions.UPDATE, "UTILISATEUR", "Role",
                Long.valueOf(saved.getId()), null, "role=" + saved.getNomRole());
        return UtilisateurMapper.toRoleDetailResponse(saved);
    }

    private RoleUtilisateur getEntity(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Rôle", id)));
    }

    private Set<Permission> resolvePermissions(List<Integer> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Integer> uniqueIds = new HashSet<>(permissionIds);
        List<Permission> permissions = permissionRepository.findAllById(uniqueIds);
        if (permissions.size() != uniqueIds.size()) {
            throw new BadRequestException("Certaines permissions demandées n'existent pas");
        }
        return new HashSet<>(permissions);
    }
}
