package com.cabex.backend.client;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.dto.CreateSocieteRequest;
import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.client.service.RisqueService;
import com.cabex.backend.client.service.SocieteService;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.document.repository.PieceJointeRepository;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Liste (archive, responsable principal), création et archivage.
 */
@ExtendWith(MockitoExtension.class)
class SocieteServiceTest {

    @Mock private SocieteRepository societeRepository;
    @Mock private ClientResponsableRepository responsableRepository;
    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private EcheancePaiementRepository echeanceRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private DomaineServiceRepository domaineRepository;
    @Mock private RisqueService risqueService;
    @Mock private AuditService auditService;
    @Mock private ConfigurationService configurationService;

    private SocieteService societeService;

    @BeforeEach
    void setUp() {
        societeService = new SocieteService(
                societeRepository, responsableRepository, demandeRepository,
                historiqueRepository, paiementRepository, echeanceRepository,
                pieceJointeRepository, domaineRepository, risqueService, auditService,
                configurationService);
    }

    private Societe societe(Integer id, String nom, boolean actif, boolean archivee) {
        Societe s = Societe.builder().id(id).nomSociete(nom).actif(actif).build();
        if (archivee) {
            s.setArchivedAt(java.time.LocalDateTime.now());
        }
        return s;
    }

    @Test
    void listeExposeArchiveEtResponsable() {
        Societe atlas = societe(1, "Atlas Conseil", true, false);
        when(societeRepository.rechercher(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(atlas)));
        when(societeRepository.compterDemandesParSociete()).thenReturn(List.of());
        when(responsableRepository.findBySocieteIdInOrderByIdAsc(List.of(1))).thenReturn(List.of(
                ClientResponsable.builder().id(1).societe(atlas).nomClient("Amina Neme").actif(true).build()));

        var page = societeService.getAll(null, null, null, null, PageRequest.of(0, 8));

        assertEquals(1, page.getTotalElements());
        assertFalse(page.getContent().get(0).archive());
        assertEquals("Amina Neme", page.getContent().get(0).responsablePrincipal());
        assertEquals(0, page.getContent().get(0).nombreDemandes());
    }

    @Test
    void filtreArchiveTransmis() {
        when(societeRepository.rechercher(isNull(), isNull(), isNull(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(societeRepository.compterDemandesParSociete()).thenReturn(List.of());

        societeService.getAll(null, null, null, true, PageRequest.of(0, 8));

        verify(societeRepository).rechercher(isNull(), isNull(), isNull(), eq(true), any());
    }

    @Test
    void createNomDuplique409() {
        when(societeRepository.existsByNomSociete("Atlas Conseil")).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> societeService.create(new CreateSocieteRequest(
                        "Atlas Conseil", null, null, null, null, null, null, null, null, 1)));
        assertEquals(ErrorMessages.referenceDejaUtilisee("Atlas Conseil"), ex.getMessage());
        verify(societeRepository, never()).save(any());
    }

    @Test
    void archiveEtGetById() {
        Societe atlas = societe(1, "Atlas Conseil", true, false);
        when(societeRepository.findById(1)).thenReturn(Optional.of(atlas));
        when(societeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = societeService.archive(1, true);

        assertTrue(reponse.archive());
        assertFalse(reponse.actif());
        verify(auditService).log(eq("ARCHIVE"), eq("CLIENT"), eq("Societe"),
                eq(1L), anyString(), anyString());
    }

    @Test
    void getDetailInexistant404() {
        when(societeRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> societeService.getDetail(999));
    }
}
