package com.cabex.backend.demande;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.UpdateLigneRequest;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
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
 * Lignes de service : ajout/modification/retrait en EN_ATTENTE,
 * interdiction du mix CREATION, minimum un service.
 */
@ExtendWith(MockitoExtension.class)
class LigneServiceTest {

    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueDemandeRepository;
    @Mock private StatutDemandeRepository statutDemandeRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ProposPaiementRepository proposRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private FactureService factureService;

    private LigneServiceService service;

    @BeforeEach
    void setUp() {
        service = new LigneServiceService(ligneRepository, historiqueLigneRepository,
                demandeRepository, historiqueDemandeRepository, statutDemandeRepository,
                serviceRepository, proposRepository, utilisateurRepository,
                auditService, factureService);
        lenient().when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(any()))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_ATTENTE").build()).build()));
        lenient().when(statutDemandeRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
        lenient().when(ligneRepository.save(any())).thenAnswer(inv -> {
            LigneService l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(1);
            }
            return l;
        });
    }

    private DemandeService demande(Integer id) {
        return DemandeService.builder().id(id)
                .referenceDemande("DEM-2026-00017")
                .societe(com.cabex.backend.client.entity.Societe.builder()
                        .id(1).nomSociete("Atlas Conseil").build())
                .actif(true).build();
    }

    private Service serviceCatalogue(Integer id, String type) {
        return Service.builder().id(id).nomService("Service " + id)
                .typeService(type).actif(true).build();
    }

    private CreateLigneRequest requete(Integer serviceId, String tarif) {
        return new CreateLigneRequest(serviceId, new BigDecimal(tarif),
                LocalDate.of(2026, 10, 18), LocalDate.of(2026, 12, 16));
    }

    @Test
    void ajoutOkEnAttente() {
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande(17)));
        when(serviceRepository.findById(1)).thenReturn(Optional.of(serviceCatalogue(1, "STANDARD")));

        var reponse = service.ajouter(17, requete(1, "2000000"));

        assertNotNull(reponse);
        verify(factureService).actualiserPourDemande(17);
        verify(historiqueLigneRepository).save(any());
    }

    @Test
    void ajoutRefuseHorsAttente() {
        DemandeService demande = demande(17);
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(17))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("VALIDATION").build()).build()));

        assertThrows(BusinessException.class, () -> service.ajouter(17, requete(1, "2000000")));
    }

    @Test
    void mixCreationInterdit() {
        DemandeService demande = demande(17);
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        Service creation = serviceCatalogue(9, "CREATION");
        LigneService existante = LigneService.builder().id(1).demande(demande)
                .service(creation).tarif(new BigDecimal("1000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(existante));
        when(serviceRepository.findById(2)).thenReturn(Optional.of(serviceCatalogue(2, "STANDARD")));

        assertThrows(BusinessException.class, () -> service.ajouter(17, requete(2, "7000000")));
    }

    @Test
    void mixTransactionImmobiliereInterdit() {
        DemandeService demande = demande(17);
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        Service transaction = serviceCatalogue(9, "TRANSACTION_IMMOBILIERE");
        LigneService existante = LigneService.builder().id(1).demande(demande)
                .service(transaction).tarif(new BigDecimal("1000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(existante));
        when(serviceRepository.findById(2)).thenReturn(Optional.of(serviceCatalogue(2, "STANDARD")));

        var ex = assertThrows(BusinessException.class,
                () -> service.ajouter(17, requete(2, "7000000")));
        assertEquals(com.cabex.backend.exception.ErrorMessages
                .SERVICE_SOLO_CREATION_TRANSACTION, ex.getMessage());
    }

    @Test
    void ajoutTransactionSurDemandeExistanteInterdit() {
        DemandeService demande = demande(17);
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        Service standard = serviceCatalogue(2, "STANDARD");
        LigneService existante = LigneService.builder().id(1).demande(demande)
                .service(standard).tarif(new BigDecimal("1000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(existante));
        when(serviceRepository.findById(9))
                .thenReturn(Optional.of(serviceCatalogue(9, "TRANSACTION_IMMOBILIERE")));

        var ex = assertThrows(BusinessException.class,
                () -> service.ajouter(17, requete(9, "7000000")));
        assertEquals(com.cabex.backend.exception.ErrorMessages
                .SERVICE_SOLO_CREATION_TRANSACTION, ex.getMessage());
    }

    @Test
    void retraitDernierServiceInterdit() {
        DemandeService demande = demande(17);
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(serviceCatalogue(1, "STANDARD")).build();
        when(ligneRepository.findById(1)).thenReturn(Optional.of(ligne));
        lenient().when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        when(ligneRepository.countByDemandeId(17)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.retirer(1));
    }

    @Test
    void modificationRecalculeFacturation() {
        DemandeService demande = demande(17);
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(serviceCatalogue(1, "STANDARD"))
                .tarif(new BigDecimal("2000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16)).build();
        when(ligneRepository.findById(1)).thenReturn(Optional.of(ligne));

        var reponse = service.modifier(1, new UpdateLigneRequest(
                new BigDecimal("2500000"), null, null));

        assertEquals(new BigDecimal("2500000"), reponse.tarif());
        verify(factureService).actualiserPourDemande(17);
    }

    @Test
    void ligneInexistante404() {
        when(ligneRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.modifier(999, new UpdateLigneRequest(null, null, null)));
    }

    @Test
    void doublonServiceRefuse() {
        DemandeService demande = demande(17);
        when(demandeRepository.findById(17)).thenReturn(Optional.of(demande));
        Service existant = serviceCatalogue(1, "STANDARD");
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(existant).tarif(new BigDecimal("2000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(ligne));
        when(serviceRepository.findById(1)).thenReturn(Optional.of(existant));

        assertThrows(BusinessException.class, () -> service.ajouter(17, requete(1, "3000000")));
    }

    @Test
    void modificationLigneFigeeSiFactureValidee() {
        DemandeService demande = demande(17);
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(serviceCatalogue(1, "STANDARD"))
                .tarif(new BigDecimal("2000000"))
                .facture(com.cabex.backend.facture.entity.Facture.builder()
                        .id(1).statut("VALIDEE").build())
                .build();
        when(ligneRepository.findById(1)).thenReturn(Optional.of(ligne));

        assertThrows(BusinessException.class, () -> service.modifier(1,
                new UpdateLigneRequest(new BigDecimal("2500000"), null, null)));
        assertThrows(BusinessException.class, () -> service.retirer(1));
    }
}
