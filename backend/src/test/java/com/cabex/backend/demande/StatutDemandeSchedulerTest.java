package com.cabex.backend.demande;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.demande.service.StatutDemandeScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Clôture quotidienne : ligne TERMINE à sa date de fin, demande TERMINEE
 * quand toutes ses lignes sont terminées (soldes conservés).
 */
@ExtendWith(MockitoExtension.class)
class StatutDemandeSchedulerTest {

    @Mock private LigneServiceRepository ligneRepository;
    @Mock private LigneServiceService ligneService;
    @Mock private DemandeGestionService demandeService;

    @Test
    void ligneEchouePasseTermineeEtDemandeCloturee() {
        LigneService ligne = mock(LigneService.class);
        when(ligne.getFinService()).thenReturn(LocalDate.now().minusDays(1));
        when(ligne.getDemande()).thenReturn(
                com.cabex.backend.demande.entity.DemandeService.builder().id(17).build());
        when(ligneRepository.findByStatutInAndFinServiceBefore(any(), any()))
                .thenReturn(List.of(ligne));
        when(ligneService.avancerStatut(eq(ligne), eq("TERMINE"), any(), eq("AUTO")))
                .thenReturn(true);

        new StatutDemandeScheduler(ligneRepository, ligneService, demandeService)
                .cloturerServicesEtDemandes();

        verify(demandeService).cloturerSiTerminee(17);
    }

    @Test
    void aucuneLigneEchoueAucuneCloture() {
        when(ligneRepository.findByStatutInAndFinServiceBefore(any(), any()))
                .thenReturn(List.of());

        new StatutDemandeScheduler(ligneRepository, ligneService, demandeService)
                .cloturerServicesEtDemandes();

        verifyNoInteractions(ligneService, demandeService);
    }
}
