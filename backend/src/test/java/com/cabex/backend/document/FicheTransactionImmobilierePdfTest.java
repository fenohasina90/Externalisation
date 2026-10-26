package com.cabex.backend.document;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La fiche de transaction immobilière reprend la fiche papier (actions, objet,
 * achat/vente, régularisation, suivi, signatures) dans la charte du logo.
 */
@ExtendWith(MockitoExtension.class)
class FicheTransactionImmobilierePdfTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    void pdfFicheTransactionGenere() throws Exception {
        Societe societe = Societe.builder().id(1)
                .nomSociete("Rakoto Immobilier")
                .contact("034 00 000 00")
                .email("contact@rakoto.mg")
                .fb("Rakoto Immobilier")
                .adresse("Lot II A 12 Antananarivo")
                .build();
        Commercial commercial = Commercial.builder().id(2)
                .nomCommercial("Jean Dupont").immatricule("C001").build();
        DemandeService demande = DemandeService.builder().id(126)
                .referenceDemande("DEM-2026-00126")
                .dateDemande(LocalDate.of(2026, 10, 18))
                .societe(societe).commercial(commercial).build();
        DossierTransactionImmobiliere dossier = DossierTransactionImmobiliere.builder()
                .id(1).demande(demande)
                .nomProprietairePrecedent("Rakoto Pierre")
                .actionAchat(true).actionVente(true)
                .objet("TERRAIN")
                .achatMesureM2(new BigDecimal("500"))
                .achatMontant(new BigDecimal("1000000"))
                .venteMesureM2(new BigDecimal("300"))
                .venteMontant(new BigDecimal("2500000"))
                .ventePrisePhoto(true).venteVisite(false)
                .regularisationTypeTerrain("COMMUNAL_DOMAINE")
                .regularisationSituationTerrain("TITRE_BORNE")
                .documentPvReconnaissance(true)
                .observations("Dossier complet")
                .nombreCommerciauxTerrain(2)
                .commissionCn(new BigDecimal("50000"))
                .prestationAbonnementMensuel(true)
                .build();

        byte[] pdf = new PdfGenerator(configurationService)
                .ficheTransactionImmobiliere(demande, dossier);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        PdfReader lecteur = new PdfReader(pdf);
        try {
            assertEquals(1, lecteur.getNumberOfPages());
        } finally {
            lecteur.close();
        }
    }
}
