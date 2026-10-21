package com.cabex.backend.echeance;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.echeance.dto.CreatePlanPaiementRequest;
import com.cabex.backend.echeance.dto.EcheancePlanRequest;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.echeance.service.PlanPaiementService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Plans par ligne : facture validée exigée, échéances manuelles
 * (date + montant) ou répartition automatique.
 */
@ExtendWith(MockitoExtension.class)
class PlanPaiementLigneTest {

    @Mock private ProposPaiementRepository proposRepository;
    @Mock private EcheancePaiementRepository echeanceRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private ConfigurationService configurationService;

    private PlanPaiementService service;

    @BeforeEach
    void setUp() {
        service = new PlanPaiementService(proposRepository, echeanceRepository,
                demandeRepository, historiqueRepository, ligneRepository,
                historiqueLigneRepository, utilisateurRepository, auditService,
                configurationService);
        lenient().when(proposRepository.save(any())).thenAnswer(inv -> {
            ProposPaiement p = inv.getArgument(0);
            p.setId(1);
            return p;
        });
    }

    private LigneService ligne(boolean factureValidee) {
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .tarif(new BigDecimal("21000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2027, 7, 19))
                .facture(factureValidee
                        ? com.cabex.backend.facture.entity.Facture.builder()
                                .id(1).statut("VALIDEE").build()
                        : null)
                .build();
        lenient().when(ligneRepository.findById(1)).thenReturn(Optional.of(ligne));
        return ligne;
    }

    private CreatePlanPaiementRequest requeteAuto() {
        return new CreatePlanPaiementRequest(
                LocalDate.of(2026, 10, 18), LocalDate.of(2027, 7, 19),
                new BigDecimal("21000000"), 3, null, null);
    }

    @Test
    void planRefuseSiFactureNonValidee() {
        ligne(false);

        assertThrows(BusinessException.class, () -> service.createForLigne(1, requeteAuto()));
    }

    @Test
    void echeancesManuellesOk() {
        ligne(true);
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(false);
        var requete = new CreatePlanPaiementRequest(
                LocalDate.of(2026, 10, 18), LocalDate.of(2027, 7, 19),
                new BigDecimal("21000000"), 2, null, List.of(
                        new EcheancePlanRequest(LocalDate.of(2026, 11, 18), new BigDecimal("10000000")),
                        new EcheancePlanRequest(LocalDate.of(2027, 1, 18), new BigDecimal("11000000"))));

        var reponse = service.createForLigne(1, requete);

        assertNotNull(reponse);
    }

    @Test
    void echeancesManuellesSommeInexacteRefusee() {
        ligne(true);
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(false);
        var requete = new CreatePlanPaiementRequest(
                LocalDate.of(2026, 10, 18), LocalDate.of(2027, 7, 19),
                new BigDecimal("21000000"), 2, null, List.of(
                        new EcheancePlanRequest(LocalDate.of(2026, 11, 18), new BigDecimal("10000000")),
                        new EcheancePlanRequest(LocalDate.of(2027, 1, 18), new BigDecimal("9000000"))));

        assertThrows(BusinessException.class, () -> service.createForLigne(1, requete));
    }

    @Test
    void echeancesManuellesHorsPeriodeRefusees() {
        ligne(true);
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(false);
        var requete = new CreatePlanPaiementRequest(
                LocalDate.of(2026, 10, 18), LocalDate.of(2027, 7, 19),
                new BigDecimal("21000000"), 1, null, List.of(
                        new EcheancePlanRequest(LocalDate.of(2028, 1, 1), new BigDecimal("21000000"))));

        assertThrows(BusinessException.class, () -> service.createForLigne(1, requete));
    }
}
