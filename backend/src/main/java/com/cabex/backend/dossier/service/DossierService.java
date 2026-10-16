package com.cabex.backend.dossier.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.dossier.DossierRegles;
import com.cabex.backend.dossier.dto.AssocieRequest;
import com.cabex.backend.dossier.dto.CreateDossierRequest;
import com.cabex.backend.dossier.dto.DossierResponse;
import com.cabex.backend.dossier.dto.UpdateDossierRequest;
import com.cabex.backend.dossier.entity.DossierAssocie;
import com.cabex.backend.dossier.entity.DossierCreation;
import com.cabex.backend.dossier.mapper.DossierMapper;
import com.cabex.backend.dossier.repository.DossierCreationRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DossierService {

    private final DossierCreationRepository dossierRepository;
    private final DemandeServiceRepository demandeRepository;
    private final AuditService auditService;

    public DossierService(DossierCreationRepository dossierRepository,
                          DemandeServiceRepository demandeRepository,
                          AuditService auditService) {
        this.dossierRepository = dossierRepository;
        this.demandeRepository = demandeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DossierResponse getByDemandeId(Integer demandeId) {
        return DossierMapper.toResponse(getDossier(demandeId));
    }

    @Transactional
    public DossierResponse create(Integer demandeId, CreateDossierRequest request) {
        DemandeService demande = getDemandeCreation(demandeId);
        if (dossierRepository.existsByDemandeId(demandeId)) {
            throw new DuplicateResourceException(ErrorMessages.DOSSIER_DEJA_EXISTANT);
        }
        DossierCreation dossier = appliquer(new DossierCreation(), demande, request);
        DossierCreation saved = dossierRepository.save(dossier);
        auditService.log(AuditActions.CREATE, "DOSSIER", "DossierCreation",
                Long.valueOf(saved.getId()), null, "demande=" + demande.getReferenceDemande());
        return DossierMapper.toResponse(saved);
    }

    @Transactional
    public DossierResponse update(Integer demandeId, UpdateDossierRequest request) {
        DemandeService demande = getDemandeCreation(demandeId);
        DossierCreation dossier = getDossier(demandeId);
        DossierCreation updated = appliquer(dossier, demande, request.versCreation());
        DossierCreation saved = dossierRepository.save(updated);
        auditService.log(AuditActions.UPDATE, "DOSSIER", "DossierCreation",
                Long.valueOf(saved.getId()), "demande=" + demande.getReferenceDemande(),
                "demande=" + demande.getReferenceDemande());
        return DossierMapper.toResponse(saved);
    }

    private DemandeService getDemandeCreation(Integer demandeId) {
        DemandeService demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
        // Mix interdit : la demande de création porte une ligne unique de type CREATION.
        boolean creation = demande.getLignes() != null && !demande.getLignes().isEmpty()
                && demande.getLignes().stream().allMatch(l -> l.getService() != null
                        && DossierRegles.estTypeCreation(l.getService().getTypeService()));
        if (!creation) {
            throw new BusinessException(ErrorMessages.DOSSIER_SERVICE_NON_CREATION);
        }
        return demande;
    }

    private DossierCreation getDossier(Integer demandeId) {
        // La demande doit exister avant de parler de son dossier.
        demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
        return dossierRepository.findByDemandeId(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Dossier de création", "demande " + demandeId)));
    }

    private DossierCreation appliquer(DossierCreation dossier, DemandeService demande,
                                      CreateDossierRequest request) {
        String forme = request.formeJuridique().trim().toUpperCase(Locale.ROOT);
        if (!DossierRegles.FORMES_JURIDIQUES.contains(forme)) {
            throw new BusinessException(ErrorMessages.DOSSIER_FORME_INVALIDE);
        }
        if ("AUTRE".equals(forme)
                && (request.formeJuridiquePrecision() == null
                    || request.formeJuridiquePrecision().isBlank())) {
            throw new BusinessException(ErrorMessages.DOSSIER_PRECISION_REQUISE);
        }
        List<AssocieRequest> associes = request.associes() != null ? request.associes() : List.of();
        if (DossierRegles.estSociete(forme) && associes.stream().allMatch(a -> a.nom() == null || a.nom().isBlank())) {
            throw new BusinessException(ErrorMessages.DOSSIER_ASSOCIES_REQUIS);
        }
        if (request.typeHebergement() != null && !request.typeHebergement().isBlank()
                && !DossierRegles.TYPES_HEBERGEMENT.contains(request.typeHebergement().trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(ErrorMessages.formatInvalide("type d'hébergement"));
        }
        if (request.regimeActivite() != null && !request.regimeActivite().isBlank()
                && !DossierRegles.REGIMES_ACTIVITE.contains(request.regimeActivite().trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(ErrorMessages.formatInvalide("régime d'activité"));
        }

        dossier.setDemande(demande);
        dossier.setFormeJuridique(forme);
        dossier.setFormeJuridiquePrecision(trim(request.formeJuridiquePrecision()));
        dossier.setDenomination(request.denomination().trim());
        dossier.setSiegeSocialActuel(trim(request.siegeSocialActuel()));
        dossier.setCapitalSocial(request.capitalSocial());
        dossier.setNombreAssociesDeclare(request.nombreAssociesDeclare());
        dossier.setNomGerant(trim(request.nomGerant()));
        dossier.setFonctionGerant(trim(request.fonctionGerant()));
        dossier.setLocationGerance(Boolean.TRUE.equals(request.locationGerance()));
        dossier.setSecteurActivite(trim(request.secteurActivite()));
        dossier.setAutorisationSpeciale(trim(request.autorisationSpeciale()));
        dossier.setAdresseSiege(trim(request.adresseSiege()));
        dossier.setVille(trim(request.ville()));
        dossier.setCommune(trim(request.commune()));
        dossier.setDistrict(trim(request.district()));
        dossier.setRegion(trim(request.region()));
        dossier.setTypeHebergement(trimUpper(request.typeHebergement()));
        dossier.setHebergementPrecision(trim(request.hebergementPrecision()));
        dossier.setContratBail(request.contratBail());
        dossier.setContratBailDetail(trim(request.contratBailDetail()));
        dossier.setObjetSocial(trim(request.objetSocial()));
        dossier.setDateSouhaiteeDebut(request.dateSouhaiteeDebut());
        dossier.setRegimeActivite(trimUpper(request.regimeActivite()));
        dossier.setActivitesEtablissement(trim(request.activitesEtablissement()));
        dossier.setObservations(trim(request.observations()));
        dossier.setAssujettissementTva(trim(request.assujettissementTva()));
        dossier.setAutreSociete(trim(request.autreSociete()));
        dossier.setSituationMatrimoniale(trim(request.situationMatrimoniale()));
        dossier.setRegimeImposition(trim(request.regimeImposition()));

        if (dossier.getAssocies() == null) {
            dossier.setAssocies(new ArrayList<>());
        }
        dossier.getAssocies().clear();
        int ordre = 0;
        List<DossierAssocie> nouveaux = new ArrayList<>();
        for (AssocieRequest a : associes) {
            if (a.nom() == null || a.nom().isBlank()) {
                continue;
            }
            DossierAssocie associe = DossierAssocie.builder()
                    .dossier(dossier)
                    .ordre(ordre++)
                    .nom(a.nom().trim())
                    .partSociale(trim(a.partSociale()))
                    .email(trim(a.email()))
                    .contact(trim(a.contact()))
                    .build();
            nouveaux.add(associe);
        }
        dossier.getAssocies().addAll(nouveaux);
        return dossier;
    }

    private String trim(String valeur) {
        return valeur == null ? null : valeur.trim();
    }

    private String trimUpper(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim().toUpperCase(Locale.ROOT);
    }
}
