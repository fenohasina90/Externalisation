package com.cabex.backend.paiement;

import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.mapper.PaiementMapper;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Le détail d'un paiement reprend les informations affichées sur le reçu PDF.
 */
class PaiementMapperTest {

    @Test
    void detailReprendLesInformationsDuRecu() {
        Societe societe = Societe.builder().id(5).nomSociete("Beta SA").build();
        DemandeService demande = DemandeService.builder().id(18)
                .referenceDemande("DEM-2026-00018").societe(societe).build();
        LigneService ligne = LigneService.builder().id(3).demande(demande)
                .service(Service.builder().id(4).nomService("Déclarations fiscales et sociales").build())
                .build();
        ProposPaiement plan = ProposPaiement.builder().id(7).demande(demande).ligneService(ligne).build();
        EcheancePaiement echeance = EcheancePaiement.builder().id(9).proposPaiement(plan)
                .numeroEcheance(1)
                .dateEcheance(LocalDate.of(2026, 10, 30))
                .montantAttendu(new BigDecimal("2000000.00"))
                .montantPaye(new BigDecimal("1300000.00"))
                .build();
        Paiement paiement = Paiement.builder().id(20).referencePaiement("PAY-2026-00020")
                .demande(demande).echeance(echeance)
                .modePaiement(ModePaiement.builder().id(1).code("ESPECES")
                        .libelle("Espèces").actif(true).build())
                .datePaiement(LocalDate.of(2026, 10, 22))
                .montantPaye(new BigDecimal("1300000.00"))
                .createdBy(Utilisateur.builder().id(1).prenom("Système").nom("Administrateur").build())
                .build();

        PaiementDetailResponse detail = PaiementMapper.toDetailResponse(paiement);

        assertEquals("PAY-2026-00020", detail.referencePaiement());
        assertEquals("DEM-2026-00018", detail.referenceDemande());
        assertEquals("Beta SA", detail.nomSociete());
        assertEquals("Déclarations fiscales et sociales", detail.nomService());
        assertEquals(1, detail.numeroEcheance());
        assertEquals(LocalDate.of(2026, 10, 30), detail.dateEcheance());
        assertEquals(new BigDecimal("2000000.00"), detail.montantAttendu());
        assertEquals(new BigDecimal("1300000.00"), detail.montantPayeEcheance());
        assertEquals(new BigDecimal("700000.00"), detail.resteAPayer());
        assertEquals("un million trois cent mille", detail.montantEnLettres());
        assertEquals("Système Administrateur", detail.createdByName());
    }

    @Test
    void paiementSansEcheanceNaPasDeMontantsAttendus() {
        DemandeService demande = DemandeService.builder().id(18)
                .referenceDemande("DEM-2026-00018").build();
        Paiement paiement = Paiement.builder().id(21).referencePaiement("PAY-2026-00021")
                .demande(demande)
                .modePaiement(ModePaiement.builder().id(1).libelle("Espèces").build())
                .datePaiement(LocalDate.of(2026, 10, 22))
                .montantPaye(new BigDecimal("50000.00"))
                .build();

        PaiementDetailResponse detail = PaiementMapper.toDetailResponse(paiement);

        assertNull(detail.numeroEcheance());
        assertNull(detail.dateEcheance());
        assertNull(detail.nomService());
        assertNull(detail.montantAttendu());
        assertNull(detail.resteAPayer());
        assertEquals(new BigDecimal("50000.00"), detail.montantPayeEcheance());
    }
}
