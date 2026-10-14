package com.cabex.backend.dossier;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.dto.AssocieRequest;
import com.cabex.backend.dossier.dto.CreateDossierRequest;
import com.cabex.backend.dossier.dto.UpdateDossierRequest;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.dossier.repository.DossierCreationRepository;
import com.cabex.backend.dossier.service.DossierService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    @Mock
    private DossierCreationRepository dossierRepository;
    @Mock
    private DemandeServiceRepository demandeRepository;
    @Mock
    private AuditService auditService;

    private DossierService dossierService;

    @BeforeEach
    void setUp() {
        dossierService = new DossierService(dossierRepository, demandeRepository, auditService);
    }

    private DemandeService demandeCreation() {
        Service service = Service.builder()
                .id(11)
                .nomService("Création d'entreprise")
                
                .actif(true)
                .build();
        Commercial commercial = Commercial.builder()
                .id(1)
                .nomCommercial("Jean Dupont")
                .immatricule("C001")
                .actif(true)
                .build();
        return DemandeService.builder()
                .id(126)
                .referenceDemande("REF-2026-126")
                .dateDemande(LocalDate.of(2026, 9, 1))
                .debutService(LocalDate.of(2026, 9, 1))
                .finService(LocalDate.of(2027, 2, 28))
                .societe(null)
                .commercial(commercial)
                .service(service)
                .tarif(new BigDecimal("700000"))
                .actif(true)
                .build();
    }

    private CreateDossierRequest requeteSarl() {
        return new CreateDossierRequest(
                "SARL", null, "Sahan Agro SARL", "Lot II M 45 Ankorondrano",
                new BigDecimal("5000000"), 2, "Nirina Rasoanaivo", "Gérante", false,
                "Agroalimentaire", null, "Lot II M 45", "Antananarivo", "Antananarivo I",
                "Antananarivo I", "Analamanga", "LOCATION_CABINET", null, true, "Bail 3/6/9",
                "Transformation et négoce", LocalDate.of(2026, 11, 1), "PERMANENTE",
                "Siège Ankorondrano", null, "Assujettie", "Non", "Mariée", "Réel",
                List.of(new AssocieRequest("Nirina Rasoanaivo", "60 %", "nirina.r@sahanagro.mg", "+261 34 90 111 22")));
    }

    private UpdateDossierRequest requeteUpdate() {
        var c = requeteSarl();
        return new UpdateDossierRequest(
                c.formeJuridique(), c.formeJuridiquePrecision(), c.denomination(),
                c.siegeSocialActuel(), c.capitalSocial(), c.nombreAssociesDeclare(),
                c.nomGerant(), c.fonctionGerant(), c.locationGerance(),
                c.secteurActivite(), c.autorisationSpeciale(), c.adresseSiege(),
                c.ville(), c.commune(), c.district(), c.region(),
                c.typeHebergement(), c.hebergementPrecision(), c.contratBail(),
                c.contratBailDetail(), c.objetSocial(), c.dateSouhaiteeDebut(),
                c.regimeActivite(), c.activitesEtablissement(), c.observations(),
                c.assujettissementTva(), c.autreSociete(), c.situationMatrimoniale(),
                c.regimeImposition(), c.associes());
    }

    @Test
    void createOkAvecAssociesEtChampsDerives() {
        DemandeService demande = demandeCreation();
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        when(dossierRepository.save(any())).thenAnswer(inv -> {
            DossierCreation d = inv.getArgument(0);
            d.setId(1);
            return d;
        });

        var reponse = dossierService.create(126, requeteSarl());

        assertEquals(1, reponse.id());
        assertEquals(126, reponse.demandeId());
        assertEquals("REF-2026-126", reponse.referenceDemande());
        assertEquals(LocalDate.of(2026, 9, 1), reponse.dateDemande());
        assertEquals("C001", reponse.numeroIm());
        assertEquals(1, reponse.associes().size());
        assertEquals("Nirina Rasoanaivo", reponse.associes().get(0).nom());
        verify(auditService).log(eq("CREATE"), eq("DOSSIER"), eq("DossierCreation"),
                eq(1L), isNull(), contains("REF-2026-126"));
    }

    @Test
    void createDemandeInexistante404() {
        when(demandeRepository.findById(999)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> dossierService.create(999, requeteSarl()));
        assertEquals(ErrorMessages.introuvable("Demande", 999), ex.getMessage());
    }

    @Test
    void createServiceNonCreation400() {
        DemandeService demande = demandeCreation();
        demande.getService().setNomService("Audit comptable");
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requeteSarl()));
        assertEquals(ErrorMessages.DOSSIER_SERVICE_NON_CREATION, ex.getMessage());
    }

    @Test
    void createDossierExistant409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> dossierService.create(126, requeteSarl()));
        assertEquals(ErrorMessages.DOSSIER_DEJA_EXISTANT, ex.getMessage());
    }

    @Test
    void createFormeAutreSansPrecision400() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        var requete = new CreateDossierRequest(
                "AUTRE", " ", "X", null, null, null, null, null, false,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(new AssocieRequest("A", null, null, null)));

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_PRECISION_REQUISE, ex.getMessage());
    }

    @Test
    void createSocieteSansAssocies400() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        var requete = new CreateDossierRequest(
                "SARL", null, "X", null, null, 0, null, null, false,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_ASSOCIES_REQUIS, ex.getMessage());
    }

    @Test
    void createIndividuelleSansAssociesOk() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        when(dossierRepository.save(any())).thenAnswer(inv -> {
            DossierCreation d = inv.getArgument(0);
            d.setId(2);
            return d;
        });
        var requete = new CreateDossierRequest(
                "INDIVIDUELLE", null, "Epicerie Nirina", null, null, null, "Nirina R", null, false,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of());

        var reponse = dossierService.create(126, requete);

        assertEquals("INDIVIDUELLE", reponse.formeJuridique());
        assertTrue(reponse.associes().isEmpty());
    }

    @Test
    void updateRemplaceAssocies() {
        DemandeService demande = demandeCreation();
        DossierCreation existant = DossierCreation.builder().id(1).demande(demande).build();
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));
        when(dossierRepository.findByDemandeId(126)).thenReturn(Optional.of(existant));
        when(dossierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var requete = new UpdateDossierRequest(
                "SARLU", null, "Sahan Agro SARLU", null, null, 1, null, null, false,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(
                        new AssocieRequest("Nirina Rasoanaivo", "100 %", null, null),
                        new AssocieRequest("   ", null, null, null)));

        var reponse = dossierService.update(126, requete);

        assertEquals("SARLU", reponse.formeJuridique());
        assertEquals("Sahan Agro SARLU", reponse.denomination());
        assertEquals(1, reponse.associes().size());
        assertEquals(0, reponse.associes().get(0).ordre());
        verify(auditService).log(eq("UPDATE"), eq("DOSSIER"), eq("DossierCreation"),
                eq(1L), anyString(), anyString());
    }

    @Test
    void getEtUpdateDossierManquant404() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.findByDemandeId(126)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dossierService.getByDemandeId(126));
        assertThrows(ResourceNotFoundException.class,
                () -> dossierService.update(126, requeteUpdate()));
    }

    @Test
    void trimEtNormalisation() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeCreation()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        ArgumentCaptor<DossierCreation> captor = ArgumentCaptor.forClass(DossierCreation.class);
        when(dossierRepository.save(captor.capture())).thenAnswer(inv -> {
            DossierCreation d = inv.getArgument(0);
            d.setId(3);
            return d;
        });
        var requete = requeteSarl();

        dossierService.create(126, requete);

        DossierCreation sauvegarde = captor.getValue();
        assertEquals("SARL", sauvegarde.getFormeJuridique());
        assertEquals("Sahan Agro SARL", sauvegarde.getDenomination());
        assertEquals("LOCATION_CABINET", sauvegarde.getTypeHebergement());
        assertEquals("PERMANENTE", sauvegarde.getRegimeActivite());
    }
}
