package com.cabex.backend.document;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Facture du mois d'un abonnement : le PDF affiche la référence de la facture
 * générale validée, celle de la facture proforma et celle de la demande.
 */
@ExtendWith(MockitoExtension.class)
class FactureAbonnementPdfTest {

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private PaiementRepository paiementRepository;

    private String extraireTexte(byte[] pdf) {
        try (com.lowagie.text.pdf.PdfReader lecteur = new com.lowagie.text.pdf.PdfReader(pdf)) {
            com.lowagie.text.pdf.parser.PdfTextExtractor extracteur =
                    new com.lowagie.text.pdf.parser.PdfTextExtractor(lecteur);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
                sb.append(extracteur.getTextFromPage(page));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lecture du PDF généré impossible", e);
        }
    }

    private FactureValidee factureDuMois() {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        DemandeService demandeDebours = DemandeService.builder().id(18)
                .referenceDemande("DEM-2026-00018").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(Service.builder().id(1).nomService("Abonnement comptabilité").build())
                .tarif(new BigDecimal("1500000"))
                .typePrestation("ABONNEMENT_MENSUEL")
                .debutService(java.time.LocalDate.of(2026, 11, 1))
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
                .mois(java.time.LocalDate.of(2026, 1, 1))
                .build();
        com.cabex.backend.facture.entity.FactureValideeLigne ligneAbo =
                com.cabex.backend.facture.entity.FactureValideeLigne.builder()
                        .id(11)
                        .factureValidee(duMois)
                        .ligneService(ligne)
                        .designation("Abonnement mensuel — Abonnement comptabilité")
                        .montant(new BigDecimal("1500000"))
                        .type("PRINCIPALE")
                        .build();
        LigneService debours = LigneService.builder().id(9).demande(demandeDebours)
                .categorie("DEBOURS").statut("NON_PAYEE")
                .designationDebours("Timbres postaux")
                .montantDebours(new BigDecimal("250000"))
                .build();
        com.cabex.backend.facture.entity.FactureValideeLigne ligneDebours =
                com.cabex.backend.facture.entity.FactureValideeLigne.builder()
                        .id(12)
                        .factureValidee(duMois)
                        .ligneService(debours)
                        .designation("Débours — Timbres postaux")
                        .montant(new BigDecimal("250000"))
                        .type("RAPPEL")
                        .build();
        duMois.setLignes(new java.util.HashSet<>(java.util.Set.of(ligneAbo, ligneDebours)));
        return duMois;
    }

    @Test
    void pdfFactureMensuelleContientLesReferences() {
        FactureValidee duMois = factureDuMois();

        byte[] pdf = new PdfGenerator(configurationService).factureAbonnement(duMois);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        String texte = extraireTexte(pdf);
        assertTrue(texte.contains("00002/EXT/TNR/2026"), "référence de la facture mensuelle");
        // La facture générale n'est volontairement plus affichée sur la mensuelle.
        assertTrue(!texte.contains("00001/EXT/TNR/2026"), "référence générale absente : toutes les factures sont identiques");
        assertTrue(texte.contains("PRO-01112026-00001"), "référence proforma");
        assertTrue(texte.contains("DEM-2026-00017"), "référence demande");
        assertTrue(texte.contains("Abonnement comptabilité"),
                "désignation figée de la ligne d'abonnement persistée à la création");
        assertTrue(texte.contains("Timbres postaux"), "désignation débours");
        assertTrue(!texte.contains("ZÉRO ARIARY"),
                "le montant en lettres doit porter sur le total TTC, même si la facture est payée");
        assertTrue(texte.contains("DEM-2026-00018"), "demande de la ligne débours");
        assertTrue(texte.toLowerCase(Locale.FRANCE).replaceAll("\\s+", " ").contains("janvier 2026"), "mois facturé");
        assertTrue(texte.toLowerCase(Locale.FRANCE).contains("rappel"), "section de rappel");
    }

    @Test
    void pdfFactureMensuelleDejaPayee_leMontantEnLettresPorteSurLeTotalTtc() {
        FactureValidee duMois = factureDuMois();
        when(paiementRepository.sommePayeeParFactureValidee(2))
                .thenReturn(new BigDecimal("1500000"));

        byte[] pdf = new PdfGenerator(configurationService, paiementRepository).factureAbonnement(duMois);

        String texte = extraireTexte(pdf);
        assertTrue(!texte.contains("ZÉRO ARIARY"),
                "une facture entièrement payée ne doit pas être arrêtée à ZÉRO : lettres = total TTC");
        assertTrue(texte.toUpperCase(Locale.FRANCE).contains("UN MILLION CINQ CENT MILLE ARIARY"),
                "le montant en lettres doit reprendre le total TTC (1 500 000 Ar)");
        assertTrue(texte.contains("0,00") || texte.contains("0 Ar") || texte.contains("NET À PAYER"),
                "la section NET À PAYER reste affichée à 0");
    }
}
