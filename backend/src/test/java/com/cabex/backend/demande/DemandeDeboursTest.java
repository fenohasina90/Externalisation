package com.cabex.backend.demande;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.demande.dto.CreateDemandeRequest;
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.ValiderMontantDeboursRequest;
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
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
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
 * Lignes DEBOURS : création par ligne (désignation, EN_ATTENTE),
 * validation du montant (NON_PAYEE), annulation uniquement depuis EN_ATTENTE.
 * Catégorie par ligne : mélange EXTERNALISATION / DEBOURS possible.
 */
@ExtendWith(MockitoExtension.class)
class DemandeDeboursTest {

    // --- Mocks demande ---
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

    private DemandeGestionService demandeService;

    // --- Mocks ligne ---
    @Mock private com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository historiqueLigneRepo;
    @Mock private com.cabex.backend.demande.repository.LigneServiceRepository ligneRepoLigne;
    @Mock private com.cabex.backend.demande.repository.HistoriqueDemandeRepository historiqueDemandeRepo2;
    @Mock private StatutDemandeRepository statutRepo2;
    @Mock private ServiceRepository serviceRepo2;
    @Mock private ProposPaiementRepository proposRepo2;
    @Mock private FactureService factureService2;
    @Mock private PaiementRepository paiementRepo2;

    private LigneServiceService ligneService;

    @BeforeEach
    void setUp() {
        demandeService = new DemandeGestionService(demandeRepository, societeRepository,
                responsableRepository, commercialRepository, serviceRepository,
                historiqueRepository, statutRepository, paiementRepository,
                utilisateurRepository, notificationService, auditService,
                ligneServiceService, factureService, ligneRepository, proposRepository);

        ligneService = new LigneServiceService(ligneRepoLigne, historiqueLigneRepo,
                demandeRepository, historiqueDemandeRepo2, statutRepo2, serviceRepo2,
                proposRepo2, utilisateurRepository, auditService, factureService2, paiementRepo2);

        lenient().when(statutRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
        lenient().when(demandeRepository.countByAnnee(anyInt())).thenReturn(0L);
        lenient().when(demandeRepository.existsByReferenceDemande(anyString())).thenReturn(false);
        lenient().when(demandeRepository.save(any())).thenAnswer(inv -> {
            var d = (DemandeService) inv.getArgument(0);
            d.setId(1);
            return d;
        });
        lenient().when(paiementRepository.sommePayeeParDemande(anyInt()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(ligneServiceService.montantTotalDemande(anyInt()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(ligneServiceService.listerParDemande(anyInt())).thenReturn(List.of());
        lenient().when(historiqueRepository.findFirstByDemandeIdOrderByIdDesc(anyInt()))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_ATTENTE").build()).build()));
    }

    private CreateDemandeRequest requeteDebours(String designation, Integer societeId) {
        return new CreateDemandeRequest(
                LocalDate.of(2026, 9, 1), null, null,
                societeId, null, null, null, null, null,
                List.of(new CreateLigneRequest(null, null, null, null, "DEBOURS", designation, null)),
                "PAR_SERVICE");
    }

    @Test
    void creationDemandeAvecLigneDeboursOk() {
        when(societeRepository.findById(1)).thenReturn(Optional.of(
                com.cabex.backend.client.entity.Societe.builder().id(1).nomSociete("Alpha").build()));
        when(ligneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = demandeService.create(requeteDebours("Divers + timbres", 1));

        assertNotNull(reponse.referenceDemande());
        verify(ligneRepository).save(argThat(l ->
                "DEBOURS".equals(l.getCategorie()) && "Divers + timbres".equals(l.getDesignationDebours())));
        verify(factureService).genererPourDemande(anyInt(), anyString());
    }

    @Test
    void ligneDeboursDesignationManquante400() {
        when(societeRepository.findById(1)).thenReturn(Optional.of(
                com.cabex.backend.client.entity.Societe.builder().id(1).build()));
        var requete = new CreateDemandeRequest(
                LocalDate.of(2026, 9, 1), null, null,
                1, null, null, null, null, null,
                List.of(new CreateLigneRequest(null, null, null, null, "DEBOURS", " ", null)),
                "PAR_SERVICE");

        var ex = assertThrows(BusinessException.class, () -> demandeService.create(requete));
        assertEquals(ErrorMessages.DESIGNATION_DEBOURS_OBLIGATOIRE, ex.getMessage());
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void ligneDeboursAvecServiceRefuse400() {
        when(societeRepository.findById(1)).thenReturn(Optional.of(
                com.cabex.backend.client.entity.Societe.builder().id(1).build()));
        var requete = new CreateDemandeRequest(
                LocalDate.of(2026, 9, 1), null, null,
                1, null, null, null, null, null,
                List.of(new CreateLigneRequest(1, new BigDecimal("1000"),
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), "DEBOURS", "Divers", null)),
                "PAR_SERVICE");

        var ex = assertThrows(BusinessException.class, () -> demandeService.create(requete));
        assertEquals(ErrorMessages.DEBOURS_SERVICE_INTERDIT, ex.getMessage());
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void categorieInvalide400() {
        when(societeRepository.findById(1)).thenReturn(Optional.of(
                com.cabex.backend.client.entity.Societe.builder().id(1).build()));
        var requete = new CreateDemandeRequest(
                LocalDate.of(2026, 9, 1), null, null,
                1, null, null, null, null, null,
                List.of(new CreateLigneRequest(null, null, null, null, "INCONNUE", null, null)),
                "PAR_SERVICE");
        var ex = assertThrows(BusinessException.class, () -> demandeService.create(requete));
        assertEquals(ErrorMessages.CATEGORIE_DEMANDE_INVALIDE, ex.getMessage());
    }

    private LigneService ligneDebours(Integer id, String statut, BigDecimal montant) {
        DemandeService d = DemandeService.builder().id(17).build();
        LigneService l = LigneService.builder().id(id).demande(d)
                .categorie("DEBOURS").designationDebours("Divers")
                .statut(statut).montantDebours(montant).build();
        lenient().when(ligneRepoLigne.findById(id)).thenReturn(Optional.of(l));
        lenient().when(demandeRepository.findById(17)).thenReturn(Optional.of(d));
        lenient().when(historiqueDemandeRepo2.findFirstByDemandeIdOrderByIdDesc(17))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_ATTENTE").build()).build()));
        lenient().when(statutRepo2.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
        return l;
    }

    @Test
    void validerMontantDeboursOkNonPayee() {
        ligneDebours(5, "EN_ATTENTE", null);
        lenient().when(ligneRepoLigne.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paiementRepo2.sommePayeeParLigne(5)).thenReturn(BigDecimal.ZERO);

        var reponse = ligneService.validerMontantDebours(5,
                new ValiderMontantDeboursRequest(new BigDecimal("500000")));

        assertEquals("NON_PAYEE", reponse.statut());
        assertEquals(new BigDecimal("500000"), reponse.montantDebours());
        verify(historiqueLigneRepo).save(argThat(h -> h.getStatut().equals("NON_PAYEE")));
    }

    @Test
    void validerMontantDeboursDejaValide400() {
        ligneDebours(5, "NON_PAYEE", new BigDecimal("500000"));

        assertThrows(BusinessException.class, () -> ligneService.validerMontantDebours(5,
                new ValiderMontantDeboursRequest(new BigDecimal("500000"))));
    }

    @Test
    void validerMontantDeboursMontantNegatif400() {
        ligneDebours(5, "EN_ATTENTE", null);
        var ex = assertThrows(BusinessException.class, () -> ligneService.validerMontantDebours(5,
                new ValiderMontantDeboursRequest(BigDecimal.ZERO)));
        assertEquals(ErrorMessages.MONTANT_DEBOURS_POSITIF, ex.getMessage());
    }

    @Test
    void validerMontantSurExternalisationRefuse400() {
        DemandeService d = DemandeService.builder().id(17).build();
        LigneService ext = LigneService.builder().id(6).demande(d)
                .categorie("EXTERNALISATION").statut(null).build();
        lenient().when(ligneRepoLigne.findById(6)).thenReturn(Optional.of(ext));

        var ex = assertThrows(BusinessException.class, () -> ligneService.validerMontantDebours(6,
                new ValiderMontantDeboursRequest(new BigDecimal("500000"))));
        assertEquals(ErrorMessages.DEBOURS_UNIQUEMENT, ex.getMessage());
    }

    @Test
    void annulationDeboursDepuisEnAttenteOk() {
        ligneDebours(5, "EN_ATTENTE", null);
        lenient().when(ligneRepoLigne.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = ligneService.annulerDebours(5);

        assertEquals("ANNULEE", reponse.statut());
    }

    @Test
    void annulationDeboursDepuisNonPayeeRefusee() {
        ligneDebours(5, "NON_PAYEE", new BigDecimal("500000"));

        assertThrows(BusinessException.class, () -> ligneService.annulerDebours(5));
    }

    @Test
    void actualiserStatutDeboursPartiel() {
        LigneService l = ligneDebours(5, "NON_PAYEE", new BigDecimal("500000"));
        lenient().when(paiementRepo2.sommePayeeParLigne(5)).thenReturn(new BigDecimal("200000"));
        lenient().when(ligneRepoLigne.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ligneService.actualiserStatutDeboursApresPaiement(l);

        assertEquals("PARTIEL", l.getStatut());
        verify(historiqueLigneRepo).save(argThat(h -> h.getStatut().equals("PARTIEL")));
    }
}
