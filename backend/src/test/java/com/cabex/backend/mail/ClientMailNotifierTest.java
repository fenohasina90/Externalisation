package com.cabex.backend.mail;

import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.document.service.DocumentGestionService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureProformaLigne;
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
    void proformasEnvoieRecapitulatifEtRemplaceLeDeboursParSaFactureValidee() {
        DemandeService demande = demande("responsable@cabex.mg", "societe@cabex.mg");
        Facture proformaService = Facture.builder().id(101)
                .referenceFacture("PRO-01012026-00001").modeFacturation("PAR_SERVICE")
                .demande(demande).build();
        Facture proformaDebours = Facture.builder().id(102)
                .referenceFacture("PRO-01012026-00002").modeFacturation("PAR_SERVICE")
                .demande(demande)
                .lignesProforma(List.of(FactureProformaLigne.builder().categorie("DEBOURS").build()))
                .build();
        when(factureRepository.findById(101)).thenReturn(Optional.of(proformaService));
        when(factureRepository.findByDemandeId(10)).thenReturn(List.of(proformaService, proformaDebours));
        FactureValidee debours = FactureValidee.builder().id(60)
                .referenceFactureValidee("00002EXT/TNR/2026").demande(demande).build();
        when(factureValideeRepository.findByDemandeIdOrderByIdAsc(10)).thenReturn(List.of(debours));
        when(documentService.recapitulatifDemande(10)).thenReturn("recap".getBytes(StandardCharsets.UTF_8));
        when(documentService.factureProforma(101)).thenReturn("p1".getBytes(StandardCharsets.UTF_8));
        when(documentService.factureValideeDocument(60)).thenReturn("debours".getBytes(StandardCharsets.UTF_8));
        when(mailService.envoyerDocumentsClient(
                anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(true);

        notifier.envoyerProformas(101);

        ArgumentCaptor<List<PieceJointeEmail>> pieces = ArgumentCaptor.forClass(List.class);
        verify(mailService).envoyerDocumentsClient(
                eq("societe@cabex.mg"), eq("responsable@cabex.mg"), anyString(), anyString(),
                pieces.capture());
        assertEquals(3, pieces.getValue().size());
        assertTrue(pieces.getValue().get(0).nomFichier().startsWith("recapitulatif-"));
        assertTrue(pieces.getValue().get(1).nomFichier().contains("PRO-01012026-00001"));
        assertTrue(pieces.getValue().get(2).nomFichier().contains("00002EXT-TNR-2026"));
        verify(documentService, never()).factureProforma(102);
    }

    @Test
    void sansAucuneAdresseErreurVisibleEtAucunEnvoi() {
        Facture facture = Facture.builder().id(101).referenceFacture("PRO-01012026-00001")
                .modeFacturation("UNIQUE").demande(demande(null, null)).build();
        when(factureRepository.findById(101)).thenReturn(Optional.of(facture));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notifier.envoyerProformas(101));

        assertTrue(ex.getMessage().contains("adresse e-mail"));
        verifyNoInteractions(mailService);
    }

    @Test
    void societeSansEmailEnvoieAuResponsableCommeDestinatairePrincipal() {
        Facture facture = Facture.builder().id(101).referenceFacture("PRO-01012026-00001")
                .modeFacturation("UNIQUE").demande(demande("responsable@cabex.mg", null)).build();
        when(factureRepository.findById(101)).thenReturn(Optional.of(facture));
        when(documentService.recapitulatifDemande(10))
                .thenReturn("recap".getBytes(StandardCharsets.UTF_8));
        when(documentService.factureProforma(101))
                .thenReturn("p1".getBytes(StandardCharsets.UTF_8));
        when(mailService.envoyerDocumentsClient(
                anyString(), nullable(String.class), anyString(), anyString(), anyList()))
                .thenReturn(true);

        notifier.envoyerProformas(101);

        verify(mailService).envoyerDocumentsClient(
                eq("responsable@cabex.mg"), isNull(), anyString(), anyString(), anyList());
    }

    @Test
    void factureValideeEnvoieUniquementLaFactureConcernee() {
        DemandeService demande = demande("responsable@cabex.mg", "societe@cabex.mg");
        FactureValidee facture = FactureValidee.builder().id(55)
                .referenceFactureValidee("00001EXT/TNR/2026").demande(demande).build();
        when(factureValideeRepository.findById(55)).thenReturn(Optional.of(facture));
        byte[] pdf = "facture".getBytes(StandardCharsets.UTF_8);
        when(documentService.factureValideeDocument(55)).thenReturn(pdf);
        when(mailService.envoyerDocumentsClient(
                anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(true);

        notifier.envoyerFactureValidee(55);

        verify(mailService).envoyerDocumentsClient(
                eq("societe@cabex.mg"),
                eq("responsable@cabex.mg"),
                contains("00001EXT/TNR/2026"),
                anyString(),
                argThat(pieces -> pieces.size() == 1
                        && pieces.get(0).nomFichier().contains("00001EXT-TNR-2026")));
    }

    @Test
    void echecSmtpRemonteUneErreur() {
        DemandeService demande = demande("responsable@cabex.mg", null);
        FactureValidee facture = FactureValidee.builder().id(55)
                .referenceFactureValidee("00001EXT/TNR/2026").demande(demande).build();
        when(factureValideeRepository.findById(55)).thenReturn(Optional.of(facture));
        when(documentService.factureValideeDocument(55))
                .thenReturn("facture".getBytes(StandardCharsets.UTF_8));
        when(mailService.envoyerDocumentsClient(
                anyString(), nullable(String.class), anyString(), anyString(), anyList()))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notifier.envoyerFactureValidee(55));

        assertTrue(ex.getMessage().contains("n'a pas pu être envoyé"));
    }
}
