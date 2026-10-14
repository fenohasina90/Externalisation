package com.cabex.backend.demande.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.demande.dto.ChangeStatutRequest;
import com.cabex.backend.demande.dto.CreateDemandeRequest;
import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueDemandeResponse;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.dto.UpdateDemandeRequest;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@org.springframework.stereotype.Service
public class DemandeGestionService {

    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    private static final String STATUT_VALIDATION = "VALIDATION";
    private static final String STATUT_EN_COURS = "EN_COURS";
    private static final String STATUT_ANNULEE = "ANNULEE";
    private static final String STATUT_TERMINEE = "TERMINEE";

    private final DemandeServiceRepository demandeRepository;
    private final SocieteRepository societeRepository;
    private final ClientResponsableRepository responsableRepository;
    private final CommercialRepository commercialRepository;
    private final ServiceRepository serviceRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final StatutDemandeRepository statutRepository;
    private final PaiementRepository paiementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public DemandeGestionService(DemandeServiceRepository demandeRepository,
                                 SocieteRepository societeRepository,
                                 ClientResponsableRepository responsableRepository,
                                 CommercialRepository commercialRepository,
                                 ServiceRepository serviceRepository,
                                 HistoriqueDemandeRepository historiqueRepository,
                                 StatutDemandeRepository statutRepository,
                                 PaiementRepository paiementRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 NotificationService notificationService,
                                 AuditService auditService) {
        this.demandeRepository = demandeRepository;
        this.societeRepository = societeRepository;
        this.responsableRepository = responsableRepository;
        this.commercialRepository = commercialRepository;
        this.serviceRepository = serviceRepository;
        this.historiqueRepository = historiqueRepository;
        this.statutRepository = statutRepository;
        this.paiementRepository = paiementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DemandeResponse> getAll(String recherche, Integer societeId, Integer serviceId,
                                        Integer domaineId, Integer commercialId, String statut,
                                        LocalDate dateDebut, LocalDate dateFin,
                                        Boolean actif, Pageable pageable) {
        return demandeRepository.rechercher(recherche, societeId, serviceId, domaineId, commercialId,
                        statut, dateDebut, dateFin, actif, pageable)
                .map(d -> toResponse(d));
    }

    @Transactional(readOnly = true)
    public DemandeResponse getById(Integer id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<HistoriqueDemandeResponse> getHistorique(Integer demandeId) {
        getEntity(demandeId);
        List<HistoriqueDemande> historique = historiqueRepository.findByDemandeIdOrderByIdAsc(demandeId);
        List<HistoriqueDemandeResponse> resultat = new ArrayList<>();
        String ancienStatut = null;
        for (HistoriqueDemande h : historique) {
            resultat.add(DemandeMapper.toHistoriqueResponse(h, ancienStatut));
            if (h.getStatut() != null) {
                ancienStatut = h.getStatut().getStatut();
            }
        }
        return resultat;
    }

    @Transactional
    public DemandeResponse create(CreateDemandeRequest request) {
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Service", request.serviceId())));
        if (!Boolean.TRUE.equals(service.getActif())) {
            throw new BusinessException(ErrorMessages.SERVICE_INDISPONIBLE);
        }
        boolean creationEntreprise =
                com.cabex.backend.dossier.DossierRegles.estServiceCreation(service.getNomService());

        Societe societe = null;
        if (request.societeId() != null) {
            societe = societeRepository.findById(request.societeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Société", request.societeId())));
        } else if (!creationEntreprise) {
            throw new BusinessException(ErrorMessages.DEMANDE_SOCIETE_REQUISE);
        }

        Commercial commercial = null;
        if (request.commercialId() != null) {
            commercial = commercialRepository.findById(request.commercialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Commercial", request.commercialId())));
        }

        ClientResponsable responsable = validateResponsable(request.clientResponsableId(), societe);

        validateDates(request.debutService(), request.finService());
        if (request.tarif() != null
                && request.tarif().signum() < 0) {
            throw new BusinessException(ErrorMessages.TARIF_POSITIF);
        }

        BigDecimal tarif = request.tarif() != null
                ? request.tarif()
                : service.getTarifService();

        DemandeService demande = DemandeService.builder()
                .referenceDemande(genererReference(LocalDate.now().getYear()))
                .dateDemande(request.dateDemande() != null
                        ? request.dateDemande()
                        : LocalDate.now())
                .debutService(request.debutService())
                .finService(request.finService())
                .societe(societe)
                .commercial(commercial)
                .clientResponsable(responsable)
                .service(service)
                .tarif(tarif)
                .commentaire(request.commentaire())
                .createdBy(currentUtilisateur())
                .actif(true)
                .build();
        demande = demandeRepository.save(demande);

        enviereHistorique(demande, STATUT_EN_ATTENTE,
                "Demande créée" + (request.commentaire() != null ? " : " + request.commentaire() : null));

        notificationService.notifierNouvelleDemande(demande);

        auditService.log(AuditActions.CREATE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), null, toResponse(demande).referenceDemande());

        return toResponse(demande);
    }

    @Transactional
    public DemandeResponse update(Integer id, UpdateDemandeRequest request) {
        DemandeService demande = getEntity(id);
        String statutActuel = statutActuel(demande.getId());
        if (STATUT_TERMINEE.equals(statutActuel) || STATUT_ANNULEE.equals(statutActuel)) {
            throw new BusinessException(ErrorMessages.TRANSITION_STATUT_INTERDITE);
        }

        validateDates(request.debutService(), request.finService());

        demande.setDebutService(request.debutService());
        demande.setFinService(request.finService());
        demande.setCommentaire(request.commentaire());

        if (request.commercialId() != null) {
            Commercial commercial = commercialRepository.findById(request.commercialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Commercial", request.commercialId())));
            demande.setCommercial(commercial);
        }

        demande = demandeRepository.save(demande);

        auditService.log(AuditActions.UPDATE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()),
                (request.debutService() != null ? "début=" + request.debutService() : "") +
                        (request.finService() != null ? ", fin=" + request.finService() : ""),
                toResponse(demande).referenceDemande());

        return toResponse(demande);
    }

    @Transactional
    public DemandeResponse changeStatut(Integer id, ChangeStatutRequest request) {
        DemandeService demande = getEntity(id);
        String nouveau = request.statut().trim().toUpperCase(Locale.ROOT);
        StatutDemande cible = statutRepository.findByStatut(nouveau)
                .orElseThrow(() -> new BusinessException(ErrorMessages.STATUT_INVALIDE));

        String actuel = statutActuel(demande.getId());
        if (!transitionAutorisee(actuel, nouveau)) {
            throw new BusinessException(ErrorMessages.TRANSITION_STATUT_INTERDITE);
        }

        enviereHistorique(demande, nouveau, request.commentaire());

        notificationService.notifierChangementStatut(demande, nouveau);

        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), actuel, nouveau);

        return toResponse(demande);
    }

    @Transactional
    public DemandeResponse archive(Integer id, boolean archive) {
        DemandeService demande = getEntity(id);
        String ancien = demande.getActif() != null && demande.getActif() ? "actif" : "inactif";
        demande.setActif(!archive);
        demande.setArchivedAt(archive ? java.time.LocalDateTime.now() : null);
        DemandeResponse response = toResponse(demandeRepository.save(demande));
        auditService.log(archive ? AuditActions.ARCHIVE : AuditActions.UPDATE,
                "DEMANDE", "DemandeService", Long.valueOf(demande.getId()),
                ancien, archive ? "archivé" : "désarchivé");
        return response;
    }

    @Transactional(readOnly = true)
    public List<StatutResponse> getStatuts() {
        return statutRepository.findAll().stream()
                .map(DemandeMapper::toStatutResponse)
                .toList();
    }

    private ClientResponsable validateResponsable(Integer responsableId, Societe societe) {
        if (responsableId == null) {
            return null;
        }
        if (societe == null) {
            throw new BusinessException(ErrorMessages.RESPONSABLE_SANS_SOCIETE);
        }
        ClientResponsable responsable = responsableRepository.findById(responsableId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Responsable", responsableId)));
        if (!responsable.getSociete().getId().equals(societe.getId())) {
            throw new BusinessException(
                    "Le responsable n'appartient pas à la société sélectionnée");
        }
        return responsable;
    }

    private void validateDates(LocalDate debut, LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }
    }

    private void enviereHistorique(DemandeService demande, String statut, String commentaire) {
        StatutDemande statutEntite = statutRepository.findByStatut(statut)
                .orElseThrow(() -> new BusinessException(ErrorMessages.STATUT_INVALIDE));
        HistoriqueDemande historique = HistoriqueDemande.builder()
                .demande(demande)
                .statut(statutEntite)
                .commentaire(commentaire)
                .utilisateur(currentUtilisateur())
                .build();
        historiqueRepository.save(historique);
    }

    private boolean transitionAutorisee(String actuel, String nouveau) {
        if (actuel == null) {
            return STATUT_EN_ATTENTE.equals(nouveau);
        }
        return switch (actuel) {
            case "EN_ATTENTE" -> STATUT_ANNULEE.equals(nouveau);
            case "VALIDATION" -> STATUT_EN_COURS.equals(nouveau) || STATUT_ANNULEE.equals(nouveau);
            case "EN_COURS" -> STATUT_TERMINEE.equals(nouveau) || STATUT_ANNULEE.equals(nouveau);
            default -> false;
        };
    }

    @Transactional
    public void validerParPremierPaiement(DemandeService demande, Integer numeroEcheance) {
        if (numeroEcheance == null || numeroEcheance != 1) {
            return;
        }
        if (!STATUT_EN_ATTENTE.equals(statutActuel(demande.getId()))) {
            return;
        }
        enviereHistorique(demande, STATUT_VALIDATION,
                "Demande validée par le paiement du premier tranche (échéance n°1)");
        notificationService.notifierChangementStatut(demande, STATUT_VALIDATION);
        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), STATUT_EN_ATTENTE, STATUT_VALIDATION);
        enviereHistorique(demande, STATUT_EN_COURS,
                "Service démarré après validation (premier paiement enregistré)");
        notificationService.notifierChangementStatut(demande, STATUT_EN_COURS);
        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), STATUT_VALIDATION, STATUT_EN_COURS);
    }

    private String genererReference(int annee) {
        String prefix = "DEM-" + annee + "-";
        long seq = demandeRepository.countByReferenceDemandeStartingWith(prefix) + 1;
        String reference;
        do {
            reference = prefix + String.format("%05d", seq);
            seq++;
        } while (demandeRepository.existsByReferenceDemande(reference));
        return reference;
    }

    private String statutActuel(Integer demandeId) {
        return historiqueRepository.findFirstByDemandeIdOrderByIdDesc(demandeId)
                .map(h -> h.getStatut().getStatut())
                .orElse(null);
    }

    private DemandeResponse toResponse(DemandeService demande) {
        BigDecimal montantPaye = paiementRepository.sommePayeeParDemande(demande.getId());
        if (montantPaye == null) {
            montantPaye = BigDecimal.ZERO;
        }
        BigDecimal reste = demande.getTarif().subtract(montantPaye);
        String statut = statutActuel(demande.getId());
        return DemandeMapper.toResponse(demande, statut, montantPaye, reste.max(BigDecimal.ZERO));
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private DemandeService getEntity(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Demande", id)));
    }
}
