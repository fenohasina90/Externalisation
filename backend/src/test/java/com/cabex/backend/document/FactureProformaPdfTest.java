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
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La proforma générée est un PDF Ipso (modèle CABEX : logo, tableau,
 * totaux, mentions de bas de page).
 */
@ExtendWith(MockitoExtension.class)
class FactureProformaPdfTest {

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

    @Test
    void pdfGenereAuFormatCabex() {
        when(configurationService.lireValeur("TAUX_TVA", "0")).thenReturn("20");
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil")
                .adresse("Lot II").contact("+261340000000").email("contact@atlas.mg").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(Service.builder().id(1).nomService("Audit comptable").build())
                .tarif(new BigDecimal("2000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16))
                .build();
        Facture facture = Facture.builder().id(1).referenceFacture("PRO-18092026-00001")
                .demande(demande).modeFacturation("PAR_SERVICE")
                .montant(new BigDecimal("2000000")).statut("PROFORMA")
                .lignes(Set.of(ligne)).build();

        byte[] pdf = new PdfGenerator(configurationService).factureProforma(facture);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void pdfValideeDocument_paiementsPrisEnCompte_commeLesAutresFactures() {
        when(configurationService.lireValeur("TAUX_TVA", "0")).thenReturn("0");
        Societe societe = Societe.builder().id(1).nomSociete("BOA")
                .adresse("-").contact("-").email("-").build();
        DemandeService demande = DemandeService.builder().id(17)
                .referenceDemande("DEM-2026-00017").societe(societe).build();
        LigneService ligne = LigneService.builder().id(1).demande(demande)
                .service(Service.builder().id(1).nomService("Audit comptable").build())
                .tarif(new BigDecimal("1000000"))
                .debutService(LocalDate.of(2026, 10, 18))
                .finService(LocalDate.of(2026, 12, 16))
                .build();
        Facture proforma = Facture.builder().id(1).referenceFacture("PRO-18092026-00001")
                .demande(demande).modeFacturation("UNIQUE")
                .montant(new BigDecimal("1000000")).statut("VALIDEE")
                .lignes(Set.of(ligne)).build();
        // Première facture validée (générale, éventuellement marquée comme mensuelle) :
        // les encaissements sont liés à ELLE, pas aux échéances de plans de paiement.
        FactureValidee fv = FactureValidee.builder().id(3)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .factureProforma(proforma)
                .demande(demande)
                .montant(new BigDecimal("1000000"))
                .valideLe(java.time.LocalDateTime.of(2026, 11, 18, 0, 0))
                .mois(java.time.LocalDate.of(2026, 10, 1))
                .build();
        when(paiementRepository.sommePayeeParFactureValidee(3)).thenReturn(new BigDecimal("1000000"));

        byte[] pdf = new PdfGenerator(configurationService, paiementRepository)
                .factureValideeDocument(fv);

        String texte = extraireTexte(pdf)
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2009', ' ')
                .replaceAll("\s+", " ");
        assertTrue(texte.contains("N° FACTURE VALIDÉE"), "badge de facture validée");
        assertTrue(texte.contains("Avance reçue1 000 000 Ar"),
                "l'avance reçue reflète les paiements encaissés sur la facture validée");
        assertTrue(texte.contains("NET À PAYER0 Ar"),
                "NET À PAYER est à jour : facture payée en totalité");
        assertTrue(!texte.contains("Avance reçue0 Ar"),
                "régression : avance reçue à 0 alors que la facture est soldée");
    }
}
