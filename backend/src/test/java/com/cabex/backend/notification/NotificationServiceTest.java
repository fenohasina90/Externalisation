package com.cabex.backend.notification;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.facture.entity.FactureValidee;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Notifications de retard de paiement basées sur les factures validées. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    @InjectMocks private NotificationService notificationService;

    private Utilisateur superAdmin() {
        return Utilisateur.builder()
                .id(2).nom("Administrateur").prenom("Système")
                .username("admin").email("admin@cabex.mg").mdpHash("hash")
                .actif(true).build();
    }

    private Utilisateur economat() {
        return Utilisateur.builder()
                .id(3).nom("Économat").prenom("Équipe")
                .username("economat").email("economat@cabex.mg").mdpHash("hash")
                .actif(true).build();
    }

    private FactureValidee facture() {
        Societe societe = Societe.builder()
                .id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder()
                .id(1).referenceDemande("DEM-2026-00001").societe(societe).build();
        return FactureValidee.builder()
                .id(5).demande(demande).referenceFactureValidee("00001EXT/TNR/2026")
                .montant(new BigDecimal("400000"))
                .valideLe(LocalDateTime.of(LocalDate.of(2026, 11, 18), java.time.LocalTime.MIDNIGHT))
                .build();
    }

    private void preparerDestinataires() {
        when(utilisateurRepository.findByRoleNomRoleAndActifTrue("SUPER_ADMIN"))
                .thenReturn(List.of(superAdmin()));
        when(utilisateurRepository.findByRoleNomRoleAndActifTrue("ECONOMAT"))
                .thenReturn(List.of(economat()));
        lenient().when(notificationRepository.existsByUtilisateurIdAndTypeNotificationAndReferenceId(
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(false);
    }

    @Test
    void retardPaiementNotifiéAuxRolesFinanciersAvecLibelle() {
        preparerDestinataires();

        notificationService.notifierRetardPaiement(
                facture(), "Prestation — Conseil ", new BigDecimal("70000"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Notification> envoyees = captor.getAllValues();
        assertTrue(envoyees.stream().allMatch(n -> "PAIEMENT_RETARD".equals(n.getTypeNotification())));
        assertTrue(envoyees.stream().allMatch(n -> "FACTURE_VALIDEE".equals(n.getReferenceType())));
        assertEquals(5L, envoyees.get(0).getReferenceId());
        assertTrue(envoyees.get(0).getMessage().contains("00001EXT/TNR/2026"));
        assertTrue(envoyees.get(0).getMessage().contains("Atlas Conseil"));
        assertTrue(envoyees.get(0).getMessage().contains("DEM-2026-00001"));
        assertTrue(envoyees.get(0).getMessage().contains("Prestation — Conseil"));
    }

    @Test
    void retardPaiementAfficheLaDateDEmissonEnFormatFrancais() {
        preparerDestinataires();

        notificationService.notifierRetardPaiement(
                facture(), "Débours — Timbres fiscaux", new BigDecimal("12000"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        String message = captor.getAllValues().get(0).getMessage();
        assertTrue(message.contains("18/11/2026"));
    }

    @Test
    void doublonExistantEmpecheRenvoi() {
        preparerDestinataires();
        org.mockito.Mockito.doReturn(true).when(notificationRepository)
                .existsByUtilisateurIdAndTypeNotificationAndReferenceId(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.same("PAIEMENT_RETARD"),
                        org.mockito.ArgumentMatchers.same(5L));

        notificationService.notifierRetardPaiement(
                facture(), "Prestation", new BigDecimal("70000"));

        org.mockito.Mockito.verify(notificationRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }
}
