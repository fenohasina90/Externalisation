package com.cabex.backend.paiement;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.dto.CreatePaiementRequest;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.paiement.service.PaiementGestionService;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Paiements DEBOURS par ligne : sans échéance, plafond = montant de la ligne,
 * statut NON_PAYEE → PARTIEL → PAYEE recalculé après chaque paiement.
 */
@ExtendWith(MockitoExtension.class)
class PaiementDeboursTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private ModePaiementRepository modeRepository;
    @Mock private com.cabex.backend.echeance.repository.ProposPaiementRepository proposRepository;
    @Mock private FactureValideeRepository factureValideeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private DemandeGestionService demandeGestionService;
    @Mock private LigneServiceRepository ligneRepository;
    @Mock private LigneServiceService ligneServiceService;

    private PaiementGestionService service;

    @BeforeEach
    void setUp() {
        service = new PaiementGestionService(paiementRepository, demandeRepository,
                modeRepository, null, proposRepository, factureValideeRepository,
                utilisateurRepository, notificationService, auditService,
                demandeGestionService, ligneRepository, ligneServiceService);
        lenient().when(modeRepository.findById(1)).thenReturn(Optional.of(
                ModePaiement.builder().id(1).code("ESPECES").libelle("Espèces").build()));
        lenient().when(paiementRepository.save(any())).thenAnswer(inv -> {
            var p = (com.cabex.backend.paiement.entity.Paiement) inv.getArgument(0);
            p.setId(1);
            return p;
        });
        lenient().when(paiementRepository.count()).thenReturn(0L);
        lenient().when(paiementRepository.existsByReferencePaiement(anyString())).thenReturn(false);
        lenient().when(paiementRepository.sommePayeeParFactureValidee(anyInt()))
                .thenReturn(BigDecimal.ZERO);
        DemandeService d = DemandeService.builder().id(17).referenceDemande("DEM-2026-00001").build();
        lenient().when(factureValideeRepository.findById(1)).thenReturn(Optional.of(
            FactureValidee.builder().id(1).referenceFactureValidee("00001/EXT/TNR/2026")
                .demande(d).montant(new BigDecimal("500000")).build()));
    }

    private LigneService debours(Integer ligneId, String statut, BigDecimal montant) {
        DemandeService d = DemandeService.builder().id(17).referenceDemande("DEM-2026-00001").build();
        LigneService l = LigneService.builder().id(ligneId).demande(d)
                .categorie("DEBOURS").statut(statut).montantDebours(montant).build();
        lenient().when(demandeRepository.findById(17)).thenReturn(Optional.of(d));
        lenient().when(ligneRepository.findById(ligneId)).thenReturn(Optional.of(l));
        return l;
    }

    private FactureValidee factureValidee(Integer id, LigneService ligne, BigDecimal montant) {
        FactureValidee fv = FactureValidee.builder().id(id)
                .referenceFactureValidee("00001/EXT/TNR/2026")
                .demande(ligne.getDemande())
                .montant(montant).build();
        lenient().when(factureValideeRepository.findById(id)).thenReturn(Optional.of(fv));
        return fv;
    }

    private CreatePaiementRequest paiement(BigDecimal montant, Integer factureValideeId) {
        return new CreatePaiementRequest(17, factureValideeId, null, 1,
                LocalDate.of(2026, 9, 10), montant, null, null, null, null, null);
    }

    @Test
    void paiementDeboursPartielOk() {
        LigneService l = debours(5, "NON_PAYEE", new BigDecimal("500000"));
        FactureValidee fv = factureValidee(1, l, new BigDecimal("500000"));
        lenient().when(ligneServiceService.montantPayeParLigne(5))
                .thenReturn(new BigDecimal("200000"));
        lenient().when(ligneRepository.findByDemandeId(17))
                .thenReturn(List.of(l));

        var reponse = service.create(paiement(new BigDecimal("200000"), 1));

        assertNotNull(reponse);
        verify(ligneServiceService).actualiserStatutApresPaiement(any());
    }

    @Test
    void paiementDeboursMontantInvalide400() {
        debours(5, "EN_ATTENTE", null);

        var ex = assertThrows(BusinessException.class,
                () -> service.create(paiement(new BigDecimal("1000"), 1)));
        assertEquals(ErrorMessages.DEBOURS_MONTANT_NON_VALIDE, ex.getMessage());
    }

    @Test
    void paiementDeboursDepassementRefuse400() {
        LigneService l = debours(5, "PARTIEL", new BigDecimal("500000"));
        lenient().when(ligneServiceService.montantPayeParLigne(5))
                .thenReturn(new BigDecimal("400000"));
        lenient().when(paiementRepository.sommePayeeParFactureValidee(1))
                .thenReturn(new BigDecimal("400000"));
        lenient().when(ligneRepository.findByDemandeId(17))
                .thenReturn(List.of(l));

        var ex = assertThrows(BusinessException.class,
                () -> service.create(paiement(new BigDecimal("200000"), 1)));
        assertTrue(ex.getMessage().contains("reste à payer"));
    }
}
