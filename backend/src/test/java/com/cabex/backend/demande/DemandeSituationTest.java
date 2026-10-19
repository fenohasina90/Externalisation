package com.cabex.backend.demande;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.SituationPaiement;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private ProposPaiementRepository proposRepository;
    @Autowired
    private EcheancePaiementRepository echeanceRepository;
    @Autowired
    private ServiceRepository serviceRepository;

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 20);
    private int compteur;

    private DemandeService demande(String reference) {
        return demandeRepository.save(DemandeService.builder()
                .referenceDemande(reference)
                .dateDemande(AUJOURDHUI)
                .societe(null)
                .actif(true)
                .build());
    }

    /**
     * Crée une demande avec un plan et une échéance.
     *
     * @param dateEcheance   date de l'échéance
     * @param attendu        montant attendu
     * @param paye           montant déjà payé
     */
    private DemandeService avecEcheance(String reference, LocalDate dateEcheance,
                                        String attendu, String paye) {
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
                .tarif(new BigDecimal(attendu))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .build());
        ProposPaiement plan = proposRepository.save(ProposPaiement.builder()
                .demande(d)
                .ligneService(ligne)
                .dateDebut(AUJOURDHUI.minusDays(30))
                .dateFin(AUJOURDHUI.plusDays(60))
                .montantTotal(new BigDecimal(attendu))
                .restePayer(new BigDecimal(attendu))
                .nombreEcheances(1)
                .statut("ACTIF")
                .build());
        echeanceRepository.save(EcheancePaiement.builder()
                .proposPaiement(plan)
                .numeroEcheance(1)
                .dateEcheance(dateEcheance)
                .montantAttendu(new BigDecimal(attendu))
                .montantPaye(new BigDecimal(paye))
                .build());
        return d;
    }

    private void jeuDeDonnees() {
        // Soldee.
        avecEcheance("DEM-2026-00001", AUJOURDHUI.plusDays(10), "1000000", "1000000");
        // Échéance dépassée, reste dû.
        avecEcheance("DEM-2026-00002", AUJOURDHUI.minusDays(5), "1000000", "200000");
        // Échéance à venir, payée partiellement.
        avecEcheance("DEM-2026-00003", AUJOURDHUI.plusDays(10), "1000000", "400000");
        // Échéance à venir, rien payé.
        avecEcheance("DEM-2026-00004", AUJOURDHUI.plusDays(10), "1000000", "0");
        // Aucun plan de paiement.
        demande("DEM-2026-00005");
    }

    private long count(String situation) {
        return demandeRepository
                .rechercher(null, null, null, null, null, null, null, null, null,
                        situation, AUJOURDHUI, Pageable.unpaged())
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
                null, null, null, null, AUJOURDHUI, Pageable.ofSize(2));
        var toutes = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, null, "PAYE", AUJOURDHUI, Pageable.unpaged());

        assertTrue(premierePage.getTotalPages() > 1, "jeu de test trop petit pour être paginé");
        assertEquals(1, toutes.getTotalElements());
    }

    /** Une échéance soldée mais échue n'est pas « en retard ». */
    @Test
    void echeanceEchueEtSoldeeNestPasEnRetard() {
        avecEcheance("DEM-2026-00007", AUJOURDHUI.minusDays(10), "500000", "500000");

        assertEquals(1, count("PAYE"));
        assertEquals(0, count("EN_RETARD"));
    }

    /**
     * La situation affichée par la liste et celle utilisée par le filtre doivent
     * être la même : une demande multi-services est jugée sur toutes ses lignes,
     * pas sur son seul premier plan de paiement.
     */
    @Test
    void situationAfficheeEgaleSituationFiltre() {
        jeuDeDonnees();
        // Deux lignes : la seconde est échue et non soldée.
        DemandeService multi = avecEcheance("DEM-2026-00030", AUJOURDHUI.plusDays(10),
                "1000000", "1000000");
        ligneSupplementaire(multi, AUJOURDHUI.minusDays(3), "1000000", "0");

        List<Object[]> lignes = demandeRepository.situations(
                List.of(multi.getId()), AUJOURDHUI);

        assertEquals(1, lignes.size());
        String affichee = SituationPaiement.classer(lignes.get(0));
        assertEquals(SituationPaiement.EN_RETARD, affichee);

        var filtrees = demandeRepository.rechercher(null, null, null, null, null, null,
                null, null, null, affichee, AUJOURDHUI, Pageable.unpaged());
        assertTrue(filtrees.getContent().stream()
                        .anyMatch(d -> "DEM-2026-00030".equals(d.getReferenceDemande())),
                "la demande multi-services doit ressortir dans le filtre de sa situation affichée");
    }

    /** Plan + échéance supplémentaire sur une demande déjà existante. */
    private void ligneSupplementaire(DemandeService demande, LocalDate dateEcheance,
                                     String attendu, String paye) {
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
                .tarif(new BigDecimal(attendu))
                .debutService(AUJOURDHUI.minusDays(30))
                .finService(AUJOURDHUI.plusDays(60))
                .build());
        ProposPaiement plan = proposRepository.save(ProposPaiement.builder()
                .demande(demande)
                .ligneService(ligne)
                .dateDebut(AUJOURDHUI.minusDays(30))
                .dateFin(AUJOURDHUI.plusDays(60))
                .montantTotal(new BigDecimal(attendu))
                .restePayer(new BigDecimal(attendu))
                .nombreEcheances(1)
                .statut("ACTIF")
                .build());
        echeanceRepository.save(EcheancePaiement.builder()
                .proposPaiement(plan)
                .numeroEcheance(1)
                .dateEcheance(dateEcheance)
                .montantAttendu(new BigDecimal(attendu))
                .montantPaye(new BigDecimal(paye))
                .build());
    }
}