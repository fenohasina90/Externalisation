package com.cabex.backend.document;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Facture du mois d'un abonnement : le PDF affiche la référence de la facture
 * générale validée, celle de la facture proforma et celle de la demande.
 */
@ExtendWith(MockitoExtension.class)
class FactureAbonnementPdfTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    void pdfFactureMensuelleContientLesReferences() {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(Service.builder().id(1).nomService("Abonnement comptabilité").build())
                .tarif(new BigDecimal("1500000"))
                .typePrestation("ABONNEMENT_MENSUEL")
                .debutService(LocalDate.of(2026, 11, 1))
                .statut("EN_COURS")
                .build();
        Facture proforma = Facture.builder().id(1)
                .referenceFacture("PRO-01112026-00001")
                .demande(demande)
                .montant(new BigDecimal("1500000"))
                .statut("VALIDEE")
                .build();
        proforma.getLignes().add(ligne);
        FactureValidee generale = FactureValidee.builder().id(1)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .factureProforma(proforma)
                .demande(demande)
                .montant(new BigDecimal("1500000"))
                .build();
        FactureValidee duMois = FactureValidee.builder().id(2)
                .referenceFactureValidee("00002/EXT/TNR/2026")
                .factureGenerale(generale)
                .ligneService(ligne)
                .demande(demande)
                .montant(new BigDecimal("1500000"))
                .valideLe(LocalDateTime.of(2026, 11, 10, 0, 0))
                .build();

        byte[] pdf = new PdfGenerator(configurationService).factureAbonnement(duMois);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        String texte;
        try (com.lowagie.text.pdf.PdfReader lecteur = new com.lowagie.text.pdf.PdfReader(pdf)) {
            com.lowagie.text.pdf.parser.PdfTextExtractor extracteur =
                    new com.lowagie.text.pdf.parser.PdfTextExtractor(lecteur);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
                sb.append(extracteur.getTextFromPage(page));
            }
            texte = sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lecture du PDF généré impossible", e);
        }
        assertTrue(texte.contains("00002/EXT/TNR/2026"), "référence de la facture mensuelle");
        assertTrue(texte.contains("00001/EXT/TNR/2026"), "référence de la facture générale");
        assertTrue(texte.contains("PRO-01112026-00001"), "référence proforma");
        assertTrue(texte.contains("DEM-2026-00017"), "référence demande");
    }
}
