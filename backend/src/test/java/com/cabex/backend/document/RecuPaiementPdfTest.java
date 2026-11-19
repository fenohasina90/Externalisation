package com.cabex.backend.document;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.entity.PieceJointe;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.facture.dto.ObjetLigne;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RecuPaiementPdfTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    void recuGenereAvecFactureValideeEtMontants() {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(Service.builder().id(1).nomService("Audit comptable").build())
                .tarif(new BigDecimal("2000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16))
                .build();
        Facture proforma = Facture.builder().id(1)
                .referenceFacture("PRO-18092026-00001")
                .demande(demande)
                .montant(new BigDecimal("2000000"))
                .statut("VALIDEE")
                .build();
        FactureValidee fv = FactureValidee.builder().id(1)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .factureProforma(proforma)
                .demande(demande)
                .montant(new BigDecimal("2000000"))
                .build();
        Paiement paiement = Paiement.builder().id(1).referencePaiement("PAY-2026-00001")
                .demande(demande)
                .factureValidee(fv)
                .modePaiement(ModePaiement.builder().id(1).code("ESPECES")
                        .libelle("Espèces").actif(true).build())
                .datePaiement(LocalDate.of(2026, 10, 19))
                .montantPaye(new BigDecimal("500000")).build();

        byte[] pdf = new PdfGenerator(configurationService).recuPaiement(paiement);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void recuAfficheServicesCommentaireEtPiecesJointes() {
        Societe societe = Societe.builder().id(1).nomSociete("BOA").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        FactureValidee fv = FactureValidee.builder().id(1)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .demande(demande)
                .montant(new BigDecimal("1000000"))
                .valideLe(LocalDateTime.of(2026, 11, 18, 0, 0))
                .build();
        Paiement paiement = Paiement.builder().id(1).referencePaiement("PAY-2026-00099")
                .demande(demande)
                .factureValidee(fv)
                .modePaiement(ModePaiement.builder().id(1).code("CHEQUE")
                        .libelle("Chèque").actif(true).build())
                .datePaiement(LocalDate.of(2026, 11, 18))
                .montantPaye(new BigDecimal("1000000"))
                .commentaire("Cheque remis au cabinet Tana")
                .banque("BAOBAB BANQUE MADAGASCAR")
                .numeroCheque("00004521").build();

        List<ObjetLigne> objets = List.of(
                new ObjetLigne("ABONNEMENT_MENSUEL", "Audit et prévention fiscaux (novembre 2026)"),
                new ObjetLigne("ABONNEMENT_MENSUEL", "Tenue de la comptabilité (novembre 2026)"),
                new ObjetLigne("PRESTATION", "Assistance lors des contrôles fiscaux"),
                new ObjetLigne("DEBOURS", "TIMBRE"));

        List<PieceJointe> pieces = List.of(
                PieceJointe.builder().id(11).nomFichier("recu-signe.pdf").nomOriginal("recu-signe.pdf")
                        .typeFichier("pdf").tailleFichier(123456L).build(),
                PieceJointe.builder().id(12).nomFichier("cheque-copie.jpg").nomOriginal("cheque-copie.jpg")
                        .typeFichier("jpg").tailleFichier(98304L).build());

        byte[] pdf = new PdfGenerator(configurationService)
                .recuPaiement(paiement, new BigDecimal("1000000"), null, objets, pieces);

        try (com.lowagie.text.pdf.PdfReader lecteur = new com.lowagie.text.pdf.PdfReader(pdf)) {
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
                sb.append(new com.lowagie.text.pdf.parser.PdfTextExtractor(lecteur)
                        .getTextFromPage(page));
            }
            String texte = sb.toString()
                    .replace('\u00A0', ' ')
                    .replace('\u202F', ' ')
                    .replace('\u2009', ' ')
                    .replaceAll("\s+", " ");

            int i = texte.indexOf("SERVICE");
            if (i >= 0) {
                System.out.println("SERVICE at " + i + " ctx=[" + texte.substring(i, Math.min(texte.length(), i + 60)) + "]");
            } else {
                System.out.println("SERVICE not found — full dump below:\n" + texte);
            }
            assertTrue(texte.contains("SERVICE"), "la carte historique est toujours là");
            assertTrue(texte.contains("Audit et prévention fiscaux"), "1re ligne d'abonnement");
            assertTrue(texte.contains("Tenue de la comptabilité"), "2e ligne d'abonnement");
            assertTrue(texte.contains("Assistance lors des contrôles fiscaux"), "prestation");
            assertTrue(texte.contains("TIMBRE"), "débours");
            assertTrue(texte.contains("PRESTATION"), "préfixe de type Prestation");
            assertTrue(texte.contains("ABONNEMENT"), "préfixe de type Abonnement");
            assertTrue(texte.contains("DÉBOURS") || texte.contains("DEBOURS"), "préfixe de type Débours");
            assertTrue(texte.contains("COMMENTAIRE"), "section « Commentaire » présente");
            assertTrue(texte.contains("Cheque remis au cabinet Tana"), "commentaire du paiement");
            assertTrue(texte.contains("PIÈCES JOINTES (2)"), "section « Pièces jointes » présente");
            assertTrue(texte.contains("recu-signe.pdf"), "première pièce jointe");
            assertTrue(texte.contains("cheque-copie.jpg"), "deuxième pièce jointe");
            // l'ancienne carte reprend l'objet précis de la facture validée
            assertTrue(texte.contains("SERVICE ET FACTURE"), "la carte historique est toujours là");
        } catch (Exception e) {
            throw new IllegalStateException("Lecture du PDF généré impossible", e);
        }
    }
}
