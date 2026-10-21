package com.cabex.backend.notification;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.notification.service.NotificationScheduler;
import com.cabex.backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Le seuil d'alerte « échéance proche » est lu depuis la configuration
 * (JOURS_AVANCE_ALERTE_ECHEANCE) à chaque exécution.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private EcheancePaiementRepository echeanceRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private NotificationScheduler scheduler;

    private EcheancePaiement echeanceDans(int jours) {
        EcheancePaiement echeance = mock(EcheancePaiement.class);
        lenient().when(echeance.getDateEcheance()).thenReturn(LocalDate.now().plusDays(jours));
        return echeance;
    }

    @Test
    void seuilLuDepuisLaConfiguration() {
        EcheancePaiement echeance = echeanceDans(5);
        when(echeanceRepository.findEnRetard(any())).thenReturn(List.of());
        when(echeanceRepository.findAVenir(any())).thenReturn(List.of(echeance));
        when(configurationService.lireEntier("JOURS_AVANCE_ALERTE_ECHEANCE", 3)).thenReturn(7);

        scheduler.verifierEcheances();

        verify(notificationService).notifierEcheanceProche(any(EcheancePaiement.class), eq(5));
    }

    @Test
    void echeanceDuJourNotifieeAvecZero() {
        EcheancePaiement echeance = echeanceDans(0);
        when(echeanceRepository.findEnRetard(any())).thenReturn(List.of());
        when(echeanceRepository.findAVenir(any())).thenReturn(List.of(echeance));
        when(configurationService.lireEntier("JOURS_AVANCE_ALERTE_ECHEANCE", 3)).thenReturn(3);

        scheduler.verifierEcheances();

        verify(notificationService).notifierEcheanceProche(any(EcheancePaiement.class), eq(0));
    }

    @Test
    void echeanceEnRetardNotifie() {
        EcheancePaiement echeance = echeanceDans(-2);
        when(echeanceRepository.findEnRetard(any())).thenReturn(List.of(echeance));
        when(echeanceRepository.findAVenir(any())).thenReturn(List.of());

        scheduler.verifierEcheances();

        verify(notificationService).notifierEcheanceEnRetard(echeance);
        verify(notificationService, never()).notifierEcheanceProche(any(EcheancePaiement.class), anyInt());
    }

    @Test
    void horsSeuilPasDeNotification() {
        EcheancePaiement echeance = echeanceDans(5);
        when(echeanceRepository.findEnRetard(any())).thenReturn(List.of());
        when(echeanceRepository.findAVenir(any())).thenReturn(List.of(echeance));
        when(configurationService.lireEntier("JOURS_AVANCE_ALERTE_ECHEANCE", 3)).thenReturn(3);

        scheduler.verifierEcheances();

        verify(notificationService, never()).notifierEcheanceProche(any(EcheancePaiement.class), anyInt());
    }
}
