package com.cabex.backend.facture;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.*;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeLigneRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.service.entity.Service;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Facture du mois d'un abonnement : création simple et création combinée
 * (abonnement + prestation / débours impayés de la même société).
 */
@ExtendWith(MockitoExtension.class)
class FactureAbonnementCreationTest {

    @Mock private FactureRepository factureRepository;
    @Mock private FactureValideeRepository factureValideeRepository;
    @Mock private FactureValideeLigneRepository factureValideeLigneRepository;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueDemandeRepository;
    @Mock private StatutDemandeRepository statutDemandeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private com.cabex.backend.paiement.repository.PaiementRepository paiementRepository;

    private FactureService service;

    private final Societe societe = Societe.builder().id(5).nomSociete("Atlas Conseil").build();

    @BeforeEach
    void setUp() {
        service = new FactureService(factureRepository, factureValideeRepository, ligneRepository,
                historiqueLigneRepository, demandeRepository, historiqueDemandeRepository,
                statutDemandeRepository, utilisateurRepository, auditService,
                notificationService, paiementRepository, factureValideeLigneRepository);
        lenient().when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(any()))
                .thenAnswer(inv -> Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_COURS").build()).build()));
        lenient().when(statutDemandeRepository.findByStatut(anyString()))
                .thenAnswer(inv -> Optional.of(StatutDemande.builder()
                        .statut(inv.getArgument(0)).build()));
        lenient().when(factureValideeRepository.countByReferenceFactureValideeEndingWith(anyString()))
                .thenReturn(0L);
        lenient().when(factureValideeRepository.save(any())).thenAnswer(inv -> {
            var fv = (FactureValidee) inv.getArgument(0);
            if (fv.getId() == null) {
                fv.setId(99);
            }
            return fv;
        });
        lenient().when(paiementRepository.sommePayeeParFactureValidee(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(paiementRepository.sommePayeeParFacturesValidees(any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(factureValideeLigneRepository.sommePayeeViaFacturesCombinees(any()))
                .thenReturn(BigDecimal.ZERO);
    }

    private DemandeService demande(Integer id) {
        return DemandeService.builder().id(id)
                .referenceDemande("DEM-2026-0000" + id)
                .societe(societe)
                .build();
    }

    private LigneService abonnement(Integer idDemande, Integer idLigne, BigDecimal tarif) {
        DemandeService d = demande(idDemande);
        Facture proforma = Facture.builder().id(1)
                .referenceFacture("PRO-01112026-00001")
                .demande(d)
                .montant(tarif)
                .statut("VALIDEE")
                .build();
        LigneService ligne = LigneService.builder().id(idLigne).demande(d)
                .categorie("EXTERNALISATION")
                .typePrestation("ABONNEMENT_MENSUEL")
                .service(Service.builder().id(1).nomService("Comptabilité mensuelle").build())
                .tarif(tarif)
                .statut("EN_COURS")
                .facture(proforma)
                .build();
        proforma.getLignes().add(ligne);
        return ligne;
    }

    private void stubAbonnement(LigneService ligne) {
        DemandeService d = ligne.getDemande();
        lenient().when(demandeRepository.findById(d.getId())).thenReturn(Optional.of(d));
        lenient().when(ligneRepository.findById(ligne.getId())).thenReturn(Optional.of(ligne));
        Facture proforma = ligne.getFacture();
        FactureValidee generale = FactureValidee.builder().id(1)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .factureProforma(proforma)
                .demande(d)
                .montant(ligne.getTarif())
                .build();
        lenient().when(factureValideeRepository.findByFactureProformaId(proforma.getId()))
                .thenReturn(Optional.of(generale));
        lenient().when(factureValideeRepository.findByDemandeIdOrderByIdAsc(anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void creationCombinationAbonnementPlusDebours() {
        LigneService abonnement = abonnement(7, 1, new BigDecimal("1500000"));
        stubAbonnement(abonnement);

        LigneService debours = LigneService.builder().id(2).demande(demande(8))
                .categorie("DEBOURS")
                .statut("NON_PAYEE")
                .montantDebours(new BigDecimal("250000"))
                .designationDebours("Timbres postaux")
                .build();
        lenient().when(demandeRepository.findById(117)).thenReturn(Optional.of(demande(117)));
        lenient().when(ligneRepository.findById(2)).thenReturn(Optional.of(debours));

        com.cabex.backend.facture.dto.FactureValideeResponse reponse = service.creerFactureMensuelleAbonnement(
                7, 1, LocalDate.of(2026, 11, 10), "2026-11", "Novembre", List.of(2));

        assertEquals(new BigDecimal("1750000"), reponse.montant());
        assertEquals(2, reponse.lignes().size());
        assertTrue(reponse.lignes().stream()
                .anyMatch(l -> l.designation().startsWith("Abonnement mensuel")
                        && l.montant().compareTo(new BigDecimal("1500000")) == 0));
        assertTrue(reponse.lignes().stream()
                .anyMatch(l -> l.designation().startsWith("Débours")
                        && l.montant().compareTo(new BigDecimal("250000")) == 0));
        assertEquals("ABONNEMENT_MENSUEL", reponse.type());
    }

    @Test
    void creationFactureMemeMoisRefusee() {
        LigneService abonnement = abonnement(7, 1, new BigDecimal("1500000"));
        stubAbonnement(abonnement);
        lenient().when(factureValideeRepository.findByLigneServiceIdAndMois(anyInt(), any()))
                .thenReturn(com.cabex.backend.facture.entity.FactureValidee.builder()
                        .id(77)
                        .referenceFactureValidee("00077/EXT/TNR/2026")
                        .demande(abonnement.getDemande())
                        .montant(new BigDecimal("1500000"))
                        .build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.creerFactureMensuelleAbonnement(
                        7, 1, LocalDate.of(2026, 11, 10), "2026-11", null, List.of()));
        assertTrue(ex.getMessage().toLowerCase(java.util.Locale.FRANCE).contains("novembre 2026"));
    }

    @Test
    void creationUtiliseMontantFactureValideePasTarif() {
        LigneService abonnement = abonnement(7, 1, new BigDecimal("1500000"));
        stubAbonnement(abonnement);
        // La facture générale est validée à 1 200 000 (différent du tarif).
        lenient().when(factureValideeRepository.findByFactureProformaId(anyInt()))
                .thenReturn(Optional.of(com.cabex.backend.facture.entity.FactureValidee.builder()
                        .id(1)
                        .referenceFactureValidee("00001/EXT/TNR/2026")
                        .demande(abonnement.getDemande())
                        .montant(new BigDecimal("1200000"))
                        .build()));

        LigneService debours = LigneService.builder().id(2).demande(demande(8))
                .categorie("DEBOURS")
                .statut("NON_PAYEE")
                .montantDebours(new BigDecimal("250000"))
                .designationDebours("Timbres postaux")
                .build();
        lenient().when(ligneRepository.findById(2)).thenReturn(Optional.of(debours));

        com.cabex.backend.facture.dto.FactureValideeResponse reponse = service.creerFactureMensuelleAbonnement(
                7, 1, LocalDate.of(2026, 11, 10), "2026-11", null, List.of(2));

        assertEquals(new BigDecimal("1450000"), reponse.montant());
        assertTrue(reponse.lignes().stream()
                .anyMatch(l -> l.designation().startsWith("Abonnement mensuel")
                        && l.montant().compareTo(new BigDecimal("1200000")) == 0),
                "la ligne d'abonnement reprend le montant validé, pas le tarif");
    }

    @Test
    void creationInclusionAutreSocieteRefusee() {
        LigneService abonnement = abonnement(7, 1, new BigDecimal("1500000"));
        stubAbonnement(abonnement);

        DemandeService autreSociete = demande(118);
        autreSociete.setSociete(Societe.builder().id(77).nomSociete("Autre client").build());
        LigneService debours = LigneService.builder().id(3).demande(autreSociete)
                .categorie("DEBOURS")
                .statut("NON_PAYEE")
                .montantDebours(new BigDecimal("50000"))
                .designationDebours("Frais bancaires")
                .build();
        lenient().when(ligneRepository.findById(3)).thenReturn(Optional.of(debours));

        assertThrows(BusinessException.class, () -> service.creerFactureMensuelleAbonnement(
                7, 1, LocalDate.of(2026, 11, 10), "2026-11", null, List.of(3)));
    }
}
