package com.cabex.backend.audit;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.UserPrincipal;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.utilisateur.dto.CreateRoleRequest;
import com.cabex.backend.utilisateur.dto.CreateUtilisateurRequest;
import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.PermissionRepository;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import com.cabex.backend.utilisateur.service.RoleService;
import com.cabex.backend.utilisateur.service.UtilisateurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Journalisation des comptes, des rôles et des transitions automatiques.
 */
@ExtendWith(MockitoExtension.class)
class AuditUtilisateursTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RoleUtilisateurRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private SocieteRepository societeRepository;
    @Mock private ClientResponsableRepository responsableRepository;
    @Mock private CommercialRepository commercialRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private StatutDemandeRepository statutRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private NotificationService notificationService;
    @Mock private com.cabex.backend.demande.service.LigneServiceService ligneServiceService;
    @Mock private com.cabex.backend.facture.service.FactureService factureService;
    @Mock private com.cabex.backend.demande.repository.LigneServiceRepository ligneRepository;
    @Mock private ConfigurationService configurationService;

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    private void connecterAdmin() {
        UserPrincipal principal = new UserPrincipal(1, "admin", "admin@cabex.mg",
                "hash", true, 1, "SUPER_ADMIN", Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void creationUtilisateurJournalisee() {
        RoleUtilisateur role = RoleUtilisateur.builder().id(2).nomRole("ACCUEIL").actif(true).build();
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        when(utilisateurRepository.existsByUsernameIgnoreCase("sarah.cohen")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("sarah.cohen@cabinet-excellent.mg")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(5);
            return u;
        });
        UtilisateurService service = new UtilisateurService(
                utilisateurRepository, roleRepository, passwordEncoder, auditService);

        var reponse = service.create(new CreateUtilisateurRequest(
                "Cohen", "Sarah", "sarah.cohen", "sarah.cohen@cabinet-excellent.mg",
                "secret123", 2, true));

        assertNotNull(reponse);
        verify(auditService).log(eq("CREATE"), eq("UTILISATEUR"), eq("Utilisateur"),
                eq(5L), isNull(), contains("sarah.cohen"));
    }

    @Test
    void activationUtilisateurJournalisee() {
        connecterAdmin();
        Utilisateur cible = Utilisateur.builder().id(5).username("sarah.cohen")
                .email("sarah.cohen@cabinet-excellent.mg").actif(false).build();
        when(utilisateurRepository.findById(5)).thenReturn(Optional.of(cible));
        when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UtilisateurService service = new UtilisateurService(
                utilisateurRepository, roleRepository, passwordEncoder, auditService);

        service.toggleActivation(5, true);

        verify(auditService).log(eq("UPDATE"), eq("UTILISATEUR"), eq("Utilisateur"),
                eq(5L), contains("false"), contains("true"));
    }

    @Test
    void creationRoleJournalisee() {
        when(roleRepository.existsByNomRole("TESTEUR")).thenReturn(false);
        when(roleRepository.save(any())).thenAnswer(inv -> {
            RoleUtilisateur r = inv.getArgument(0);
            r.setId(9);
            return r;
        });
        RoleService service = new RoleService(roleRepository, permissionRepository, auditService);

        service.create(new CreateRoleRequest("Testeur", "Rôle de test", List.of()));

        verify(auditService).log(eq("CREATE"), eq("UTILISATEUR"), eq("Role"),
                eq(9L), isNull(), contains("TESTEUR"));
    }

}
