package com.cabex.backend.audit;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.client.service.ResponsableService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.document.entity.PieceJointe;
import com.cabex.backend.document.repository.PieceJointeRepository;
import com.cabex.backend.document.service.DocumentGestionService;
import com.cabex.backend.document.service.PdfGenerator;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.echeance.service.EcheanceGestionService;
import com.cabex.backend.echeance.service.PlanPaiementService;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import com.cabex.backend.client.dto.CreateResponsableRequest;
import com.cabex.backend.document.dto.DocumentResponse;
import com.cabex.backend.echeance.dto.CreateEcheanceRequest;
import com.cabex.backend.echeance.dto.CreatePlanPaiementRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Chaque action métier (sans exception) enregistre une trace d'audit :
 * plans, échéances, documents, responsables.
 */
@ExtendWith(MockitoExtension.class)
class AuditJournalisationTest {

    @Mock private ProposPaiementRepository proposRepository;
    @Mock private EcheancePaiementRepository echeanceRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private com.cabex.backend.demande.repository.LigneServiceRepository ligneRepository;
    @Mock private com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository historiqueLigneRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RoleUtilisateurRepository roleRepository;
    @Mock private PieceJointeRepository pieceRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private ClientResponsableRepository responsableRepository;
    @Mock private SocieteRepository societeRepository;
    @Mock private PdfGenerator pdfGenerator;
    @Mock private AuditService auditService;
    @Mock private com.cabex.backend.configuration.service.ConfigurationService configurationService;
    @Mock private com.cabex.backend.facture.repository.FactureRepository factureRepository;

    @TempDir
    Path dossierTemporaire;

    private DemandeService demande() {
        DemandeService demande = DemandeService.builder()
                .id(10).referenceDemande("REF-2026-014")
                .dateDemande(LocalDate.of(2026, 9, 1))
                .actif(true)
                .build();
        LigneService ligne = LigneService.builder()
                .id(1).demande(demande)
                .tarif(new BigDecimal("1000000"))
                .debutService(LocalDate.of(2026, 9, 1))
                .finService(LocalDate.of(2027, 2, 28))
                .facture(com.cabex.backend.facture.entity.Facture.builder()
                        .id(1).statut("VALIDEE").build())
                .build();
        demande.setLignes(new java.util.HashSet<>(java.util.Set.of(ligne)));
        return demande;
    }

    private LigneService ligneDe(DemandeService demande) {
        return demande.getLignes().iterator().next();
    }

    @Test
    void creationPlanJournalisee() {
        DemandeService demande = demande();
        when(ligneRepository.findByDemandeId(10))
                .thenReturn(java.util.List.of(ligneDe(demande)));
        lenient().when(ligneRepository.findById(1))
                .thenReturn(Optional.of(ligneDe(demande)));
        when(proposRepository.existsByLigneServiceId(1)).thenReturn(false);
        when(proposRepository.save(any())).thenAnswer(inv -> {
            ProposPaiement p = inv.getArgument(0);
            p.setId(1);
            return p;
        });
        PlanPaiementService service = new PlanPaiementService(
                proposRepository, echeanceRepository, demandeRepository,
                historiqueRepository, ligneRepository, historiqueLigneRepository,
                utilisateurRepository, auditService);

        service.create(10, new CreatePlanPaiementRequest(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30),
                new BigDecimal("1000000"), 3, null, null));

        verify(auditService).log(eq("CREATE"), eq("PAIEMENT"), eq("PlanPaiement"),
                eq(1L), isNull(), contains("REF-2026-014"));
    }

    @Test
    void modificationPlanJournalisee() {
        DemandeService demande = demande();
        ProposPaiement propos = ProposPaiement.builder()
                .id(1).demande(demande).ligneService(ligneDe(demande))
                .dateDebut(LocalDate.of(2026, 9, 1)).dateFin(LocalDate.of(2026, 11, 30))
                .montantTotal(new BigDecimal("1000000")).restePayer(new BigDecimal("300000"))
                .nombreEcheances(3).statut("ACTIF").build();
        when(proposRepository.findById(1)).thenReturn(Optional.of(propos));
        when(proposRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PlanPaiementService service = new PlanPaiementService(
                proposRepository, echeanceRepository, demandeRepository,
                historiqueRepository, ligneRepository, historiqueLigneRepository,
                utilisateurRepository, auditService);

        service.update(1, new com.cabex.backend.echeance.dto.UpdatePlanPaiementRequest(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30),
                new BigDecimal("1000000"), null));

        verify(auditService).log(eq("UPDATE"), eq("PAIEMENT"), eq("PlanPaiement"),
                eq(1L), anyString(), anyString());
    }

    @Test
    void creationEcheanceJournalisee() {
        DemandeService demande = demande();
        ProposPaiement propos = ProposPaiement.builder()
                .id(1).demande(demande)
                .dateDebut(LocalDate.of(2026, 9, 1)).dateFin(LocalDate.of(2026, 11, 30))
                .montantTotal(new BigDecimal("1000000")).build();
        when(proposRepository.findById(1)).thenReturn(Optional.of(propos));
        when(echeanceRepository.existsByProposPaiementIdAndNumeroEcheance(1, 1)).thenReturn(false);
        when(echeanceRepository.save(any())).thenAnswer(inv -> {
            EcheancePaiement e = inv.getArgument(0);
            e.setId(5);
            return e;
        });
        EcheanceGestionService service = new EcheanceGestionService(
                echeanceRepository, proposRepository, auditService);

        service.create(1, new CreateEcheanceRequest(1,
                LocalDate.of(2026, 10, 10), new BigDecimal("300000"), null));

        verify(auditService).log(eq("CREATE"), eq("PAIEMENT"), eq("EcheancePaiement"),
                eq(5L), isNull(), contains("echeance=1"));
    }

    @Test
    void televersementEtSuppressionJournalises() {
        DemandeService demande = demande();
        when(demandeRepository.findById(10)).thenReturn(Optional.of(demande));
        when(pieceRepository.save(any())).thenAnswer(inv -> {
            PieceJointe p = inv.getArgument(0);
            p.setId(7);
            return p;
        });
        DocumentGestionService service = new DocumentGestionService(
                pieceRepository, demandeRepository, paiementRepository, proposRepository,
                utilisateurRepository, pdfGenerator, auditService, configurationService,
                factureRepository);
        ReflectionTestUtils.setField(service, "storagePath", dossierTemporaire.toString());
        ReflectionTestUtils.setField(service, "maxSize", 10 * 1024 * 1024L);
        lenient().when(configurationService.lireEntierLong(anyString(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(1));

        MockMultipartFile fichier = new MockMultipartFile(
                "fichier", "contrat.pdf", "application/pdf", "contenu".getBytes());
        DocumentResponse envoye = service.upload(10, fichier, null);
        assertNotNull(envoye);

        verify(auditService).log(eq("CREATE"), eq("DOCUMENT"), eq("PieceJointe"),
                eq(7L), isNull(), contains("contrat.pdf"));

        PieceJointe piece = PieceJointe.builder().id(7)
                .demande(demande).nomFichier("x.pdf").nomOriginal("contrat.pdf")
                .cheminFichier(dossierTemporaire.resolve("x.pdf").toString()).build();
        when(pieceRepository.findById(7)).thenReturn(Optional.of(piece));

        service.delete(7);

        verify(auditService).log(eq("DELETE"), eq("DOCUMENT"), eq("PieceJointe"),
                eq(7L), contains("contrat.pdf"), isNull());
    }

    @Test
    void crudResponsableJournalise() {
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").actif(true).build();
        when(societeRepository.findById(1)).thenReturn(Optional.of(societe));
        when(responsableRepository.save(any())).thenAnswer(inv -> {
            ClientResponsable r = inv.getArgument(0);
            r.setId(3);
            return r;
        });
        ClientResponsable existant = ClientResponsable.builder()
                .id(3).societe(societe).nomClient("Amina Neme").actif(true).build();
        when(responsableRepository.findById(3)).thenReturn(Optional.of(existant));
        ResponsableService service = new ResponsableService(
                responsableRepository, societeRepository, auditService);

        service.create(new CreateResponsableRequest(1, "Amina Neme", null, null, null));
        verify(auditService).log(eq("CREATE"), eq("CLIENT"), eq("ClientResponsable"),
                eq(3L), isNull(), contains("Amina Neme"));

        service.toggleActivation(3, false);
        verify(auditService).log(eq("UPDATE"), eq("CLIENT"), eq("ClientResponsable"),
                eq(3L), contains("true"), contains("false"));

        service.delete(3);
        verify(auditService).log(eq("DELETE"), eq("CLIENT"), eq("ClientResponsable"),
                eq(3L), anyString(), isNull());
    }
}
