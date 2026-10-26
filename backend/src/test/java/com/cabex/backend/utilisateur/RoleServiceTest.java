package com.cabex.backend.utilisateur;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.utilisateur.dto.CreateRoleRequest;
import com.cabex.backend.utilisateur.dto.RoleDetailResponse;
import com.cabex.backend.utilisateur.dto.UpdateRoleRequest;
import com.cabex.backend.utilisateur.entity.Permission;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.repository.PermissionRepository;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Création et mise à jour des rôles (permissions résolues par identifiants).
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleUtilisateurRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuditService auditService;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, permissionRepository, auditService);
    }

    private Permission permission(Integer id, String code) {
        return Permission.builder().id(id).codePermission(code).nomPermission(code)
                .module("DEMANDE").actif(true).build();
    }

    @Test
    void updateRemplaceLesPermissions() {
        RoleUtilisateur role = RoleUtilisateur.builder().id(2).nomRole("ACCUEIL")
                .actif(true).permissions(Set.of(permission(1, "DEMANDE_VIEW"))).build();
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        Permission create = permission(2, "DEMANDE_CREATE");
        when(permissionRepository.findAllById(Set.of(1, 2))).thenReturn(List.of(permission(1, "DEMANDE_VIEW"), create));
        when(roleRepository.save(any(RoleUtilisateur.class))).thenAnswer(i -> i.getArgument(0));

        RoleDetailResponse resultat = roleService.update(2, new UpdateRoleRequest(null, null, List.of(1, 2)));

        assertEquals(2, resultat.permissions().size());
        verify(auditService).log(any(), eq("UTILISATEUR"), eq("Role"), eq(2L), any(), any());
    }

    @Test
    void updateRoleInexistant() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> roleService.update(99, new UpdateRoleRequest(null, null, List.of())));
    }

    @Test
    void updatePermissionInexistante() {
        RoleUtilisateur role = RoleUtilisateur.builder().id(2).nomRole("ACCUEIL").actif(true).build();
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(Set.of(999))).thenReturn(List.of());
        assertThrows(BadRequestException.class,
                () -> roleService.update(2, new UpdateRoleRequest(null, null, List.of(999))));
    }

    @Test
    void createNomDuplique() {
        when(roleRepository.existsByNomRole("ACCUEIL")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> roleService.create(new CreateRoleRequest("accueil", null, List.of())));
    }
}
