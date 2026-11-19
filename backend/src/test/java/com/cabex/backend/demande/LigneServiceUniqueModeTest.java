package com.cabex.backend.demande;

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
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureValideeLigneRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Demande multi-services, mode UNIQUE, sans abonnement : chaque ligne passe
 * EN_ATTENTE → EN_COURS (validation) → TERMINEE/PAYEE quand le montant facturé
 * est réglé en totalité.
 */
@ExtendWith(MockitoExtension.class)
class LigneServiceUniqueModeTest {

    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueDemandeRepository;
    @Mock private StatutDemandeRepository statutDemandeRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private com.cabex.backend.facture.service.FactureService factureService;
    @Mock private com.cabex.backend.paiement.repository.PaiementRepository paiementRepository;
    @Mock private FactureValideeRepository factureValideeRepository;
    @Mock private FactureValideeLigneRepository factureValideeLigneRepository;

    private LigneServiceService service;

    private final DemandeService demande = DemandeService.builder().id(17)
            .referenceDemande("DEM-2026-00017").build();
    private final Service svc = Service.builder().id(1)
            .nomService("Audit comptable").typeService("STANDARD").build();
    private final Facture proforma = Facture.builder().id(11)
            .referenceFacture("PRO-18112026-00001").demande(demande)
            .modeFacturation("UNIQUE").statut("VALIDEE")
            .montant(new BigDecimal("300000")).build();
    private LigneService l1;
    private LigneService l2;

    @BeforeEach
    void setUp() {
        service = new LigneServiceService(ligneRepository, historiqueLigneRepository,
                demandeRepository, historiqueDemandeRepository, statutDemandeRepository,
                serviceRepository, utilisateurRepository,
                auditService, factureService, paiementRepository, factureValideeRepository,
                factureValideeLigneRepository);

        l1 = LigneService.builder().id(1).demande(demande).service(svc)
                .categorie("EXTERNALISATION").typePrestation("PRESTATION")
                .tarif(new BigDecimal("200000")).statut("EN_COURS")
                .debutService(LocalDate.of(2026, 10, 1))
                .finService(LocalDate.of(2026, 12, 31))
                .facture(proforma).build();
        l2 = LigneService.builder().id(2).demande(demande)
                .categorie("DEBOURS").typePrestation(null)
                .designationDebours("TIMBRE").montantDebours(new BigDecimal("100000"))
                .statut("EN_COURS").facture(proforma).build();
        proforma.setLignes(new java.util.HashSet<>(java.util.Set.of(l1, l2)));

        FactureValidee fv = FactureValidee.builder().id(3)
                .referenceFactureValidee("00001/EXT/TNR/2026").demande(demande)
                .factureProforma(proforma)
                .montant(new BigDecimal("300000"))
                .valideLe(LocalDateTime.of(2026, 11, 18, 0, 0))
                .build();
        lenient().when(factureValideeRepository.findByFactureProformaId(11))
                .thenReturn(Optional.of(fv));
        lenient().when(paiementRepository.sommePayeeParFacturesValidees(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(factureValideeLigneRepository.sommePayeeViaFacturesCombinees(anyInt()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(ligneRepository.findById(1)).thenReturn(Optional.of(l1));
        lenient().when(ligneRepository.findById(2)).thenReturn(Optional.of(l2));
        lenient().when(ligneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(any()))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_COURS").build()).build()));
        lenient().when(statutDemandeRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
    }

    @Test
    void paiementTotal_marqueToutesLesLignesTerminees() {
        when(paiementRepository.sommePayeeParFactureValidee(3)).thenReturn(new BigDecimal("300000"));

        service.actualiserStatutApresPaiement(l1);
        service.actualiserStatutApresPaiement(l2);

        assertEquals("TERMINE", l1.getStatut(), "prestation payée en totalité");
        assertEquals("PAYEE", l2.getStatut(), "débours payé en totalité");
    }

    @Test
    void paiementPartiel_lignesRestentEnCours() {
        when(paiementRepository.sommePayeeParFactureValidee(3)).thenReturn(new BigDecimal("150000"));

        service.actualiserStatutApresPaiement(l1);
        service.actualiserStatutApresPaiement(l2);

        assertEquals("EN_COURS", l1.getStatut());
        assertEquals("EN_COURS", l2.getStatut());
    }

    @Test
    void paiementRien_lignesRestentEnCours() {
        when(paiementRepository.sommePayeeParFactureValidee(3)).thenReturn(BigDecimal.ZERO);

        service.actualiserStatutApresPaiement(l1);
        service.actualiserStatutApresPaiement(l2);

        assertEquals("EN_COURS", l1.getStatut());
        assertEquals("EN_COURS", l2.getStatut());
    }

    @Test
    void paiementSupprime_lignesRetombentEnCours() {
        // La facture était réglée ; après suppression du paiement, le solde reste dû
        // → les lignes ne doivent pas rester TERMINEE/PAYEE.
        l1.setStatut("TERMINE");
        l2.setStatut("PAYEE");
        when(paiementRepository.sommePayeeParFactureValidee(3)).thenReturn(new BigDecimal("150000"));

        service.actualiserStatutApresPaiement(l1);
        service.actualiserStatutApresPaiement(l2);

        assertEquals("EN_COURS", l1.getStatut());
        assertEquals("EN_COURS", l2.getStatut());
    }
}
