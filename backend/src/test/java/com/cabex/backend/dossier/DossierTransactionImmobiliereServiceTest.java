package com.cabex.backend.dossier;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.dto.CreateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.dto.UpdateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.cabex.backend.dossier.repository.DossierTransactionImmobiliereRepository;
import com.cabex.backend.dossier.service.DossierTransactionImmobiliereService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierTransactionImmobiliereServiceTest {

    @Mock
    private DossierTransactionImmobiliereRepository dossierRepository;
    @Mock
    private DemandeServiceRepository demandeRepository;
    @Mock
    private AuditService auditService;

    private DossierTransactionImmobiliereService dossierService;

    @BeforeEach
    void setUp() {
        dossierService = new DossierTransactionImmobiliereService(
                dossierRepository, demandeRepository, auditService);
    }

    private DemandeService demandeTransaction() {
        Service service = Service.builder()
                .id(20)
                .nomService("Transaction immobilière")
                .typeService("TRANSACTION_IMMOBILIERE")
                .actif(true)
                .build();
        Commercial commercial = Commercial.builder()
                .id(1)
                .nomCommercial("Jean Dupont")
                .immatricule("C001")
                .actif(true)
                .build();
        Societe societe = Societe.builder()
                .id(9)
                .nomSociete("Rakoto Immobilier")
                .contact("+261 34 00 000 00")
                .email("contact@rakoto.mg")
                .fb("Rakoto Immobilier")
                .adresse("Lot II A 12 Antananarivo")
                .build();
        return DemandeService.builder()
                .id(126)
                .referenceDemande("REF-2026-126")
                .dateDemande(LocalDate.of(2026, 10, 1))
                .societe(societe)
                .commercial(commercial)
                .actif(true)
                .lignes(Set.of(LigneService.builder()
                        .id(1)
                        .service(service)
                        .tarif(new BigDecimal("500000"))
                        .debutService(LocalDate.of(2026, 10, 1))
                        .finService(LocalDate.of(2026, 12, 31))
                        .build()))
                .build();
    }

    private CreateDossierTransactionImmobiliereRequest requeteAchat() {
        return new CreateDossierTransactionImmobiliereRequest(
                "  Rakoto Pierre  ", true, false, false, false, false, false, null,
                "terrain", null, new BigDecimal("500.00"), "  Plan A  ", "  Axe nord  ",
                new BigDecimal("1000000.00"), false,
                null, null, null, null, null, null, null,
                "communal_domaine", "titre_borne",
                true, false, true, false, true,
                "  Observation importante  ", 2, new BigDecimal("50000.00"), true);
    }

    private UpdateDossierTransactionImmobiliereRequest requeteUpdate() {
        var c = requeteAchat();
        return new UpdateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(),
                false, true, false, false, false, false,
                null, "maison", null,
                null, null, null, null, null,
                new BigDecimal("250.00"), "Plan B", "Axe sud", true, true,
                new BigDecimal("2000000.00"), true,
                null, null, false, false, false, false, false,
                "Mise à jour", 1, new BigDecimal("75000.00"), false);
    }

    @Test
    void createOkAvecChampsDerivesEtNormalisation() {
        DemandeService demande = demandeTransaction();
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(false);
        when(dossierRepository.save(any())).thenAnswer(inv -> {
            DossierTransactionImmobiliere d = inv.getArgument(0);
            d.setId(1);
            return d;
        });

        var reponse = dossierService.create(126, requeteAchat());

        assertEquals(1, reponse.id());
        assertEquals("REF-2026-126", reponse.referenceDemande());
        assertEquals(LocalDate.of(2026, 10, 1), reponse.dateDemande());
        assertEquals("Jean Dupont", reponse.nomCommercial());
        assertEquals("C001", reponse.numeroIm());
        assertEquals("Rakoto Immobilier", reponse.nomSociete());
        assertEquals("+261 34 00 000 00", reponse.contactClient());
        assertEquals("contact@rakoto.mg", reponse.emailClient());
        assertEquals("Rakoto Immobilier", reponse.fbClient());
        assertEquals("Lot II A 12 Antananarivo", reponse.adresseClient());
        assertEquals("Rakoto Pierre", reponse.nomProprietairePrecedent());
        assertTrue(reponse.actionAchat());
        assertFalse(reponse.actionVente());
        assertEquals("TERRAIN", reponse.objet());
        assertEquals("Plan A", reponse.achatPosition());
        assertEquals("Axe nord", reponse.achatAxeEmplacement());
        assertEquals("COMMUNAL_DOMAINE", reponse.regularisationTypeTerrain());
        assertEquals("TITRE_BORNE", reponse.regularisationSituationTerrain());
        assertEquals("Observation importante", reponse.observations());
        assertTrue(reponse.prestationAbonnementMensuel());
        verify(auditService).log(eq("CREATE"), eq("DOSSIER"),
                eq("DossierTransactionImmobiliere"), eq(1L), isNull(),
                contains("REF-2026-126"));
    }

    @Test
    void createDemandeInexistante404() {
        when(demandeRepository.findById(999)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> dossierService.create(999, requeteAchat()));
        assertEquals(ErrorMessages.introuvable("Demande", 999), ex.getMessage());
    }

    @Test
    void createServiceNonTransaction409() {
        DemandeService demande = demandeTransaction();
        demande.getLignes().iterator().next().getService().setTypeService("STANDARD");
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requeteAchat()));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_SERVICE_NON_TRANSACTION, ex.getMessage());
    }

    @Test
    void createSansLigne409() {
        DemandeService demande = demandeTransaction();
        demande.setLignes(Set.of());
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requeteAchat()));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_SERVICE_NON_TRANSACTION, ex.getMessage());
    }

    @Test
    void createDossierExistant409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        when(dossierRepository.existsByDemandeId(126)).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> dossierService.create(126, requeteAchat()));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_DEJA_EXISTANT, ex.getMessage());
    }

    @Test
    void createSansAction409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();
        var requete = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), false, false, false, false, false, false, null,
                c.objet(), null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), c.regularisationTypeTerrain(),
                c.regularisationSituationTerrain(), c.documentPvReconnaissance(),
                c.documentNumeroDemandeAcquisition(), c.documentActeVentePriveeCommunale(),
                c.documentSituationJuridique(), c.documentActeNotoriete(), c.observations(),
                c.nombreCommerciauxTerrain(), c.commissionCn(), c.prestationAbonnementMensuel());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_ACTION_REQUISE, ex.getMessage());
    }

    @Test
    void createActionAutreSansPrecision409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();
        var requete = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), false, false, false, false, false, true, " ",
                c.objet(), null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), c.regularisationTypeTerrain(),
                c.regularisationSituationTerrain(), c.documentPvReconnaissance(),
                c.documentNumeroDemandeAcquisition(), c.documentActeVentePriveeCommunale(),
                c.documentSituationJuridique(), c.documentActeNotoriete(), c.observations(),
                c.nombreCommerciauxTerrain(), c.commissionCn(), c.prestationAbonnementMensuel());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_ACTION_AUTRE_PRECISION_REQUISE,
                ex.getMessage());
    }

    @Test
    void createObjetInvalide409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();
        var requete = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), true, false, false, false, false, false, null,
                "IMMEUBLE", null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), c.regularisationTypeTerrain(),
                c.regularisationSituationTerrain(), c.documentPvReconnaissance(),
                c.documentNumeroDemandeAcquisition(), c.documentActeVentePriveeCommunale(),
                c.documentSituationJuridique(), c.documentActeNotoriete(), c.observations(),
                c.nombreCommerciauxTerrain(), c.commissionCn(), c.prestationAbonnementMensuel());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_OBJET_INVALIDE, ex.getMessage());
    }

    @Test
    void createObjetAutreSansPrecision409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();
        var requete = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), true, false, false, false, false, false, null,
                "autre", "  ", c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), c.regularisationTypeTerrain(),
                c.regularisationSituationTerrain(), c.documentPvReconnaissance(),
                c.documentNumeroDemandeAcquisition(), c.documentActeVentePriveeCommunale(),
                c.documentSituationJuridique(), c.documentActeNotoriete(), c.observations(),
                c.nombreCommerciauxTerrain(), c.commissionCn(), c.prestationAbonnementMensuel());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_OBJET_AUTRE_PRECISION_REQUISE,
                ex.getMessage());
    }

    @Test
    void createTypeEtSituationTerrainInvalides409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();

        var mauvaisType = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), true, false, false, false, false, false, null,
                c.objet(), null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), "PUBLIC", c.regularisationSituationTerrain(),
                c.documentPvReconnaissance(), c.documentNumeroDemandeAcquisition(),
                c.documentActeVentePriveeCommunale(), c.documentSituationJuridique(),
                c.documentActeNotoriete(), c.observations(), c.nombreCommerciauxTerrain(),
                c.commissionCn(), c.prestationAbonnementMensuel());
        var exType = assertThrows(BusinessException.class,
                () -> dossierService.create(126, mauvaisType));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_TYPE_TERRAIN_INVALIDE,
                exType.getMessage());

        var mauvaiseSituation = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), true, false, false, false, false, false, null,
                c.objet(), null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                c.achatMontant(), c.achatPermisConstruire(), c.venteMesureM2(), c.ventePosition(),
                c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(), c.venteMontant(),
                c.ventePermisConstruire(), c.regularisationTypeTerrain(), "INCONNUE",
                c.documentPvReconnaissance(), c.documentNumeroDemandeAcquisition(),
                c.documentActeVentePriveeCommunale(), c.documentSituationJuridique(),
                c.documentActeNotoriete(), c.observations(), c.nombreCommerciauxTerrain(),
                c.commissionCn(), c.prestationAbonnementMensuel());
        var exSituation = assertThrows(BusinessException.class,
                () -> dossierService.create(126, mauvaiseSituation));
        assertEquals(ErrorMessages.DOSSIER_TRANSACTION_SITUATION_TERRAIN_INVALIDE,
                exSituation.getMessage());
    }

    @Test
    void createMontantNegatif409() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        var c = requeteAchat();
        var requete = new CreateDossierTransactionImmobiliereRequest(
                c.nomProprietairePrecedent(), true, false, false, false, false, false, null,
                c.objet(), null, c.achatMesureM2(), c.achatPosition(), c.achatAxeEmplacement(),
                new BigDecimal("-1"), c.achatPermisConstruire(), c.venteMesureM2(),
                c.ventePosition(), c.venteAxeEmplacement(), c.ventePrisePhoto(), c.venteVisite(),
                c.venteMontant(), c.ventePermisConstruire(), c.regularisationTypeTerrain(),
                c.regularisationSituationTerrain(), c.documentPvReconnaissance(),
                c.documentNumeroDemandeAcquisition(), c.documentActeVentePriveeCommunale(),
                c.documentSituationJuridique(), c.documentActeNotoriete(), c.observations(),
                c.nombreCommerciauxTerrain(), c.commissionCn(), c.prestationAbonnementMensuel());

        var ex = assertThrows(BusinessException.class,
                () -> dossierService.create(126, requete));
        assertEquals(ErrorMessages.valeurNegative("montant d'achat"), ex.getMessage());
    }

    @Test
    void updateOk() {
        DemandeService demande = demandeTransaction();
        DossierTransactionImmobiliere existant = DossierTransactionImmobiliere.builder()
                .id(5).demande(demande).build();
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demande));
        when(dossierRepository.findByDemandeId(126)).thenReturn(Optional.of(existant));
        when(dossierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = dossierService.update(126, requeteUpdate());

        assertEquals(5, reponse.id());
        assertFalse(reponse.actionAchat());
        assertTrue(reponse.actionVente());
        assertEquals("MAISON", reponse.objet());
        assertEquals("Plan B", reponse.ventePosition());
        assertEquals("Axe sud", reponse.venteAxeEmplacement());
        assertTrue(reponse.ventePrisePhoto());
        assertTrue(reponse.venteVisite());
        assertEquals("Mise à jour", reponse.observations());
        verify(auditService).log(eq("UPDATE"), eq("DOSSIER"),
                eq("DossierTransactionImmobiliere"), eq(5L), anyString(), anyString());
    }

    @Test
    void getEtUpdateDossierManquant404() {
        when(demandeRepository.findById(126)).thenReturn(Optional.of(demandeTransaction()));
        when(dossierRepository.findByDemandeId(126)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dossierService.getByDemandeId(126));
        assertThrows(ResourceNotFoundException.class,
                () -> dossierService.update(126, requeteUpdate()));
    }
}
