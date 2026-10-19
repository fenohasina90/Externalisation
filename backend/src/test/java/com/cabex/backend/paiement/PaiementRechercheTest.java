package com.cabex.backend.paiement;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import com.cabex.backend.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filtre de recherche de l'historique des paiements : la recherche doit porter
 * sur la référence du paiement ET celle de la demande, le client, le mode et
 * la référence de virement. Les paiements archivés (soft-delete, règle R12)
 * restent exclus, y compris de la recherche globale.
 */
@DataJpaTest
class PaiementRechercheTest {

    @Autowired
    private PaiementRepository paiementRepository;
    @Autowired
    private DemandeServiceRepository demandeRepository;
    @Autowired
    private LigneServiceRepository ligneRepository;
    @Autowired
    private ProposPaiementRepository proposRepository;
    @Autowired
    private EcheancePaiementRepository echeanceRepository;
    @Autowired
    private ModePaiementRepository modeRepository;
    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private DomaineServiceRepository domaineRepository;

    private ModePaiement virement;

    private DemandeService demande(String reference, Societe societe) {
        return demandeRepository.save(DemandeService.builder()
                .referenceDemande(reference)
                .dateDemande(LocalDate.of(2026, 10, 18))
                .societe(societe)
                .actif(true)
                .build());
    }

    private Paiement paiement(String reference, DemandeService demande, ModePaiement mode,
                              String refVirement, boolean actif) {
        return paiementRepository.save(Paiement.builder()
                .referencePaiement(reference)
                .demande(demande)
                .modePaiement(mode)
                .datePaiement(LocalDate.of(2026, 10, 20))
                .montantPaye(new BigDecimal("250000"))
                .referenceVirement(refVirement)
                .actif(actif)
                .build());
    }

    private DemandeService demandeAvecEcheance(String reference, Societe societe, String montant) {
        DemandeService d = demande(reference, societe);
        Service service = serviceRepository.save(Service.builder()
                .nomService("Conseil")
                .codeService("SRV-" + reference)
                .typeService("STANDARD")
                .actif(true)
                .build());
        LigneService ligne = ligneRepository.save(LigneService.builder()
                .demande(d)
                .service(service)
                .tarif(new BigDecimal(montant))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16))
                .build());
        ProposPaiement plan = proposRepository.save(ProposPaiement.builder()
                .demande(d)
                .ligneService(ligne)
                .dateDebut(LocalDate.of(2026, 10, 18))
                .dateFin(LocalDate.of(2026, 12, 16))
                .montantTotal(new BigDecimal(montant))
                .restePayer(new BigDecimal(montant))
                .nombreEcheances(1)
                .statut("ACTIF")
                .build());
        return echeanceRepository.save(EcheancePaiement.builder()
                .proposPaiement(plan)
                .numeroEcheance(1)
                .dateEcheance(LocalDate.of(2026, 11, 18))
                .montantAttendu(new BigDecimal(montant))
                .montantPaye(BigDecimal.ZERO)
                .build()).getProposPaiement().getDemande();
    }

    private void jeuDeDonnees() {
        virement = modeRepository.save(ModePaiement.builder()
                .code("VIREMENT")
                .libelle("Virement")
                .actif(true)
                .build());
        ModePaiement especes = modeRepository.save(ModePaiement.builder()
                .code("ESPECES")
                .libelle("Espèces")
                .actif(true)
                .build());

        DomaineService domaine = domaineRepository.save(DomaineService.builder()
                .nomDomaine("Conseil")
                .actif(true)
                .build());
        Societe atlas = societeRepository.save(Societe.builder()
                .nomSociete("Atlas Conseil")
                .domaine(domaine)
                .actif(true)
                .build());

        DemandeService d1 = demandeAvecEcheance("DEM-2026-00017", atlas, "1000000");
        paiement("PAY-2026-00001", d1, virement, "VIR-2026-1004", true);

        DemandeService d2 = demande("DEM-2026-00018", null);
        paiement("PAY-2026-00002", d2, especes, null, true);
    }

    @Test
    void rechercheParReferenceDemande() {
        jeuDeDonnees();

        var page = paiementRepository.rechercher(
                "DEM-2026-00017", null, null, null, null, Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("PAY-2026-00001", page.getContent().get(0).getReferencePaiement());
    }

    @Test
    void rechercheParReferencePaiement() {
        jeuDeDonnees();

        var page = paiementRepository.rechercher(
                "PAY-2026-00001", null, null, null, null, Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void rechercheParClientEtParMode() {
        jeuDeDonnees();

        var parClient = paiementRepository.rechercher(
                "atlas", null, null, null, null, Pageable.unpaged());
        assertEquals(1, parClient.getTotalElements());

        // Libellé du mode sans accent à tester (« Virement »), la comparaison
        // étant faite sur LOWER().
        var parMode = paiementRepository.rechercher(
                "virement", null, null, null, null, Pageable.unpaged());
        assertEquals(1, parMode.getTotalElements());
        assertEquals("PAY-2026-00001", parMode.getContent().get(0).getReferencePaiement());
    }

    @Test
    void rechercheParReferenceVirement() {
        jeuDeDonnees();

        var page = paiementRepository.rechercher(
                "VIR-2026-1004", null, null, null, null, Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    /** Un paiement d'une demande sans société reste trouvable (LEFT JOIN). */
    @Test
    void rechercheSansSociete() {
        jeuDeDonnees();

        var parReference = paiementRepository.rechercher(
                "PAY-2026-00002", null, null, null, null, Pageable.unpaged());
        assertEquals(1, parReference.getTotalElements());

        var parDemande = paiementRepository.rechercher(
                "DEM-2026-00018", null, null, null, null, Pageable.unpaged());
        assertEquals(1, parDemande.getTotalElements());
    }

    /** Les paiements archivés ne sont jamais retournés par le filtre. */
    @Test
    void paiementArchiveExcluDuFiltre() {
        jeuDeDonnees();
        paiement("PAY-2026-00009", demande("DEM-2026-00019", null), virement, null, false);

        var page = paiementRepository.rechercher(
                "DEM-2026-00019", null, null, null, null, Pageable.unpaged());

        assertEquals(0, page.getTotalElements());
    }

    /** Règle R12 : `actif = true` doit couvrir toutes les branches de la recherche globale. */
    @Test
    void paiementArchiveExcluDeLaRechercheGlobale() {
        jeuDeDonnees();
        DemandeService dArchive = demande("DEM-2026-00021", null);
        paiement("PAY-2026-00010", dArchive, virement, null, true);

        var resultats = paiementRepository.rechercherGlobale(
                "DEM-2026-00021", Pageable.unpaged());

        assertEquals(1, resultats.size());
        assertTrue(resultats.stream()
                .allMatch(p -> "PAY-2026-00010".equals(p.getReferencePaiement())));
    }
}