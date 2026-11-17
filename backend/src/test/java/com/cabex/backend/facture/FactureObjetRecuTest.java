package com.cabex.backend.facture;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.HistoriqueLigneService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeLigneRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.facture.dto.ObjetFactureValideeResponse;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Objet précis d'une facture validée tel qu'il figure sur le reçu de paiement :
 * services facturés, débours désignés, abonnement, ou repli « Facture générale ».
 */
@ExtendWith(MockitoExtension.class)
class FactureObjetRecuTest {

    @Mock private FactureRepository factureRepository;
    @Mock private FactureValideeRepository factureValideeRepository;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueDemandeRepository;
    @Mock private StatutDemandeRepository statutDemandeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private PaiementRepository paiementRepository;
    @Mock private FactureValideeLigneRepository factureValideeLigneRepository;

    private FactureService service;

    private final Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
    private final DemandeService demande = DemandeService.builder().id(7)
            .referenceDemande("DEM-2026-00017").societe(societe).build();

    private LigneService ligne(Integer id, String categorie, String serviceNom, String debours,
                               String typePrestation, Facture facture) {
        Service service = serviceNom == null ? null
                : Service.builder().id(id).nomService(serviceNom).typeService("STANDARD").build();
        return LigneService.builder().id(id).demande(demande)
                .service(service)
                .categorie(categorie)
                .typePrestation(typePrestation)
                .designationDebours(debours)
                .montantDebours(debours != null ? new BigDecimal("50000") : null)
                .tarif(new BigDecimal("100000"))
                .facture(facture)
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new FactureService(factureRepository, factureValideeRepository, ligneRepository,
                historiqueLigneRepository, demandeRepository, historiqueDemandeRepository,
                statutDemandeRepository, utilisateurRepository, auditService,
                notificationService, paiementRepository, factureValideeLigneRepository);
        lenient().when(historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(anyInt()))
                .thenReturn(Optional.of(HistoriqueDemande.builder()
                        .statut(StatutDemande.builder().statut("EN_ATTENTE").build()).build()));
    }

    private Facture proforma() {
        return Facture.builder().id(11).referenceFacture("PRO-17/11/2026-00001")
                .demande(demande).montant(new BigDecimal("2050000")).statut("VALIDEE")
                .build();
    }

    @Test
    void fvEnfantDebours_afficheDesignation() {
        FactureValidee fv = FactureValidee.builder().id(1).referenceFactureValidee("FV-1")
                .ligneService(ligne(3, "DEBOURS", null, "Transport aller-retour", null, null))
                .demande(demande).montant(new BigDecimal("50000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Désignation du débours", objet.libelleObjet());
        assertEquals("Transport aller-retour", objet.objet());
    }

    @Test
    void fvEnfantPrestation_afficheNomService() {
        FactureValidee fv = FactureValidee.builder().id(2).referenceFactureValidee("FV-2")
                .ligneService(ligne(4, "EXTERNALISATION", "Climatisation", null, "PRESTATION", null))
                .demande(demande).montant(new BigDecimal("1200000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Nom du service", objet.libelleObjet());
        assertEquals("Climatisation", objet.objet());
    }

    @Test
    void fvMensuelleAbonnement_afficheNomAbonnement() {
        FactureValidee fv = FactureValidee.builder().id(3).referenceFactureValidee("FV-3")
                .ligneService(ligne(5, "EXTERNALISATION", "Abonnement mensuel", null,
                        "ABONNEMENT_MENSUEL", null))
                .demande(demande).montant(new BigDecimal("600000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Nom du service", objet.libelleObjet());
        assertEquals("Abonnement mensuel", objet.objet());
    }

    @Test
    void fvGeneraleMultiServices_afficheTousLesServices() {
        Facture proforma = proforma();
        LigneService clim = ligne(6, "EXTERNALISATION", "Climatisation", null, "PRESTATION", proforma);
        LigneService peint = ligne(7, "EXTERNALISATION", "Peinture", null, "PRESTATION", proforma);
        LigneService deb = ligne(8, "DEBOURS", null, "Transport aller-retour", null, proforma);

        when(ligneRepository.findByDemandeIdOrderByIdAsc(7)).thenReturn(List.of(clim, peint, deb));

        FactureValidee fv = FactureValidee.builder().id(4).referenceFactureValidee("FV-4")
                .factureProforma(proforma).demande(demande).montant(new BigDecimal("2050000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Services facturés", objet.libelleObjet());
        assertEquals("Climatisation, Peinture, Transport aller-retour", objet.objet());
    }

    @Test
    void fvGeneraleSansLigne_afficheFactureGenerale() {
        when(ligneRepository.findByDemandeIdOrderByIdAsc(7)).thenReturn(List.of());

        FactureValidee fv = FactureValidee.builder().id(5).referenceFactureValidee("FV-5")
                .factureProforma(proforma()).demande(demande).montant(new BigDecimal("1000000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Objet de la facture", objet.libelleObjet());
        assertEquals("Facture générale — DEM-2026-00017", objet.objet());
    }

    @Test
    void fvEnfantDeboursUniquement_afficheDesignerDebours() {
        Facture proforma = proforma();
        LigneService deb = ligne(9, "DEBOURS", null, "Transport", null, proforma);

        when(ligneRepository.findByDemandeIdOrderByIdAsc(7)).thenReturn(List.of(deb));

        FactureValidee fv = FactureValidee.builder().id(6).referenceFactureValidee("FV-6")
                .factureProforma(proforma).demande(demande).montant(new BigDecimal("50000")).build();

        ObjetFactureValideeResponse objet = service.objetFactureValidee(fv);

        assertEquals("Désignation du débours", objet.libelleObjet());
        assertEquals("Transport", objet.objet());
    }
}
