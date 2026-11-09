package com.cabex.backend.dossier.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.DossierRegles;
import com.cabex.backend.dossier.DossierTransactionImmobiliereRegles;
import com.cabex.backend.dossier.dto.CreateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.dto.DossierTransactionImmobiliereResponse;
import com.cabex.backend.dossier.dto.UpdateDossierTransactionImmobiliereRequest;
import com.cabex.backend.dossier.entity.DossierTransactionImmobiliere;
import com.cabex.backend.dossier.mapper.DossierTransactionImmobiliereMapper;
import com.cabex.backend.dossier.repository.DossierTransactionImmobiliereRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DossierTransactionImmobiliereService {

    private final DossierTransactionImmobiliereRepository dossierRepository;
    private final DemandeServiceRepository demandeRepository;
    private final AuditService auditService;

    public DossierTransactionImmobiliereService(
            DossierTransactionImmobiliereRepository dossierRepository,
            DemandeServiceRepository demandeRepository,
            AuditService auditService) {
        this.dossierRepository = dossierRepository;
        this.demandeRepository = demandeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DossierTransactionImmobiliereResponse getByDemandeId(Integer demandeId) {
        getDemande(demandeId);
        return DossierTransactionImmobiliereMapper.toResponse(getDossier(demandeId));
    }

    @Transactional
    public DossierTransactionImmobiliereResponse create(
            Integer demandeId, CreateDossierTransactionImmobiliereRequest request) {
        DemandeService demande = getDemandeTransaction(demandeId);
        if (dossierRepository.existsByDemandeId(demandeId)) {
            throw new DuplicateResourceException(
                    ErrorMessages.DOSSIER_TRANSACTION_DEJA_EXISTANT);
        }
        DossierTransactionImmobiliere dossier =
                appliquer(new DossierTransactionImmobiliere(), demande, request);
        DossierTransactionImmobiliere saved = dossierRepository.save(dossier);
        auditService.log(AuditActions.CREATE, "DOSSIER", "DossierTransactionImmobiliere",
                Long.valueOf(saved.getId()), null, demande.getReferenceDemande());
        return DossierTransactionImmobiliereMapper.toResponse(saved);
    }

    @Transactional
    public DossierTransactionImmobiliereResponse update(
            Integer demandeId, UpdateDossierTransactionImmobiliereRequest request) {
        DemandeService demande = getDemandeTransaction(demandeId);
        DossierTransactionImmobiliere dossier = getDossier(demandeId);
        DossierTransactionImmobiliere updated =
                appliquer(dossier, demande, request.versCreation());
        DossierTransactionImmobiliere saved = dossierRepository.save(updated);
        auditService.log(AuditActions.UPDATE, "DOSSIER", "DossierTransactionImmobiliere",
                Long.valueOf(saved.getId()), demande.getReferenceDemande(),
                demande.getReferenceDemande());
        return DossierTransactionImmobiliereMapper.toResponse(saved);
    }

    private DemandeService getDemandeTransaction(Integer demandeId) {
        DemandeService demande = getDemande(demandeId);
        boolean transaction = demande.getLignes() != null
                && demande.getLignes().stream().anyMatch(l -> l.getService() != null
                        && DossierRegles.estTypeTransactionImmobiliere(
                                l.getService().getTypeService()));
        if (!transaction) {
            throw new BusinessException(
                    ErrorMessages.DOSSIER_TRANSACTION_SERVICE_NON_TRANSACTION);
        }
        return demande;
    }

    private DemandeService getDemande(Integer demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
    }

    private DossierTransactionImmobiliere getDossier(Integer demandeId) {
        return dossierRepository.findByDemandeId(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Dossier de transaction immobilière",
                                "demande " + demandeId)));
    }

    private DossierTransactionImmobiliere appliquer(
            DossierTransactionImmobiliere dossier,
            DemandeService demande,
            CreateDossierTransactionImmobiliereRequest request) {
        boolean actionAutre = Boolean.TRUE.equals(request.actionAutre());
        if (!Boolean.TRUE.equals(request.actionAchat())
                && !Boolean.TRUE.equals(request.actionVente())
                && !Boolean.TRUE.equals(request.actionLocation())
                && !Boolean.TRUE.equals(request.actionRegularisationPapiers())
                && !Boolean.TRUE.equals(request.actionGestionImmobiliere())
                && !actionAutre) {
            throw new BusinessException(ErrorMessages.DOSSIER_TRANSACTION_ACTION_REQUISE);
        }
        String actionAutrePrecision = trim(request.actionAutrePrecision());
        if (actionAutre && actionAutrePrecision == null) {
            throw new BusinessException(
                    ErrorMessages.DOSSIER_TRANSACTION_ACTION_AUTRE_PRECISION_REQUISE);
        }

        String objet = DossierTransactionImmobiliereRegles.normaliser(request.objet());
        if (objet != null
                && !DossierTransactionImmobiliereRegles.OBJETS.contains(objet)) {
            throw new BusinessException(ErrorMessages.DOSSIER_TRANSACTION_OBJET_INVALIDE);
        }
        String objetAutrePrecision = trim(request.objetAutrePrecision());
        if ("AUTRE".equals(objet) && objetAutrePrecision == null) {
            throw new BusinessException(
                    ErrorMessages.DOSSIER_TRANSACTION_OBJET_AUTRE_PRECISION_REQUISE);
        }

        String typeTerrain = DossierTransactionImmobiliereRegles.normaliser(
                request.regularisationTypeTerrain());
        if (typeTerrain != null
                && !DossierTransactionImmobiliereRegles.TYPES_TERRAIN.contains(typeTerrain)) {
            throw new BusinessException(
                    ErrorMessages.DOSSIER_TRANSACTION_TYPE_TERRAIN_INVALIDE);
        }
        String situationTerrain = DossierTransactionImmobiliereRegles.normaliser(
                request.regularisationSituationTerrain());
        if (situationTerrain != null
                && !DossierTransactionImmobiliereRegles.SITUATIONS_TERRAIN.contains(situationTerrain)) {
            throw new BusinessException(
                    ErrorMessages.DOSSIER_TRANSACTION_SITUATION_TERRAIN_INVALIDE);
        }

        validerPositif(request.achatMesureM2(), "mesure d'achat");
        validerPositif(request.achatMontant(), "montant d'achat");
        validerPositif(request.venteMesureM2(), "mesure de vente");
        validerPositif(request.venteMontant(), "montant de vente");
        validerPositif(request.commissionCn(), "commission CN");
        if (request.nombreCommerciauxTerrain() != null
                && request.nombreCommerciauxTerrain() < 0) {
            throw new BusinessException(
                    ErrorMessages.valeurNegative("nombre de commerciaux sur terrain"));
        }

        dossier.setDemande(demande);
        dossier.setNomProprietairePrecedent(trim(request.nomProprietairePrecedent()));
        dossier.setActionAchat(Boolean.TRUE.equals(request.actionAchat()));
        dossier.setActionVente(Boolean.TRUE.equals(request.actionVente()));
        dossier.setActionLocation(Boolean.TRUE.equals(request.actionLocation()));
        dossier.setActionRegularisationPapiers(
                Boolean.TRUE.equals(request.actionRegularisationPapiers()));
        dossier.setActionGestionImmobiliere(
                Boolean.TRUE.equals(request.actionGestionImmobiliere()));
        dossier.setActionAutre(actionAutre);
        dossier.setActionAutrePrecision(actionAutrePrecision);
        dossier.setObjet(objet);
        dossier.setObjetAutrePrecision(objetAutrePrecision);
        dossier.setAchatMesureM2(request.achatMesureM2());
        dossier.setAchatPosition(trim(request.achatPosition()));
        dossier.setAchatAxeEmplacement(trim(request.achatAxeEmplacement()));
        dossier.setAchatMontant(request.achatMontant());
        dossier.setAchatPermisConstruire(request.achatPermisConstruire());
        dossier.setVenteMesureM2(request.venteMesureM2());
        dossier.setVentePosition(trim(request.ventePosition()));
        dossier.setVenteAxeEmplacement(trim(request.venteAxeEmplacement()));
        dossier.setVentePrisePhoto(request.ventePrisePhoto());
        dossier.setVenteVisite(request.venteVisite());
        dossier.setVenteMontant(request.venteMontant());
        dossier.setVentePermisConstruire(request.ventePermisConstruire());
        dossier.setRegularisationTypeTerrain(typeTerrain);
        dossier.setRegularisationSituationTerrain(situationTerrain);
        dossier.setDocumentPvReconnaissance(
                Boolean.TRUE.equals(request.documentPvReconnaissance()));
        dossier.setDocumentNumeroDemandeAcquisition(
                Boolean.TRUE.equals(request.documentNumeroDemandeAcquisition()));
        dossier.setDocumentActeVentePriveeCommunale(
                Boolean.TRUE.equals(request.documentActeVentePriveeCommunale()));
        dossier.setDocumentSituationJuridique(
                Boolean.TRUE.equals(request.documentSituationJuridique()));
        dossier.setDocumentActeNotoriete(
                Boolean.TRUE.equals(request.documentActeNotoriete()));
        dossier.setObservations(trim(request.observations()));
        dossier.setNombreCommerciauxTerrain(request.nombreCommerciauxTerrain());
        dossier.setCommissionCn(request.commissionCn());
        dossier.setPrestationAbonnementMensuel(
                Boolean.TRUE.equals(request.prestationAbonnementMensuel()));
        return dossier;
    }

    private void validerPositif(BigDecimal valeur, String champ) {
        if (valeur != null && valeur.signum() < 0) {
            throw new BusinessException(ErrorMessages.valeurNegative(champ));
        }
    }

    private String trim(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
