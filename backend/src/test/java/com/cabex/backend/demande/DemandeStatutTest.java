package com.cabex.backend.demande;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.demande.dto.ChangeStatutRequest;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.repository.PaiementRepository;
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
 * Matrice des statuts de demande (annulation limitée à EN_ATTENTE),
 * garde du passage à VALIDATION et clôture automatique.
 */
@ExtendWith(MockitoExtension.class)
class DemandeStatutTest {

    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private SocieteRepository societeRepository;
    @Mock private ClientResponsableRepository responsableRepository;
    @Mock private CommercialRepository commercialRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private StatutDemandeRepository statutRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private LigneServiceService ligneServiceService;
    @Mock private FactureService factureService;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private ProposPaiementRepository proposRepository;

    private DemandeGestionService service;

    @BeforeEach
    void setUp() {
        service = new DemandeGestionService(demandeRepository, societeRepository,
                responsableRepository, commercialRepository, serviceRepository,
                historiqueRepository, statutRepository, paiementRepository,
                utilisateurRepository, notificationService, auditService,
                ligneServiceService, factureService, ligneRepository, proposRepository);
    }

    private void statutDemande(Integer id, String statut) {
        DemandeService demande = DemandeService.builder().id(id)
                .referenceDemande("DEM-2026-00017").actif(true).build();
        lenient().when(demandeRepository.findById(id)).thenReturn(Optional.of(demande));
        lenient().when(historiqueRepository.findFirstByDemandeIdOrderByIdDesc(id))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut(statut).build()).build()));
        lenient().when(statutRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
    }

    @Test
    void annulationDepuisValidationRefusee() {
        statutDemande(17, "VALIDATION");
        assertThrows(BusinessException.class,
                () -> service.changeStatut(17, new ChangeStatutRequest("ANNULEE", null)));
    }

    @Test
    void annulationDepuisEnCoursRefusee() {
        statutDemande(17, "EN_COURS");
        assertThrows(BusinessException.class,
                () -> service.changeStatut(17, new ChangeStatutRequest("ANNULEE", null)));
    }

    @Test
    void annulationDepuisEnAttenteOk() {
        statutDemande(17, "EN_ATTENTE");
        lenient().when(paiementRepository.sommePayeeParDemande(17)).thenReturn(BigDecimal.ZERO);
        lenient().when(ligneServiceService.montantTotalDemande(17)).thenReturn(BigDecimal.ZERO);
        lenient().when(ligneServiceService.listerParDemande(17)).thenReturn(List.of());

        var reponse = service.changeStatut(17, new ChangeStatutRequest("ANNULEE", null));

        assertNotNull(reponse);
    }

    @Test
    void validationRefuseeSiFactureNonValidee() {
        statutDemande(17, "EN_ATTENTE");
        DemandeService demande = demandeRepository.findById(17).orElseThrow();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .tarif(new BigDecimal("2000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(ligne));
        when(factureService.toutesValidees(17)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.changeStatut(17, new ChangeStatutRequest("VALIDATION", null)));
    }

    @Test
    void validationRefuseeSiPlanManquant() {
        statutDemande(17, "EN_ATTENTE");
        LigneService ligne = LigneService.builder().id(1)
                .demande(DemandeService.builder().id(17).build())
                .tarif(new BigDecimal("2000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(ligne));
        when(factureService.toutesValidees(17)).thenReturn(true);
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.changeStatut(17, new ChangeStatutRequest("VALIDATION", null)));
    }

    @Test
    void validationOkSiFacturesValideesEtPlansEtabli() {
        statutDemande(17, "EN_ATTENTE");
        LigneService ligne = LigneService.builder().id(1)
                .demande(DemandeService.builder().id(17).build())
                .tarif(new BigDecimal("2000000")).build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(ligne));
        when(factureService.toutesValidees(17)).thenReturn(true);
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(true);
        lenient().when(paiementRepository.sommePayeeParDemande(17)).thenReturn(BigDecimal.ZERO);
        lenient().when(ligneServiceService.montantTotalDemande(17))
                .thenReturn(new BigDecimal("2000000"));
        lenient().when(ligneServiceService.listerParDemande(17)).thenReturn(List.of());

        var reponse = service.changeStatut(17, new ChangeStatutRequest("VALIDATION", null));

        assertNotNull(reponse);
    }

    @Test
    void clotureAutoSiToutesLignesTerminees() {
        statutDemande(17, "EN_COURS");
        LigneService terminee = LigneService.builder().id(1).statut("TERMINE").build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(terminee));

        assertTrue(service.cloturerSiTerminee(17));
    }

    @Test
    void pasDeClotureSiLigneEnCours() {
        statutDemande(17, "EN_COURS");
        LigneService enCours = LigneService.builder().id(1).statut("EN_COURS").build();
        when(ligneRepository.findByDemandeId(17)).thenReturn(List.of(enCours));

        assertFalse(service.cloturerSiTerminee(17));
    }
}
