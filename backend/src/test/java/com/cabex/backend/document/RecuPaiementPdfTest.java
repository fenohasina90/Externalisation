package com.cabex.backend.document;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.document.service.PdfGenerator;
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
}
