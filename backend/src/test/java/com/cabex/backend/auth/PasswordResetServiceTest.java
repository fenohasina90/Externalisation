package com.cabex.backend.auth;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.auth.dto.ForgotPasswordRequest;
import com.cabex.backend.auth.service.PasswordResetService;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import com.cabex.backend.mail.MailService;
import com.cabex.backend.utilisateur.entity.DemandeReinitialisation;
import com.cabex.backend.utilisateur.entity.DemandeReinitialisationStatut;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.DemandeReinitialisationRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private DemandeReinitialisationRepository demandeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ConfigurationRepository configurationRepository;
    @Mock
    private MailService mailService;
    @Mock
    private AuditService auditService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService service;

    private Utilisateur utilisateur() {
        return Utilisateur.builder()
                .id(3)
                .nom("Rakoto")
                .prenom("Andry")
                .username("andry")
                .email("andry@cabex.mg")
                .actif(true)
                .build();
    }

    @Test
    void envoieLeMotDePasseTemporaireParEmailSansApprovaluation() {
        Utilisateur utilisateur = utilisateur();
        when(utilisateurRepository.findByEmail("andry@cabex.mg")).thenReturn(Optional.of(utilisateur));
        when(demandeRepository.existsByUtilisateurIdAndStatutIn(eq(3), anyList())).thenReturn(false);
        when(configurationRepository.findByCle("DUREE_VALIDITE_MDP_TEMPORAIRE")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(demandeRepository.save(any(DemandeReinitialisation.class))).thenAnswer(invocation -> {
            DemandeReinitialisation demande = invocation.getArgument(0);
            demande.setId(9L);
            return demande;
        });

        service.demanderReinitialisation(new ForgotPasswordRequest("  andry@cabex.mg  "));

        ArgumentCaptor<DemandeReinitialisation> demandeCaptor =
                ArgumentCaptor.forClass(DemandeReinitialisation.class);
        verify(demandeRepository).save(demandeCaptor.capture());
        DemandeReinitialisation demande = demandeCaptor.getValue();
        assertEquals(DemandeReinitialisationStatut.ENVOYEE, demande.getStatut());
        assertNotNull(demande.getDateExpirationMdp());

        assertEquals("hash-bcrypt", utilisateur.getMdpHash());
        assertTrue(utilisateur.getDoitChangerMdp());
        assertNotNull(utilisateur.getDateExpirationMdpTemporaire());

        ArgumentCaptor<String> motDePasseCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).envoyerMotDePasseTemporaire(
                eq("andry@cabex.mg"), eq("Andry Rakoto"), motDePasseCaptor.capture(), eq(1440));
        assertEquals(10, motDePasseCaptor.getValue().length());

        verify(auditService, times(2)).log(anyString(), anyString(), anyString(), any(), any(), any());
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    void emailInconnuNeDeclencheRien() {
        when(utilisateurRepository.findByEmail("inconnu@cabex.mg")).thenReturn(Optional.empty());

        service.demanderReinitialisation(new ForgotPasswordRequest("inconnu@cabex.mg"));

        verifyNoInteractions(mailService, demandeRepository, auditService, passwordEncoder);
    }

    @Test
    void demandeDejaActiveNeGenerePasUnSecondMotDePasse() {
        Utilisateur utilisateur = utilisateur();
        when(utilisateurRepository.findByEmail("andry@cabex.mg")).thenReturn(Optional.of(utilisateur));
        when(demandeRepository.existsByUtilisateurIdAndStatutIn(eq(3), anyList())).thenReturn(true);

        service.demanderReinitialisation(new ForgotPasswordRequest("andry@cabex.mg"));

        verifyNoInteractions(mailService, auditService, passwordEncoder);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void echecDenvoiLeveUneExceptionSansTracerDeSucces() {
        Utilisateur utilisateur = utilisateur();
        when(utilisateurRepository.findByEmail("andry@cabex.mg")).thenReturn(Optional.of(utilisateur));
        when(demandeRepository.existsByUtilisateurIdAndStatutIn(eq(3), anyList())).thenReturn(false);
        when(configurationRepository.findByCle("DUREE_VALIDITE_MDP_TEMPORAIRE")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(demandeRepository.save(any(DemandeReinitialisation.class))).thenAnswer(invocation -> {
            DemandeReinitialisation demande = invocation.getArgument(0);
            demande.setId(9L);
            return demande;
        });
        doThrow(new IllegalStateException("SMTP indisponible")).when(mailService)
                .envoyerMotDePasseTemporaire(anyString(), anyString(), anyString(), anyInt());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.demanderReinitialisation(new ForgotPasswordRequest("andry@cabex.mg")));

        assertTrue(ex.getMessage().contains("Impossible d'envoyer"));
        verifyNoInteractions(auditService);
    }
}
