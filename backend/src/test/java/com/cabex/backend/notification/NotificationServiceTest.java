package com.cabex.backend.notification;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.notification.entity.Notification;
import com.cabex.backend.notification.repository.NotificationRepository;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Libellé de l'alerte « échéance proche » selon le nombre de jours restants. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    @InjectMocks private NotificationService notificationService;

    private EcheancePaiement echeance() {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder()
                .id(1).referenceDemande("DEM-2026-00001").societe(societe).build();
        ProposPaiement plan = ProposPaiement.builder()
                .id(1).demande(demande).build();
        return EcheancePaiement.builder()
                .id(5).proposPaiement(plan).numeroEcheance(1)
                .dateEcheance(LocalDate.now())
                .montantAttendu(new BigDecimal("100000"))
                .build();
    }

    private void preparerDestinataires() {
        Utilisateur admin = Utilisateur.builder()
                .id(2).nom("Administrateur").prenom("Système")
                .username("admin").email("admin@cabex.mg").mdpHash("hash")
                .actif(true).build();
        when(utilisateurRepository.findByRoleNomRoleAndActifTrue("SUPER_ADMIN"))
                .thenReturn(List.of(admin));
        when(utilisateurRepository.findByRoleNomRoleAndActifTrue("ECONOMAT"))
                .thenReturn(List.of());
    }

    private String messageEcheanceProche(int jours) {
        preparerDestinataires();
        notificationService.notifierEcheanceProche(echeance(), jours);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue().getMessage();
    }

    @Test
    void echeanceDuJourAfficheAujourdHui() {
        assertTrue(messageEcheanceProche(0).contains("arrive à échéance aujourd'hui"));
    }

    @Test
    void echeanceDemainAfficheDemain() {
        assertTrue(messageEcheanceProche(1).contains("arrive à échéance demain"));
    }

    @Test
    void echeanceDansPlusieursJoursAfficheLeNombre() {
        assertTrue(messageEcheanceProche(3).contains("arrive à échéance dans 3 jours"));
    }
}
