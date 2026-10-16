package com.cabex.backend.document;

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
        Facture facture = Facture.builder().id(1).referenceFacture("PRO-2026-00001")
                .demande(demande).modeFacturation("PAR_SERVICE")
                .montant(new BigDecimal("2000000")).statut("PROFORMA")
                .lignes(Set.of(ligne)).build();

        byte[] pdf = new PdfGenerator(configurationService).factureProforma(facture);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
