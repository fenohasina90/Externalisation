package com.cabex.backend.dossier;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.dossier.repository.DossierAssocieRepository;
import com.cabex.backend.dossier.repository.DossierCreationRepository;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DossierRepositoryTest {

    @Autowired
    private DossierCreationRepository dossierRepository;
    @Autowired
    private DossierAssocieRepository associeRepository;
    @Autowired
    private DemandeServiceRepository demandeRepository;
    @Autowired
    private ServiceRepository serviceRepository;

    private DemandeService demandeSansSociete() {
        Service service = serviceRepository.save(Service.builder()
                .nomService("Création d'entreprise")
                .codeService("SRV-0001")
                
                .actif(true)
                .build());
        return demandeRepository.save(DemandeService.builder()
                .referenceDemande("REF-2026-126")
                .dateDemande(LocalDate.of(2026, 9, 1))
                .societe(null)
                .actif(true)
                .build());
    }

    @Test
    void persistanceEnCascadeEtLectureParDemande() {
        DemandeService demande = demandeSansSociete();
        DossierCreation dossier = DossierCreation.builder()
                .demande(demande)
                .formeJuridique("SARL")
                .denomination("Sahan Agro SARL")
                .locationGerance(false)
                .build();
        dossier.getAssocies().add(DossierAssocie.builder()
                .dossier(dossier).ordre(0).nom("Nirina Rasoanaivo").partSociale("60 %").build());
        dossier.getAssocies().add(DossierAssocie.builder()
                .dossier(dossier).ordre(1).nom("Tovo Randria").partSociale("40 %").build());

        DossierCreation saved = dossierRepository.save(dossier);

        assertNotNull(saved.getId());
        assertTrue(dossierRepository.existsByDemandeId(demande.getId()));
        assertEquals(2, associeRepository.findByDossierIdOrderByOrdreAsc(saved.getId()).size());
        assertEquals("Nirina Rasoanaivo",
                dossierRepository.findByDemandeId(demande.getId()).orElseThrow().getAssocies().get(0).getNom());
    }

    @Test
    void unSeulDossierParDemande() {
        DemandeService demande = demandeSansSociete();
        dossierRepository.save(DossierCreation.builder()
                .demande(demande).formeJuridique("SARL").denomination("A")
                .locationGerance(false).build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            dossierRepository.saveAndFlush(DossierCreation.builder()
                    .demande(demande).formeJuridique("SARL").denomination("B")
                    .locationGerance(false).build());
        });
    }

    @Test
    void suppressionEnCascadeDesAssocies() {
        DemandeService demande = demandeSansSociete();
        DossierCreation dossier = DossierCreation.builder()
                .demande(demande).formeJuridique("SARL").denomination("A")
                .locationGerance(false).build();
        dossier.getAssocies().add(DossierAssocie.builder()
                .dossier(dossier).ordre(0).nom("Nirina Rasoanaivo").build());
        Integer id = dossierRepository.save(dossier).getId();

        dossierRepository.deleteById(id);

        assertTrue(associeRepository.findByDossierIdOrderByOrdreAsc(id).isEmpty());
    }
}
