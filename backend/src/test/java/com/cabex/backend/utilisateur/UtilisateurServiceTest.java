package com.cabex.backend.utilisateur;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.utilisateur.dto.CreateUtilisateurRequest;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import com.cabex.backend.utilisateur.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Création, filtres de liste et activation.
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private RoleUtilisateurRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private UtilisateurService utilisateurService;

    @BeforeEach
    void setUp() {
        utilisateurService = new UtilisateurService(
                utilisateurRepository, roleRepository, passwordEncoder, auditService);
    }

    private RoleUtilisateur roleAccueil() {
        return RoleUtilisateur.builder().id(2).nomRole("ACCUEIL").actif(true).build();
    }

    @Test
    void createOkActifParDefaut() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(roleAccueil()));
        when(utilisateurRepository.existsByUsername("sarah.cohen")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("sarah.cohen@cabinet-excellent.mg")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(5);
            return u;
        });

        var reponse = utilisateurService.create(new CreateUtilisateurRequest(
                "Cohen", "Sarah", "sarah.cohen", "sarah.cohen@cabinet-excellent.mg",
                "secret123", 2, null));

        assertEquals("sarah.cohen", reponse.username());
        assertTrue(reponse.actif());
        verify(passwordEncoder).encode("secret123");
        verify(auditService).log(eq("CREATE"), eq("UTILISATEUR"), eq("Utilisateur"),
                eq(5L), isNull(), contains("sarah.cohen"));
    }

    @Test
    void createUsernameDuplique409() {
        when(utilisateurRepository.existsByUsername("sarah.cohen")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> utilisateurService.create(new CreateUtilisateurRequest(
                        "Cohen", "Sarah", "sarah.cohen", "sarah.cohen@cabinet-excellent.mg",
                        "secret123", 2, true)));
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void listeFiltreeParRoleEtStatut() {
        Utilisateur u = Utilisateur.builder().id(5).username("sarah.cohen")
                .email("sarah.cohen@cabinet-excellent.mg").actif(true).role(roleAccueil()).build();
        when(utilisateurRepository.findWithFilters("sarah", 2, true, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(u)));

        var page = utilisateurService.getAll("sarah", 2, true, PageRequest.of(0, 8));

        assertEquals(1, page.getTotalElements());
        verify(utilisateurRepository).findWithFilters("sarah", 2, true, PageRequest.of(0, 8));
    }

    @Test
    void getByIdInexistant404() {
        when(utilisateurRepository.findById(999)).thenReturn(Optional.empty());

        var ex = assertThrows(com.cabex.backend.exception.ResourceNotFoundException.class,
                () -> utilisateurService.getById(999));
        assertEquals(ErrorMessages.introuvable("Utilisateur", 999), ex.getMessage());
    }
}
