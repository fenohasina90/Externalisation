package com.cabex.backend.facture;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.*;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.facture.dto.FactureValideeFicheResponse;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeLigneRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Onglet « Facturation » de la fiche client : toutes les factures validées du
 * client, groupées en facture générale (proforma validée) et factures de
 * paiement enfants, paiements inclus.
 */
@ExtendWith(MockitoExtension.class)
class FactureFacturationClientTest {

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
        lenient().when(paiementRepository.sommePayeeParFactureValidee(anyInt()))
                .thenReturn(BigDecimal.ZERO);
    }

    private FactureValideeFicheResponse fichePaiementAvecPaiement(String ref, BigDecimal montant,
                                                                  BigDecimal paye) {
        return service.listerFacturationParClient(societe.getId()).stream()
                .filter(f -> ref.equals(f.referenceFactureValidee()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Facture " + ref + " introuvable"));
    }

    @Test
    void listerFacturationParClientGroupeEtMontePaiements() {
        DemandeService demande = DemandeService.builder().id(11)
                .referenceDemande("DEM-2026-00011").societe(societe).build();
        Facture proforma = Facture.builder().id(1)
                .referenceFacture("PRO-01112026-00001").demande(demande)
                .montant(new BigDecimal("165000")).statut("VALIDEE").build();
        LigneService lignePrestation = LigneService.builder().id(21).demande(demande)
                .categorie("EXTERNALISATION").typePrestation("PRESTATION")
                .service(Service.builder().id(31).nomService("Déclaration sociale").build())
                .tarif(new BigDecimal("75000")).facture(proforma).build();
        LigneService ligneAbonnement = LigneService.builder().id(22).demande(demande)
                .categorie("EXTERNALISATION").typePrestation(LigneServiceService.ABONNEMENT_MENSUEL)
                .service(Service.builder().id(32).nomService("Comptabilité mensuelle").build())
                .tarif(new BigDecimal("95000")).facture(proforma).build();

        Utilisateur agent = Utilisateur.builder().id(7).prenom("Système").nom("Administrateur").build();
        FactureValidee generale = FactureValidee.builder().id(101)
                .referenceFactureValidee("00001EXT/TNR/2026")
                .factureProforma(proforma).demande(demande)
                .montant(new BigDecimal("165000"))
                .valideLe(LocalDateTime.of(2026, 11, 13, 10, 0))
                .validePar(agent)
                .build();
        FactureValidee enfantPresta = FactureValidee.builder().id(102)
                .referenceFactureValidee("00002EXT/TNR/2026")
                .factureProforma(null).factureGenerale(generale)
                .ligneService(lignePrestation).demande(demande)
                .montant(new BigDecimal("75000"))
                .valideLe(LocalDateTime.of(2026, 11, 13, 10, 1))
                .validePar(agent).build();
        FactureValidee mensuelle = FactureValidee.builder().id(103)
                .referenceFactureValidee("00003EXT/TNR/2026")
                .factureProforma(null).factureGenerale(generale)
                .ligneService(ligneAbonnement).demande(demande)
                .montant(new BigDecimal("95000"))
                .mois(LocalDate.of(2026, 11, 1))
                .valideLe(LocalDateTime.of(2026, 11, 13, 10, 2))
                .validePar(agent).build();

        ModePaiement virement = ModePaiement.builder().id(1).code("VIREMENT").libelle("Virement bancaire").build();
        Paiement paiementPresta = Paiement.builder().id(201)
                .referencePaiement("PAY-2026-00011")
                .demande(demande).factureValidee(enfantPresta)
                .modePaiement(virement).datePaiement(LocalDate.of(2026, 11, 13))
                .montantPaye(new BigDecimal("25000")).referenceVirement("VRC-01")
                .createdBy(agent).build();
        Paiement paiementMensuel = Paiement.builder().id(202)
                .referencePaiement("PAY-2026-00012")
                .demande(demande).factureValidee(mensuelle)
                .modePaiement(ModePaiement.builder().id(2).code("ESPECES").libelle("Espèces").build())
                .datePaiement(LocalDate.of(2026, 11, 13))
                .montantPaye(new BigDecimal("30000"))
                .createdBy(agent).build();

        when(factureValideeRepository.findBySocieteId(societe.getId()))
                .thenReturn(List.of(generale, enfantPresta, mensuelle));
        when(paiementRepository.findBySocieteIdForFacturation(societe.getId()))
                .thenReturn(List.of(paiementPresta, paiementMensuel));

        List<FactureValideeFicheResponse> fiches = service.listerFacturationParClient(societe.getId());

        assertEquals(3, fiches.size());

        FactureValideeFicheResponse g = fiches.get(0);
        assertEquals("GENERALE", g.type());
        assertEquals("Facture générale — DEM-2026-00011", g.objet());
        assertEquals(new BigDecimal("165000"), g.montant());

        FactureValideeFicheResponse presta = fichePaiementAvecPaiement("00002EXT/TNR/2026",
                new BigDecimal("75000"), new BigDecimal("25000"));
        assertEquals("PRESTATION", presta.type());
        assertEquals(101, presta.factureGeneraleId());
        assertEquals("00001EXT/TNR/2026", presta.referenceFactureGenerale());
        assertEquals("Déclaration sociale", presta.objet());
        assertEquals(new BigDecimal("25000"), presta.montantPaye());
        assertEquals(new BigDecimal("50000"), presta.resteAPayer());
        assertEquals(1, presta.paiements().size());
        assertEquals("VRC-01", presta.paiements().get(0).referenceVirement());
        assertEquals("Système Administrateur", presta.paiements().get(0).createdByName());

        FactureValideeFicheResponse mois = fichePaiementAvecPaiement("00003EXT/TNR/2026",
                new BigDecimal("95000"), new BigDecimal("30000"));
        assertEquals("ABONNEMENT_MENSUEL", mois.type());
        assertEquals("Comptabilité mensuelle", mois.objet());
        assertEquals("2026-11", mois.mois());
        assertEquals(new BigDecimal("30000"), mois.montantPaye());
        assertEquals(new BigDecimal("65000"), mois.resteAPayer());
        assertEquals("PAY-2026-00012", mois.paiements().get(0).referencePaiement());
    }

    @Test
    void aucunFacturesRetourneListVide() {
        when(factureValideeRepository.findBySocieteId(societe.getId())).thenReturn(List.of());
        assertTrue(service.listerFacturationParClient(societe.getId()).isEmpty());
    }
}
