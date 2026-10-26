package com.cabex.backend.echeance;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.dto.EcheanceCibleResponse;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.echeance.service.EcheanceGestionService;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcheanceGestionServiceTest {

    @Mock private EcheancePaiementRepository echeanceRepository;
    @Mock private ProposPaiementRepository proposRepository;
    @Mock private AuditService auditService;
    @Mock private ConfigurationService configurationService;

    @InjectMocks private EcheanceGestionService echeanceService;

    private EcheancePaiement echeance() {
        DemandeService demande = DemandeService.builder()
                .id(7)
                .referenceDemande("DEM-2026-00017")
                .build();
        ProposPaiement propos = ProposPaiement.builder()
                .id(3)
                .demande(demande)
                .build();
        return EcheancePaiement.builder()
                .id(42)
                .numeroEcheance(2)
                .proposPaiement(propos)
                .build();
    }

    @Test
    void getCibleRetourneLaDemandeDeLEcheance() {
        when(echeanceRepository.findById(42)).thenReturn(Optional.of(echeance()));

        EcheanceCibleResponse cible = echeanceService.getCible(42);

        assertEquals(42, cible.id());
        assertEquals(2, cible.numeroEcheance());
        assertEquals(7, cible.demandeId());
        assertEquals("DEM-2026-00017", cible.referenceDemande());
    }

    @Test
    void getCibleEcheanceInexistanteLeve404() {
        when(echeanceRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> echeanceService.getCible(999));
    }
}
