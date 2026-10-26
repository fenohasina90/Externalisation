package com.cabex.backend.facture;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Facturation : génération UNIQUE / PAR_SERVICE, actualisation,
 * validation traçée, blocage hors EN_ATTENTE.
 */
@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    @Mock private FactureRepository factureRepository;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueDemandeRepository;
    @Mock private StatutDemandeRepository statutDemandeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;

    private FactureService service;

    @BeforeEach
    void setUp() {
        service = new FactureService(factureRepository, ligneRepository,
                historiqueLigneRepository, demandeRepository, historiqueDemandeRepository,
                statutDemandeRepository, utilisateurRepository, auditService);
        lenient().when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(any()))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_ATTENTE").build()).build()));
        lenient().when(statutDemandeRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
        lenient().when(factureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ligneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(factureRepository.countByReferenceFactureStartingWith(anyString()))
                .thenReturn(0L);
    }

    private DemandeService demande() {
        return DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").actif(true).build();
    }

    private LigneService ligne(Integer id, DemandeService demande, String tarif) {
        return LigneService.builder().id(id).demande(demande)
                .service(Service.builder().id(id).nomService("Service " + id).build())
                .tarif(new BigDecimal(tarif)).build();
    }

    @Test
    void genererUniqueSommeLesTarifs() {
        DemandeService demande = demande();
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        when(factureRepository.findByDemandeId(17)).thenReturn(List.of());
        when(ligneRepository.findByDemandeIdOrderByIdAsc(17)).thenReturn(List.of(
                ligne(1, demande, "2000000"), ligne(2, demande, "7000000"),
                ligne(3, demande, "12000000")));

        var factures = service.genererPourDemande(17, "UNIQUE");

        assertEquals(1, factures.size());
        assertEquals(new BigDecimal("21000000"), factures.get(0).montant());
        assertEquals("PROFORMA", factures.get(0).statut());
    }

    @Test
    void genererParServiceUneFactureParLigne() {
        DemandeService demande = demande();
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        when(factureRepository.findByDemandeId(17)).thenReturn(List.of());
        when(ligneRepository.findByDemandeIdOrderByIdAsc(17)).thenReturn(List.of(
                ligne(1, demande, "2000000"), ligne(2, demande, "7000000")));

        var factures = service.genererPourDemande(17, "PAR_SERVICE");

        assertEquals(2, factures.size());
    }

    @Test
    void genererDeuxFoisRefuse() {
        DemandeService demande = demande();
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        when(factureRepository.findByDemandeId(17)).thenReturn(List.of(
                Facture.builder().id(1).statut("PROFORMA").build()));

        assertThrows(BusinessException.class, () -> service.genererPourDemande(17, "UNIQUE"));
    }

    @Test
    void validerTraceEtPasseLignesValides() {
        DemandeService demande = demande();
        Facture facture = Facture.builder().id(1).demande(demande)
                .referenceFacture("PRO-2026-00001").montant(new BigDecimal("2000000"))
                .statut("PROFORMA").build();
        LigneService ligne = ligne(1, demande, "2000000");
        ligne.setFacture(facture);
        when(factureRepository.findById(1)).thenReturn(Optional.of(facture));
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(ligne));

        var reponse = service.valider(1, null);

        assertEquals("VALIDEE", reponse.statut());
        assertEquals("VALIDE", ligne.getStatut());
        verify(historiqueLigneRepository).save(any());
    }

    @Test
    void validerDeuxFoisRefuse() {
        Facture facture = Facture.builder().id(1).statut("VALIDEE").build();
        when(factureRepository.findById(1)).thenReturn(Optional.of(facture));

        assertThrows(BusinessException.class, () -> service.valider(1, null));
    }
}
