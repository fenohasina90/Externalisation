package com.cabex.backend.client;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Filtre d'archivage de la recherche paginée (H2). */
@DataJpaTest
class SocieteRepositoryTest {

    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private DomaineServiceRepository domaineRepository;

    private Societe societe(String nom, boolean actif, boolean archivee) {
        DomaineService domaine = domaineRepository.findAll().stream().findFirst()
                .orElseGet(() -> domaineRepository.save(DomaineService.builder()
                        .nomDomaine("Comptabilité").actif(true).build()));
        Societe s = Societe.builder().nomSociete(nom).actif(actif).domaine(domaine).build();
        if (archivee) {
            s.setArchivedAt(LocalDateTime.now());
        }
        return societeRepository.save(s);
    }

    @Test
    void filtreArchive() {
        societe("Atlas Conseil", true, false);
        societe("Globex France", false, false);
        societe("Hélios Groupe", false, true);

        assertEquals(1, societeRepository
                .rechercher(null, null, null, true, PageRequest.of(0, 8)).getTotalElements());
        assertEquals(2, societeRepository
                .rechercher(null, null, null, false, PageRequest.of(0, 8)).getTotalElements());
        assertEquals(1, societeRepository
                .rechercher(null, true, null, null, PageRequest.of(0, 8)).getTotalElements());
        assertEquals(3, societeRepository
                .rechercher(null, null, null, null, PageRequest.of(0, 8)).getTotalElements());
    }
}
