package com.cabex.backend.demande;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.SituationPaiement;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filtre `situation` de la liste des demandes : il doit porter sur l'ensemble
 * du jeu de résultats, pas sur la seule page affichée. Les prédicats sont
 * mutuellement exclusifs et doivent rester cohérents avec l'ordre de
 * précédence de la situation d'un plan (PAYEE &gt; EN_RETARD &gt; PARTIEL &gt;
 * EN_ATTENTE).
 */
@DataJpaTest
class DemandeSituationTest {

    @Autowired
    private DemandeServiceRepository demandeRepository;
    @Autowired
    private LigneServiceRepository ligneRepository;
    @Autowired
    private PaiementRepository paiementRepository;
    @Autowired
    private ModePaiementRepository modeRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private FactureValideeRepository factureValideeRepository;
    @Autowired
    private HistoriqueDemandeRepository historiqueRepository;
    @Autowired
    private StatutDemandeRepository statutDemandeRepository;

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime DEADLINE_PD = AUJOURDHUI.minusDays(5).atTime(LocalTime.MAX);
    private static final LocalDateTime DEADLINE_ABO = AUJOURDHUI.minusDays(7).atTime(LocalTime.MAX);
    private int compteur;

    private void marquerTerminee(DemandeService d) {
        StatutDemande statut = statutDemandeRepository.findByStatut("TERMINEE")
                .orElseGet(() -> statutDemandeRepository.save(
                        StatutDemande.builder().statut("TERMINEE").build()));
        historiqueRepository.save(HistoriqueDemande.builder()
                .demande(d).statut(statut).build());
    }

    private DemandeService demande(String reference) {
        return demandeRepository.save(DemandeService.builder()
                .referenceDemande(reference)
                .dateDemande(AUJOURDHUI)
                .societe(null)
                .actif(true)
                .build());
    }

    /**
     * Crée une demande avec une facture validée (et paiement selon le montant versé).
     *
     * @param valideLe       date de validation/émission de la facture
     * @param montant        montant de la facture
     * @param paye           montant déjà payé (0 si aucune pièce)
     */
    private DemandeService avecFactureValidee(String reference, LocalDate valideLe,
                                              String montant, String paye) {
        DemandeService d = demande(reference);
        String code = "SRV-" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal(montant))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut("EN_COURS")
                .categorie("EXTERNALISATION")
                .build());
        FactureValidee fv = factureValideeRepository.save(FactureValidee.builder()
                .referenceFactureValidee("0000" + reference.substring(10) + "EXT/TNR/2026")
                .demande(d)
                .montant(new BigDecimal(montant))
                .valideLe(valideLe.atTime(LocalTime.of(0, 0)))
                .build());
        BigDecimal payeBig = new BigDecimal(paye);
        if (payeBig.signum() > 0) {
            ModePaiement virement = modeRepository.findByCode("VIREMENT")
                    .orElseGet(() -> modeRepository.save(ModePaiement.builder()
                            .code("VIREMENT").libelle("Virement").actif(true).build()));
            paiementRepository.save(Paiement.builder()
                    .referencePaiement("PAY-" + reference)
                    .demande(d)
                    .factureValidee(fv)
                    .modePaiement(virement)
                    .datePaiement(AUJOURDHUI)
                    .montantPaye(payeBig)
                    .actif(true)
                    .build());
        }
        return d;
    }

    private void jeuDeDonnees() {
        // Soldée.
        marquerTerminee(avecFactureValidee("DEM-2026-00001", AUJOURDHUI.minusDays(1), "1000000", "1000000"));
        // Émission ancienne, reste dû → EN_RETARD.
        avecFactureValidee("DEM-2026-00002", AUJOURDHUI.minusDays(8), "1000000", "200000");
        // Émission récente, payée partiellement → PARTIEL.
        avecFactureValidee("DEM-2026-00003", AUJOURDHUI, "1000000", "400000");
        // Émission récente, rien payé → EN_ATTENTE.
        avecFactureValidee("DEM-2026-00004", AUJOURDHUI, "1000000", "0");
        // Aucune facture validée → NON_PLANIFIE.
        demande("DEM-2026-00005");
    }

    private DemandeService ligneMixte(String reference, String statut) {
        DemandeService d = demande(reference);
        String code = "SRV-" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal("1000000"))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut(statut)
                .categorie("EXTERNALISATION")
                .build());
        return d;
    }

    private long count(String situation) {
        return demandeRepository
                .rechercher(null, null, null, null, null, null, null, null,
                        situation, null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged())
                .getTotalElements();
    }

    @Test
    void aucuneSituationNeRetientToutLeMonde() {
        jeuDeDonnees();
        assertEquals(5, count(null));
    }

    @Test
    void situationPaye() {
        jeuDeDonnees();
        assertEquals(1, count("PAYE"));
    }

    @Test
    void situationEnRetard() {
        jeuDeDonnees();
        assertEquals(1, count("EN_RETARD"));
    }

    @Test
    void situationPartiel() {
        jeuDeDonnees();
        assertEquals(1, count("PARTIEL"));
    }

    @Test
    void situationEnAttente() {
        jeuDeDonnees();
        assertEquals(1, count("EN_ATTENTE"));
    }

    @Test
    void situationEnRetardFactureNonSoldeeApresDelaisPrestation() {
        DemandeService d = demande("DEM-2026-06001");
        String code = "SRV-" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal("500000"))
                .debutService(AUJOURDHUI.minusDays(10))
                .finService(AUJOURDHUI.plusDays(30))
                .statut("EN_COURS")
                .categorie("EXTERNALISATION")
                .build());
        factureValideeRepository.save(FactureValidee.builder()
                .referenceFactureValidee("00007EXT/TNR/2026")
                .demande(d)
                .montant(new BigDecimal("500000"))
                .valideLe(AUJOURDHUI.minusDays(8).atTime(LocalTime.of(0, 0)))
                .build());

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, "EN_RETARD", null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());
        assertEquals(1, filtrees.getTotalElements());
        assertEquals("DEM-2026-06001", filtrees.getContent().get(0).getReferenceDemande());
    }

    @Test
    void situationEnRetardMensualiteNonSoldeeApresDelaisAbonnement() {
        DemandeService d = demande("DEM-2026-06002");
        String code = "SRV-" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal("450000"))
                .debutService(AUJOURDHUI.minusDays(10))
                .finService(AUJOURDHUI.plusDays(180))
                .statut("EN_COURS")
                .categorie("EXTERNALISATION")
                .typePrestation("ABONNEMENT_MENSUEL")
                .build());
        factureValideeRepository.save(FactureValidee.builder()
                .referenceFactureValidee("00008EXT/TNR/2026")
                .demande(d)
                .montant(new BigDecimal("450000"))
                .valideLe(AUJOURDHUI.minusDays(8).atTime(LocalTime.of(0, 0)))
                .mois(AUJOURDHUI.minusDays(30).withDayOfMonth(1))
                .build());

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, "EN_RETARD", null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());
        assertEquals(1, filtrees.getTotalElements());
        assertEquals("DEM-2026-06002", filtrees.getContent().get(0).getReferenceDemande());
    }

    @Test
    void situationPasEnRetardMensualiteAvantDelaisAbonnement() {
        DemandeService d = demande("DEM-2026-06003");
        String code = "SRV-" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal("450000"))
                .debutService(AUJOURDHUI.minusDays(10))
                .finService(AUJOURDHUI.plusDays(180))
                .statut("EN_COURS")
                .categorie("EXTERNALISATION")
                .typePrestation("ABONNEMENT_MENSUEL")
                .build());
        factureValideeRepository.save(FactureValidee.builder()
                .referenceFactureValidee("00009EXT/TNR/2026")
                .demande(d)
                .montant(new BigDecimal("450000"))
                .valideLe(AUJOURDHUI.minusDays(3).atTime(LocalTime.of(0, 0)))
                .mois(AUJOURDHUI.minusDays(30).withDayOfMonth(1))
                .build());

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, "EN_RETARD", null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());
        assertEquals(0, filtrees.getTotalElements());
    }

    @Test
    void situationNonPlanifie() {
        jeuDeDonnees();
        assertEquals(1, count("NON_PLANIFIE"));
    }

    /** Les situations sont exclusives : leur somme vaut le total. */
    @Test
    void lesSituationsSontMutuellementExclusives() {
        jeuDeDonnees();
        long somme = count("PAYE") + count("EN_RETARD") + count("PARTIEL")
                + count("EN_ATTENTE") + count("NON_PLANIFIE");
        assertEquals(count(null), somme);
    }

    /**
     * Le filtre doit porter sur l'ensemble des résultats, pas sur la page
     * affichée : la demande « Payé » est ici en dernière page.
     */
    @Test
    void leFiltreTraverseLesPages() {
        jeuDeDonnees();

        var premierePage = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, null, null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO, Pageable.ofSize(2));
        var toutes = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, "PAYE", null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());

        assertTrue(premierePage.getTotalPages() > 1, "jeu de test trop petit pour être paginé");
        assertEquals(1, toutes.getTotalElements());
    }

    /** Une échéance soldée mais échue n'est pas « en retard ». */
    @Test
    void factureEchueEtSoldeeNestPasEnRetard() {
        marquerTerminee(avecFactureValidee("DEM-2026-00007", AUJOURDHUI, "500000", "500000"));

        assertEquals(1, count("PAYE"));
        assertEquals(0, count("EN_RETARD"));
    }

    /**
     * La situation affichée par la liste et celle utilisée par le filtre doivent
     * être la même : une demande multi-services est jugée sur toutes ses lignes,
     * pas sur son seul premier plan de paiement.
     */
    /** Agrégat factures validées pour une demande (même requête que FactureService). */
    private List<Object[]> facturesAgregeesPourDemandes(Integer demandeId) {
        return factureValideeRepository.facturesAgregees(List.of(demandeId));
    }

    @Test
    void situationAfficheeEgaleSituationFiltre() {
        jeuDeDonnees();
        // Deux lignes : la seconde est échue et non soldée.
        DemandeService multi = avecFactureValidee("DEM-2026-00030", AUJOURDHUI,
                "1000000", "1000000");
        ligneSupplementaire(multi, AUJOURDHUI.minusDays(8), "1000000", "0");

        String affichee = SituationPaiement.classerFacture(
                facturesAgregeesPourDemandes(multi.getId()),
                5, 7, AUJOURDHUI, "EN_ATTENTE");
        assertEquals(SituationPaiement.EN_RETARD, affichee);

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, affichee, null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());
        assertTrue(filtrees.getContent().stream()
                        .anyMatch(d -> "DEM-2026-00030".equals(d.getReferenceDemande())),
                "la demande multi-services doit ressortir dans le filtre de sa situation affichée");
    }

    /** Facture supplémentaire non soldée sur une demande déjà créditée. */
    private void ligneSupplementaire(DemandeService demande, LocalDate valideLe,
                                     String montant, String paye) {
        String code = "SRV-X" + (++compteur);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Service " + code)
                .codeService(code)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(demande)
                .service(service)
                .tarif(new BigDecimal(montant))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut("EN_COURS")
                .categorie("EXTERNALISATION")
                .build());
        FactureValidee fv = factureValideeRepository.save(FactureValidee.builder()
                .referenceFactureValidee("EXTRA-" + code)
                .demande(demande)
                .montant(new BigDecimal(montant))
                .valideLe(valideLe.atTime(LocalTime.of(0, 0)))
                .build());
        BigDecimal payeBig = new BigDecimal(paye);
        if (payeBig.signum() > 0) {
            ModePaiement virement = modeRepository.findByCode("VIREMENT")
                    .orElseGet(() -> modeRepository.save(ModePaiement.builder()
                            .code("VIREMENT").libelle("Virement").actif(true).build()));
            paiementRepository.save(Paiement.builder()
                    .referencePaiement("PAY-" + code)
                    .demande(demande)
                    .factureValidee(fv)
                    .modePaiement(virement)
                    .datePaiement(AUJOURDHUI)
                    .montantPaye(payeBig)
                    .actif(true)
                    .build());
        }
    }
    @Test
    void situationPayeLigneTermineeSansPlan() {
        marquerTerminee(ligneMixte("DEM-2026-01001", "TERMINE"));
        assertEquals(1, count("PAYE"));
        assertEquals(0, count("EN_ATTENTE"));
    }

    @Test
    void situationPartielleLinesMixtes() {
        DemandeService d1 = demande("DEM-2026-02001");
        // une ligne terminée, une ligne PARTIEL → la demande est partiellement réglée
        Service sd = serviceRepository.save(Service.builder()
                .nomService("ServiceP" + (++compteur))
                .codeService("SRV-" + compteur)
                .typeService("STANDARD")
                .actif(true)
                .build());
        ligneRepository.save(LigneService.builder()
                .demande(d1)
                .service(sd)
                .tarif(new BigDecimal("1000000"))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut("TERMINE")
                .categorie("EXTERNALISATION")
                .build());
        Service sp = serviceRepository.save(Service.builder()
                .nomService("ServiceP" + (++compteur))
                .codeService("SRV-" + compteur)
                .typeService("STANDARD")
                .actif(true)
                .build());
        ligneRepository.save(LigneService.builder()
                .demande(d1)
                .service(sp)
                .tarif(new BigDecimal("5000000"))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut("PARTIEL")
                .categorie("EXTERNALISATION")
                .build());
        assertEquals(1, count("PARTIEL"));
    }

    @Test
    void situationEnAttenteLinesAucunePayee() {
        ligneMixte("DEM-2026-03001", "EN_COURS");
        assertEquals(1, count("EN_ATTENTE"));
        assertEquals(0, count("PAYE"));
        assertEquals(0, count("PARTIEL"));
    }

    private List<Object[]> situationsLignes(Integer demandeId) {
        return demandeRepository.situationsLignes(List.of(demandeId));
    }

    @Test
    void situationAfficheeEgaleSituationFiltreMixte() {
        DemandeService d = demande("DEM-2026-05001");
        Service service = serviceRepository.save(Service.builder()
                .nomService("ServiceAffiche" + (++compteur))
                .codeService("SRV-" + compteur)
                .typeService("STANDARD")
                .actif(true)
                .build());
        ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal("1000000"))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .statut("PARTIEL")
                .categorie("EXTERNALISATION")
                .build());

        List<Object[]> lignes = situationsLignes(d.getId());
        String affichee = SituationPaiement.classerLignes(
                ((Number) lignes.get(0)[1]).longValue(),
                ((Number) lignes.get(0)[2]).longValue(),
                ((Number) lignes.get(0)[3]).longValue(),
                "EN_ATTENTE");

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, affichee, null, null, AUJOURDHUI, DEADLINE_PD, DEADLINE_ABO,
                        Pageable.unpaged());
        assertTrue(filtrees.getContent().stream()
                        .anyMatch(dd -> "DEM-2026-05001".equals(dd.getReferenceDemande())),
                "la demande mixte partielle doit ressortir dans le filtre PARTIEL");
    }
}
