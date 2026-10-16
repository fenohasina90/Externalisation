package com.cabex.backend.demande;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.demande.dto.CreateDemandeRequest;
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Création de demande avec societe_id NULL (création d'entreprise).
 */
@ExtendWith(MockitoExtension.class)
class DemandeSansSocieteTest {

    @Mock private DemandeServiceRepository demandeRepository;
    @Mock private SocieteRepository societeRepository;
    @Mock private ClientResponsableRepository responsableRepository;
    @Mock private CommercialRepository commercialRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private HistoriqueDemandeRepository historiqueRepository;
    @Mock private StatutDemandeRepository statutRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private com.cabex.backend.demande.service.LigneServiceService ligneServiceService;
    @Mock private com.cabex.backend.facture.service.FactureService factureService;
    @Mock private com.cabex.backend.demande.repository.LigneServiceRepository ligneRepository;
    @Mock private com.cabex.backend.echeance.repository.ProposPaiementRepository proposRepository;

    private DemandeGestionService service;

    @BeforeEach
    void setUp() {
        service = new DemandeGestionService(
                demandeRepository, societeRepository, responsableRepository,
                commercialRepository, serviceRepository, historiqueRepository,
                statutRepository, paiementRepository, utilisateurRepository,
                notificationService, auditService, ligneServiceService,
                factureService, ligneRepository, proposRepository);
        lenient().when(statutRepository.findByStatut("EN_ATTENTE"))
                .thenReturn(Optional.of(StatutDemande.builder().id(1).statut("EN_ATTENTE").build()));
        lenient().when(demandeRepository.countByReferenceDemandeStartingWith(anyString())).thenReturn(0L);
        lenient().when(demandeRepository.existsByReferenceDemande(anyString())).thenReturn(false);
        lenient().when(demandeRepository.save(any())).thenAnswer(inv -> {
            var d = (com.cabex.backend.demande.entity.DemandeService) inv.getArgument(0);
            d.setId(1);
            return d;
        });
        lenient().when(ligneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ligneServiceService.montantTotalDemande(any()))
                .thenReturn(new BigDecimal("700000"));
    }

    private Service serviceCreation() {
        return Service.builder().id(11).nomService("Création d'entreprise")
                .typeService("CREATION")
                .actif(true).build();
    }

    private Service serviceAudit() {
        return Service.builder().id(1).nomService("Audit comptable")
                .typeService("STANDARD")
                .actif(true).build();
    }

    private CreateDemandeRequest requete(Integer societeId, Integer serviceId, Integer responsableId) {
        return new CreateDemandeRequest(
                LocalDate.of(2026, 9, 1), null, null,
                societeId, null, responsableId, null, null, null,
                java.util.List.of(new CreateLigneRequest(
                        serviceId, new BigDecimal("700000"),
                        LocalDate.of(2026, 9, 1), LocalDate.of(2027, 2, 28))),
                "PAR_SERVICE");
    }

    @Test
    void creationEntrepriseSansSocieteOk() {
        when(serviceRepository.findById(11)).thenReturn(Optional.of(serviceCreation()));

        var reponse = service.create(requete(null, 11, null));

        assertNotNull(reponse.referenceDemande());
        verify(societeRepository, never()).findById(any());
        verify(demandeRepository).save(argThat(d -> d.getSociete() == null));
    }

    @Test
    void serviceClassiqueSansSociete400() {
        when(serviceRepository.findById(1)).thenReturn(Optional.of(serviceAudit()));

        var ex = assertThrows(BusinessException.class,
                () -> service.create(requete(null, 1, null)));
        assertEquals(ErrorMessages.DEMANDE_SOCIETE_REQUISE, ex.getMessage());
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void responsableRattacheRefuseSansSociete400() {
        lenient().when(serviceRepository.findById(11)).thenReturn(Optional.of(serviceCreation()));
        com.cabex.backend.client.entity.ClientResponsable rattache =
                com.cabex.backend.client.entity.ClientResponsable.builder()
                        .id(5).nomClient("Amina Neme")
                        .societe(com.cabex.backend.client.entity.Societe.builder()
                                .id(9).nomSociete("Autre").build())
                        .build();
        when(responsableRepository.findById(5)).thenReturn(Optional.of(rattache));

        var ex = assertThrows(BusinessException.class,
                () -> service.create(requete(null, 11, 5)));
        assertEquals("Le responsable n'appartient pas à la société sélectionnée", ex.getMessage());
        verify(demandeRepository, never()).save(any());
    }

    @Test
    void responsableLibreAccepteSansSociete() {
        when(serviceRepository.findById(11)).thenReturn(Optional.of(serviceCreation()));
        com.cabex.backend.client.entity.ClientResponsable libre =
                com.cabex.backend.client.entity.ClientResponsable.builder()
                        .id(6).nomClient("Contact libre").societe(null).build();
        when(responsableRepository.findById(6)).thenReturn(Optional.of(libre));

        var reponse = service.create(requete(null, 11, 6));

        assertNotNull(reponse.referenceDemande());
        verify(demandeRepository).save(any());
    }

    @Test
    void serviceClassiqueAvecSocieteOk() {
        when(serviceRepository.findById(1)).thenReturn(Optional.of(serviceAudit()));
        Societe societe = Societe.builder().id(1).nomSociete("Atlas Conseil").actif(true).build();
        when(societeRepository.findById(1)).thenReturn(Optional.of(societe));

        var reponse = service.create(requete(1, 1, null));

        assertNotNull(reponse.referenceDemande());
        verify(demandeRepository).save(argThat(d ->
                d.getSociete() != null && d.getSociete().getId().equals(1)));
    }
}
