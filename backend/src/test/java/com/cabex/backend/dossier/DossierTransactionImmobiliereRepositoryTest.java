package com.cabex.backend.dossier;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.cabex.backend.dossier.repository.DossierTransactionImmobiliereRepository;
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
class DossierTransactionImmobiliereRepositoryTest {

    @Autowired
    private DossierTransactionImmobiliereRepository dossierRepository;
    @Autowired
    private DemandeServiceRepository demandeRepository;
    @Autowired
    private ServiceRepository serviceRepository;

    private DemandeService demandeTransaction() {
        serviceRepository.save(Service.builder()
                .nomService("Transaction immobilière")
                .codeService("SRV-DTI-001")
                .typeService("TRANSACTION_IMMOBILIERE")
                .actif(true)
                .build());
        return demandeRepository.save(DemandeService.builder()
                .referenceDemande("REF-DTI-001")
                .dateDemande(LocalDate.of(2026, 10, 1))
                .actif(true)
                .build());
    }

    @Test
    void persistanceEtLectureParDemande() {
        DemandeService demande = demandeTransaction();
        DossierTransactionImmobiliere dossier = DossierTransactionImmobiliere.builder()
                .demande(demande)
                .nomProprietairePrecedent("Rakoto Pierre")
                .actionAchat(true)
                .objet("TERRAIN")
                .achatMesureM2(new BigDecimal("500.00"))
                .achatMontant(new BigDecimal("1000000.00"))
                .regularisationTypeTerrain("COMMUNAL_DOMAINE")
                .documentPvReconnaissance(true)
                .nombreCommerciauxTerrain(2)
                .commissionCn(new BigDecimal("50000.00"))
                .build();

        DossierTransactionImmobiliere saved = dossierRepository.save(dossier);

        assertNotNull(saved.getId());
        assertTrue(dossierRepository.existsByDemandeId(demande.getId()));
        DossierTransactionImmobiliere relu = dossierRepository
                .findByDemandeId(demande.getId()).orElseThrow();
        assertEquals("Rakoto Pierre", relu.getNomProprietairePrecedent());
        assertTrue(relu.getActionAchat());
        assertFalse(relu.getActionVente());
        assertEquals("TERRAIN", relu.getObjet());
        assertEquals("COMMUNAL_DOMAINE", relu.getRegularisationTypeTerrain());
        assertTrue(relu.getDocumentPvReconnaissance());
        assertEquals(2, relu.getNombreCommerciauxTerrain());
    }

    @Test
    void unSeulDossierParDemande() {
        DemandeService demande = demandeTransaction();
        dossierRepository.save(DossierTransactionImmobiliere.builder()
                .demande(demande).actionAchat(true).objet("TERRAIN").build());

        assertThrows(DataIntegrityViolationException.class, () -> {
            dossierRepository.saveAndFlush(DossierTransactionImmobiliere.builder()
                    .demande(demande).actionVente(true).objet("MAISON").build());
        });
    }
}
