package com.cabex.backend.mail;

import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.document.service.DocumentGestionService;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.mail.MailService.PieceJointeEmail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientMailNotifierTest {

    @Mock
    private MailService mailService;
    @Mock
    private DocumentGestionService documentService;
    @Mock
    private DemandeServiceRepository demandeRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private FactureValideeRepository factureValideeRepository;

    @InjectMocks
    private ClientMailNotifier notifier;

    private DemandeService demande(String emailResponsable, String emailSociete) {
        Societe societe = Societe.builder().id(1).nomSociete("TechPro").email(emailSociete).build();
        ClientResponsable responsable = emailResponsable == null ? null
                : ClientResponsable.builder().id(2).nomClient("Andry").email(emailResponsable).build();
        return DemandeService.builder()
                .id(10)
                .referenceDemande("DEM-01012026EXT00001")
                .societe(societe)
                .clientResponsable(responsable)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void creationEnvoieRecapitulatifEtToutesLesProformasAuResponsable() {
        DemandeService demande = demande("responsable@cabex.mg", "societe@cabex.mg");
        when(demandeRepository.findById(10)).thenReturn(Optional.of(demande));
        byte[] recap = "recap".getBytes(StandardCharsets.UTF_8);
        when(documentService.recapitulatifDemande(10)).thenReturn(recap);
        Facture f1 = Facture.builder().id(101).referenceFacture("PRO-01012026-00001").build();
        Facture f2 = Facture.builder().id(102).referenceFacture("PRO-01012026-00002").build();
        when(factureRepository.findByDemandeId(10)).thenReturn(List.of(f1, f2));
        when(documentService.factureProforma(101)).thenReturn("p1".getBytes(StandardCharsets.UTF_8));
        when(documentService.factureProforma(102)).thenReturn("p2".getBytes(StandardCharsets.UTF_8));
        when(mailService.envoyerDemandeClient(anyString(), anyString(), any(), anyList())).thenReturn(true);

        notifier.notifierDemandeCreee(10);

        ArgumentCaptor<List<PieceJointeEmail>> pieces = ArgumentCaptor.forClass(List.class);
        verify(mailService).envoyerDemandeClient(
                eq("responsable@cabex.mg"),
                eq("DEM-01012026EXT00001"),
                same(recap),
                pieces.capture());
        // Le récapitulatif est ajouté par MailService ; ici on vérifie les 2 proformas.
        assertEquals(2, pieces.getValue().size());
        assertTrue(pieces.getValue().get(0).nomFichier().contains("PRO-01012026-00001"));
        assertTrue(pieces.getValue().get(1).nomFichier().contains("PRO-01012026-00002"));
    }

    @Test
    void sansAdresseClientAucunEnvoi() {
        when(demandeRepository.findById(10)).thenReturn(Optional.of(demande(null, null)));

        notifier.notifierDemandeCreee(10);

        verifyNoInteractions(mailService, documentService, factureRepository);
    }

    @Test
    void validationEnvoieLaFactureValideeEnPdf() {
        DemandeService demande = demande("responsable@cabex.mg", null);
        FactureValidee facture = FactureValidee.builder()
                .id(55)
                .referenceFactureValidee("00001EXT/TNR/2026")
                .demande(demande)
                .build();
        when(factureValideeRepository.findById(55)).thenReturn(Optional.of(facture));
        byte[] pdf = "facture".getBytes(StandardCharsets.UTF_8);
        when(documentService.factureValideeDocument(55)).thenReturn(pdf);
        when(mailService.envoyerFactureValideeClient(
                anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() -> notifier.notifierFactureValidee(55));

        verify(mailService).envoyerFactureValideeClient(
                "responsable@cabex.mg",
                "DEM-01012026EXT00001",
                "00001EXT/TNR/2026",
                pdf);
    }

    @Test
    void echecSmtpNestPasBloquant() {
        when(demandeRepository.findById(10)).thenReturn(Optional.of(demande(null, "societe@cabex.mg")));
        when(documentService.recapitulatifDemande(10))
                .thenReturn("recap".getBytes(StandardCharsets.UTF_8));
        when(factureRepository.findByDemandeId(10)).thenReturn(List.of());
        when(mailService.envoyerDemandeClient(anyString(), anyString(), any(), anyList()))
                .thenReturn(false);

        assertDoesNotThrow(() -> notifier.notifierDemandeCreee(10));

        verify(mailService).envoyerDemandeClient(
                eq("societe@cabex.mg"),
                eq("DEM-01012026EXT00001"),
                any(),
                anyList());
    }
}
