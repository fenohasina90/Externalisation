package com.cabex.backend.facture;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La proforma fige le tarif à la génération : après la validation (qui
 * écrase ligne_service.tarif avec le montant validé), le PDF proforma
 * affiche toujours le tarif figé.
 */
@ExtendWith(MockitoExtension.class)
class FactureProformaTarifFreezeTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    void pdfProformaAfficheTarifFiGePasTarifValidee() throws Exception {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder().id(1)
                .referenceDemande("DEM-2026-00001").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .categorie("EXTERNALISATION")
                .typePrestation("PRESTATION")
                .service(Service.builder().id(1).nomService("Comptabilité").build())
                .tarif(new BigDecimal("1800000"))
                .tarifProforma(new BigDecimal("1500000"))
                .debutService(LocalDate.of(2026, 10, 1))
                .finService(LocalDate.of(2026, 12, 31))
                .statut("VALIDEE")
                .build();
        Facture proforma = Facture.builder().id(1)
                .referenceFacture("PRO-2026-00001")
                .demande(demande)
                .montant(new BigDecimal("1500000"))
                .statut("VALIDEE")
                .build();
        proforma.getLignes().add(ligne);

        byte[] pdf = new PdfGenerator(configurationService).factureProforma(proforma);
        assertNotNull(pdf);

        String texte;
        try (com.lowagie.text.pdf.PdfReader lecteur = new com.lowagie.text.pdf.PdfReader(pdf)) {
            com.lowagie.text.pdf.parser.PdfTextExtractor extracteur =
                    new com.lowagie.text.pdf.parser.PdfTextExtractor(lecteur);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
                sb.append(extracteur.getTextFromPage(page));
            }
            texte = sb.toString().replaceAll("[^0-9]", "");
        }

        assertTrue(texte.contains("1500000"), "tarif figé de la proforma doit apparaître dans le PDF");
        assertFalse(texte.contains("1800000"), "tarif validé ne doit pas apparaître dans la proforma");
    }
}
