package com.cabex.backend.document;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La fiche de création reprend la fiche papier (cases, associés,
 * signatures) dans le design proforma.
 */
@ExtendWith(MockitoExtension.class)
class FicheCreationPdfTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    void pdfFicheGenere() {
        Service service = Service.builder().id(9).nomService("Création d'entreprise")
                .typeService("CREATION").build();
        DemandeService demande = DemandeService.builder().id(126)
                .referenceDemande("DEM-2026-00126")
                .dateDemande(LocalDate.of(2026, 10, 18)).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(service).tarif(new BigDecimal("700000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2027, 2, 28)).build();
        demande.setLignes(Set.of(ligne));
        DossierCreation dossier = DossierCreation.builder().id(1).demande(demande)
                .formeJuridique("SARL").denomination("Sahan Agro SARL")
                .capitalSocial(new BigDecimal("5000000")).nombreAssociesDeclare(2)
                .nomGerant("Nirina Rasoanaivo")
                .associes(List.of(
                        DossierAssocie.builder().nom("Nirina Rasoanaivo")
                                .partSociale("60 %").ordre(0).build()))
                .build();

        byte[] pdf = new PdfGenerator(configurationService).ficheCreation(demande, dossier);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
