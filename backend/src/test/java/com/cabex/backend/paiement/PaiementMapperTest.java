package com.cabex.backend.paiement;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.mapper.PaiementMapper;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Le détail d'un paiement reprend les informations de la facture validée.
 */
class PaiementMapperTest {

    @Test
    void detailReprendLesInformationsDuRecu() {
        Societe societe = Societe.builder().id(5).nomSociete("Beta SA").build();
        DemandeService demande = DemandeService.builder().id(18)
                .referenceDemande("DEM-2026-00018").societe(societe).build();
        Facture proforma = Facture.builder().id(7)
                .referenceFacture("PRO-2026-00001")
                .demande(demande)
                .montant(new BigDecimal("2000000.00"))
                .statut("VALIDEE")
                .build();
        LigneService ligne = LigneService.builder().id(3).demande(demande)
                .service(Service.builder().id(4).nomService("Déclarations fiscales et sociales").build())
                .facture(proforma)
                .build();
        proforma.getLignes().add(ligne);
        FactureValidee fv = FactureValidee.builder().id(9)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .factureProforma(proforma)
                .demande(demande)
                .montant(new BigDecimal("2000000.00"))
                .build();
        Paiement paiement = Paiement.builder().id(20).referencePaiement("PAY-2026-00020")
                .demande(demande)
                .factureValidee(fv)
                .modePaiement(ModePaiement.builder().id(1).code("ESPECES")
                        .libelle("Espèces").actif(true).build())
                .datePaiement(LocalDate.of(2026, 10, 22))
                .montantPaye(new BigDecimal("1300000.00"))
                .createdBy(Utilisateur.builder().id(1).prenom("Système").nom("Administrateur").build())
                .build();

        PaiementDetailResponse detail = PaiementMapper.toDetailResponse(paiement,
                new BigDecimal("1300000.00"));

        assertEquals("PAY-2026-00020", detail.referencePaiement());
        assertEquals("DEM-2026-00018", detail.referenceDemande());
        assertEquals("Beta SA", detail.nomSociete());
        assertEquals("Déclarations fiscales et sociales", detail.nomService());
        assertEquals("00001/EXT/TNR/2026", detail.referenceFactureValidee());
        assertEquals(new BigDecimal("2000000.00"), detail.montantFactureValidee());
        assertEquals(new BigDecimal("1300000.00"), detail.montantPayeSurFacture());
        assertEquals(new BigDecimal("700000.00"), detail.resteAPayerFacture());
        assertEquals("un million trois cent mille", detail.montantEnLettres());
        assertEquals("Système Administrateur", detail.createdByName());
    }

    @Test
    void paiementSansFactureValideeNaPasDeMontants() {
        DemandeService demande = DemandeService.builder().id(18)
                .referenceDemande("DEM-2026-00018").build();
        Paiement paiement = Paiement.builder().id(21).referencePaiement("PAY-2026-00021")
                .demande(demande)
                .modePaiement(ModePaiement.builder().id(1).libelle("Espèces").build())
                .datePaiement(LocalDate.of(2026, 10, 22))
                .montantPaye(new BigDecimal("50000.00"))
                .build();

        PaiementDetailResponse detail = PaiementMapper.toDetailResponse(paiement,
                BigDecimal.ZERO);

        assertNull(detail.factureValideeId());
        assertNull(detail.referenceFactureValidee());
        assertNull(detail.nomService());
        assertNull(detail.montantFactureValidee());
        assertNull(detail.resteAPayerFacture());
        assertEquals(new BigDecimal("50000.00"), detail.montantPaye());
    }
}
