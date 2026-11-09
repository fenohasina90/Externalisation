package com.cabex.backend.demande;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.SocieteRepository;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Les demandes sans société (création d'entreprise) doivent figurer
 * dans la liste : les filtres optionnels utilisent des LEFT JOIN,
 * pas des jointures internes. La recherche porte sur la référence de la
 * demande et sur le nom du client.
 */
@DataJpaTest
class DemandeRechercheTest {

    @Autowired
    private DemandeServiceRepository demandeRepository;
    @Autowired
    private LigneServiceRepository ligneRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private DomaineServiceRepository domaineRepository;

    @Test
    void demandeSansSocietePresenteDansLaListe() {
        Service service = serviceRepository.save(Service.builder()
                .nomService("Création d'entreprise")
                .codeService("SRV-0001")
                .typeService("CREATION")
                .actif(true)
                .build());
        DemandeService demande = demandeRepository.save(DemandeService.builder()
                .referenceDemande("DEM-2026-00017")
                .dateDemande(LocalDate.of(2026, 10, 18))
                .societe(null)
                .actif(true)
                .build());
        ligneRepository.save(LigneService.builder()
                .demande(demande)
                .service(service)
                .tarif(new BigDecimal("2000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16))
                .build());

        var page = demandeRepository.rechercher(
                null, null, null, null, null, null, null, null, null, null,
                Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("DEM-2026-00017", page.getContent().get(0).getReferenceDemande());
    }

    @Test
    void rechercheParReferenceDemande() {
        demandeAvecSociete();

        var page = demandeRepository.rechercher(
                "DEM-2026-00021", null, null, null, null, null, null, null, null, null,
                Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void rechercheParNomDuClient() {
        demandeAvecSociete();

        var page = demandeRepository.rechercher(
                "atlas", null, null, null, null, null, null, null, null, null,
                Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("DEM-2026-00021", page.getContent().get(0).getReferenceDemande());
    }

    /** Une demande sans société reste trouvable (LEFT JOIN). */
    @Test
    void rechercheSansSociete() {
        DomaineService domaine = domaineRepository.save(DomaineService.builder()
                .nomDomaine("Formalités")
                .codeDomaine("FORM")
                .actif(true)
                .build());
        Societe autre = societeRepository.save(Societe.builder()
                .nomSociete("Globex France")
                .actif(true)
                .build());
        demandeRepository.save(DemandeService.builder()
                .referenceDemande("DEM-2026-00022")
                .dateDemande(LocalDate.of(2026, 10, 19))
                .societe(autre)
                .actif(true)
                .build());
        demandeRepository.save(DemandeService.builder()
                .referenceDemande("DEM-2026-00023")
                .dateDemande(LocalDate.of(2026, 10, 19))
                .societe(null)
                .actif(true)
                .build());

        var page = demandeRepository.rechercher(
                "DEM-2026-00023", null, null, null, null, null, null, null, null, null,
                Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("DEM-2026-00023", page.getContent().get(0).getReferenceDemande());
    }

    private void demandeAvecSociete() {
        DomaineService domaine = domaineRepository.save(DomaineService.builder()
                .nomDomaine("Conseil")
                .codeDomaine("CONSEIL")
                .actif(true)
                .build());
        Societe atlas = societeRepository.save(Societe.builder()
                .nomSociete("Atlas Conseil")
                .actif(true)
                .build());
        demandeRepository.save(DemandeService.builder()
                .referenceDemande("DEM-2026-00021")
                .dateDemande(LocalDate.of(2026, 10, 18))
                .societe(atlas)
                .actif(true)
                .build());
    }
}
