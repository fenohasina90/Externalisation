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
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.DemandeResponse;
import com.cabex.backend.demande.dto.HistoriqueDemandeResponse;
import com.cabex.backend.demande.dto.LigneServiceResponse;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.dto.UpdateDemandeRequest;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final LigneServiceService ligneServiceService;
    private final FactureService factureService;
    private final LigneServiceRepository ligneRepository;
    private final ProposPaiementRepository proposRepository;

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
                                 AuditService auditService,
                                 LigneServiceService ligneServiceService,
                                 FactureService factureService,
                                 LigneServiceRepository ligneRepository,
                                 ProposPaiementRepository proposRepository) {
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
        this.ligneServiceService = ligneServiceService;
        this.factureService = factureService;
        this.ligneRepository = ligneRepository;
        this.proposRepository = proposRepository;
    }

    @Transactional(readOnly = true)
    public Page<DemandeResponse> getAll(String recherche, Integer societeId, Integer serviceId,
                                        Integer commercialId, String statut,
                                        LocalDate dateDebut, LocalDate dateFin,
                                        Boolean actif, String situation, Pageable pageable) {
        Pageable tri = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Order.desc("dateDemande"), Sort.Order.desc("id")));
        Page<DemandeService> page = demandeRepository.rechercher(recherche, societeId, serviceId,
                commercialId, statut, dateDebut, dateFin, actif, situation,
                LocalDate.now(), tri);

        List<DemandeService> demandes = page.getContent();
        if (demandes.isEmpty()) {
            return page.map(d -> toResponse(d));
        }

        List<Integer> demandeIds = demandes.stream()
                .map(DemandeService::getId)
                .toList();
        Map<Integer, List<LigneService>> lignesParDemande = ligneRepository
                .findByDemandeIdInWithService(demandeIds).stream()
                .collect(Collectors.groupingBy(
                        l -> l.getDemande().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<Integer> ligneIds = lignesParDemande.values().stream()
                .flatMap(List::stream)
                .map(LigneService::getId)
                .toList();
        Map<Integer, BigDecimal> paiementsParLigne = ligneIds.isEmpty()
                ? Map.of()
                : ligneRepository.sommesPayeesParLignes(ligneIds).stream()
                        .collect(Collectors.toMap(
                                o -> ((Number) o[0]).intValue(),
                                o -> toBigDecimal(o[1])));
        Map<Integer, BigDecimal> paiementsParDemande = paiementRepository
                .sommesPayeesParDemandes(demandeIds).stream()
                .collect(Collectors.toMap(
                        o -> ((Number) o[0]).intValue(),
                        o -> toBigDecimal(o[1])));
        Map<Integer, String> statuts = historiqueRepository.statutsActuels(demandeIds).stream()
                .collect(Collectors.toMap(
                        o -> ((Number) o[0]).intValue(),
                        o -> (String) o[1]));
        Map<Integer, InfoSituation> situations = situations(demandes);

        return page.map(d -> toResponseListe(d, lignesParDemande, paiementsParLigne,
                paiementsParDemande, statuts, situations));
    }

    private DemandeResponse toResponseListe(DemandeService demande,
                                            Map<Integer, List<LigneService>> lignesParDemande,
                                            Map<Integer, BigDecimal> paiementsParLigne,
                                            Map<Integer, BigDecimal> paiementsParDemande,
                                            Map<Integer, String> statuts,
                                            Map<Integer, InfoSituation> situations) {
        List<LigneService> lignes = lignesParDemande.getOrDefault(demande.getId(), List.of());
        BigDecimal montantTotal = lignes.stream()
                .map(l -> l.getTarif() != null ? l.getTarif() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantPaye = paiementsParDemande.getOrDefault(demande.getId(), BigDecimal.ZERO);
        BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);
        List<LigneServiceResponse> lignesReponse = lignes.stream()
                .map(l -> {
                    BigDecimal tarif = l.getTarif() != null ? l.getTarif() : BigDecimal.ZERO;
                    BigDecimal paye = paiementsParLigne.getOrDefault(l.getId(), BigDecimal.ZERO);
                    return DemandeMapper.toLigneResponse(l, paye,
                            tarif.subtract(paye).max(BigDecimal.ZERO));
                })
                .toList();
        InfoSituation info = situations.getOrDefault(demande.getId(), InfoSituation.NON_PLANIFIE);
        return DemandeMapper.toResponse(demande, statuts.get(demande.getId()),
                montantPaye, reste, lignesReponse, montantTotal,
                info.situation(), info.nombreEcheances(), lignes);
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    @Transactional(readOnly = true)
    public DemandeResponse getById(Integer id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public DemandeResponse getByReference(String reference) {
        return toResponse(demandeRepository.findByReferenceDemande(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", reference))));
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
        Societe societe = null;
        if (request.societeId() != null) {
            societe = societeRepository.findById(request.societeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Société", request.societeId())));
        }

        Commercial commercial = null;
        if (request.commercialId() != null) {
            commercial = commercialRepository.findById(request.commercialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Commercial", request.commercialId())));
        }

        ClientResponsable responsable = validateResponsable(request.clientResponsableId(), societe);

        List<CreateLigneRequest> lignesDemandees = request.lignes() != null
                ? request.lignes().stream().filter(java.util.Objects::nonNull).toList()
                : List.of();
        if (lignesDemandees.isEmpty()) {
            if (request.serviceId() == null || request.tarif() == null
                    || request.debutService() == null || request.finService() == null) {
                throw new BusinessException(ErrorMessages.DONNEES_OBLIGATOIRES);
            }
            lignesDemandees = List.of(new CreateLigneRequest(
                    request.serviceId(), request.tarif(),
                    request.debutService(), request.finService()));
        }

        List<com.cabex.backend.service.entity.Service> servicesDemandes = lignesDemandees.stream()
                .map(l -> serviceRepository.findById(l.serviceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorMessages.introuvable("Service", l.serviceId()))))
                .peek(s -> {
                    if (!Boolean.TRUE.equals(s.getActif())) {
                        throw new BusinessException(ErrorMessages.SERVICE_INDISPONIBLE);
                    }
                })
                .toList();
        long nbCreation = servicesDemandes.stream()
                .filter(s -> com.cabex.backend.dossier.DossierRegles
                        .estTypeCreation(s.getTypeService()))
                .count();
        boolean typeSolo = servicesDemandes.stream()
                .anyMatch(s -> com.cabex.backend.dossier.DossierRegles
                        .estTypeCreation(s.getTypeService())
                        || com.cabex.backend.dossier.DossierRegles
                        .estTypeTransactionImmobiliere(s.getTypeService()));
        if (typeSolo && lignesDemandees.size() > 1) {
            throw new BusinessException(ErrorMessages.SERVICE_SOLO_CREATION_TRANSACTION);
        }
        if (societe == null && nbCreation == 0) {
            throw new BusinessException(ErrorMessages.DEMANDE_SOCIETE_REQUISE);
        }
        for (CreateLigneRequest ligne : lignesDemandees) {
            validateDates(ligne.debutService(), ligne.finService());
            if (ligne.tarif().signum() < 0) {
                throw new BusinessException(ErrorMessages.TARIF_POSITIF);
            }
        }
        long distincts = lignesDemandees.stream().map(CreateLigneRequest::serviceId).distinct().count();
        if (distincts < lignesDemandees.size()) {
            throw new BusinessException("Un service ne peut pas être sélectionné deux fois dans une demande");
        }

        DemandeService demande = DemandeService.builder()
                .referenceDemande(genererReference(LocalDate.now().getYear()))
                .dateDemande(request.dateDemande() != null
                        ? request.dateDemande()
                        : LocalDate.now())
                .societe(societe)
                .commercial(commercial)
                .clientResponsable(responsable)
                .commentaire(request.commentaire())
                .createdBy(currentUtilisateur())
                .actif(true)
                .build();
        demande = demandeRepository.save(demande);

        enviereHistorique(demande, STATUT_EN_ATTENTE,
                "Demande créée" + (request.commentaire() != null ? " : " + request.commentaire() : null));

        for (CreateLigneRequest ligne : lignesDemandees) {
            Service service = serviceRepository.findById(ligne.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Service", ligne.serviceId())));
            LigneService creee = LigneService.builder()
                    .demande(demande)
                    .service(service)
                    .tarif(ligne.tarif())
                    .debutService(ligne.debutService())
                    .finService(ligne.finService())
                    .build();
            creee = ligneRepository.save(creee);
            ligneServiceService.ajouterHistoriqueCreation(creee,
                    "Service ajouté à la création : " + service.getNomService());
        }

        factureService.genererPourDemande(demande.getId(),
                request.modeFacturation() != null ? request.modeFacturation() : "PAR_SERVICE");

        notificationService.notifierNouvelleDemande(demande);

        auditService.log(AuditActions.CREATE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), null, toResponse(demande).referenceDemande());

        return toResponse(demande);
    }

    @Transactional
    public DemandeResponse update(Integer id, UpdateDemandeRequest request) {
        DemandeService demande = getEntity(id);
        String statutActuel = statutActuel(demande.getId());
        if (!STATUT_EN_ATTENTE.equals(statutActuel)) {
            throw new BusinessException(
                    "La demande ne peut être modifiée que quand elle est EN_ATTENTE");
        }

        demande.setCommentaire(request.commentaire());

        if (request.commercialId() != null) {
            Commercial commercial = commercialRepository.findById(request.commercialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Commercial", request.commercialId())));
            demande.setCommercial(commercial);
        }

        demande = demandeRepository.save(demande);

        auditService.log(AuditActions.UPDATE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), null,
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
        if (STATUT_VALIDATION.equals(nouveau)) {
            verifierPassageValidation(demande);
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
        ClientResponsable responsable = responsableRepository.findById(responsableId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Responsable", responsableId)));
        if (societe == null) {
            // Création d'entreprise : responsable libre (sans société) exigé.
            if (responsable.getSociete() != null) {
                throw new BusinessException(
                        "Le responsable n'appartient pas à la société sélectionnée");
            }
            return responsable;
        }
        if (responsable.getSociete() == null
                || !responsable.getSociete().getId().equals(societe.getId())) {
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
            // Annulation ordinaire uniquement depuis EN_ATTENTE (§3.2).
            case "EN_ATTENTE" -> STATUT_VALIDATION.equals(nouveau) || STATUT_ANNULEE.equals(nouveau);
            case "VALIDATION" -> STATUT_EN_COURS.equals(nouveau);
            case "EN_COURS" -> STATUT_TERMINEE.equals(nouveau);
            default -> false;
        };
    }

    /**
     * Garde du passage à VALIDATION : toutes les factures validées et
     * un plan de paiement établi pour chaque ligne (contrôle backend).
     */
    private void verifierPassageValidation(DemandeService demande) {
        List<LigneService> lignes = ligneRepository.findByDemandeId(demande.getId());
        if (lignes.isEmpty()) {
            throw new BusinessException("Une demande contient au moins un service");
        }
        if (!factureService.toutesValidees(demande.getId())) {
            throw new BusinessException(
                    "Toutes les factures de la demande doivent être validées avant le passage à VALIDATION");
        }
        for (LigneService ligne : lignes) {
            if (!proposRepository.existsByLigneServiceId(ligne.getId())) {
                throw new BusinessException(
                        "Chaque service doit disposer de son plan de paiement avant le passage à VALIDATION");
            }
        }
    }

    /**
     * Promotion automatique : une ligne passe EN_COURS à son premier paiement ;
     * la demande passe EN_COURS dès qu'au moins une ligne a démarré.
     */
    @Transactional
    public void promouvoirEnCoursParLigne(LigneService ligne, String commentairePaiement) {
        boolean ligneDemarree = ligneServiceService.avancerStatut(ligne,
                LigneServiceService.LIGNE_EN_COURS,
                "Service démarré : " + commentairePaiement, "AUTO");
        if (!ligneDemarree) {
            return;
        }
        DemandeService demande = ligne.getDemande();
        String actuel = statutActuel(demande.getId());
        if (STATUT_EN_ATTENTE.equals(actuel)) {
            enviereHistorique(demande, STATUT_VALIDATION,
                    "Demande validée par le premier paiement (service démarré)");
            auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                    Long.valueOf(demande.getId()), STATUT_EN_ATTENTE, STATUT_VALIDATION);
            actuel = STATUT_VALIDATION;
        }
        if (STATUT_VALIDATION.equals(actuel)) {
            enviereHistorique(demande, STATUT_EN_COURS,
                    "Service démarré après validation (premier paiement enregistré)");
            notificationService.notifierChangementStatut(demande, STATUT_EN_COURS);
            auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                    Long.valueOf(demande.getId()), STATUT_VALIDATION, STATUT_EN_COURS);
        }
    }

    /**
     * Clôture automatique : la demande passe TERMINEE quand toutes ses lignes
     * ont atteint leur date de fin (soldes éventuels conservés).
     */
    @Transactional
    public boolean cloturerSiTerminee(Integer demandeId) {
        DemandeService demande = getEntity(demandeId);
        String actuel = statutActuel(demandeId);
        if (!STATUT_EN_COURS.equals(actuel) && !STATUT_VALIDATION.equals(actuel)) {
            return false;
        }
        List<LigneService> lignes = ligneRepository.findByDemandeId(demandeId);
        if (lignes.isEmpty()) {
            return false;
        }
        boolean toutesTerminees = lignes.stream()
                .allMatch(l -> LigneServiceService.LIGNE_TERMINE.equals(l.getStatut()));
        if (!toutesTerminees) {
            return false;
        }
        enviereHistorique(demande, STATUT_TERMINEE,
                "Tous les services ont atteint leur date de fin (clôture automatique)");
        notificationService.notifierChangementStatut(demande, STATUT_TERMINEE);
        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), actuel, STATUT_TERMINEE);
        return true;
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
        BigDecimal montantTotal = ligneServiceService.montantTotalDemande(demande.getId());
        BigDecimal reste = montantTotal.subtract(montantPaye);
        String statut = statutActuel(demande.getId());
        InfoSituation info = situation(demande.getId());
        return DemandeMapper.toResponse(demande, statut, montantPaye, reste.max(BigDecimal.ZERO),
                ligneServiceService.listerParDemande(demande.getId()), montantTotal,
                info.situation(), info.nombreEcheances());
    }

    /** Situation de règlement et nombre d'échéances d'une demande isolée. */
    private InfoSituation situation(Integer demandeId) {
        List<Object[]> lignes = demandeRepository.situations(List.of(demandeId), LocalDate.now());
        if (lignes.isEmpty()) {
            return InfoSituation.NON_PLANIFIE;
        }
        return new InfoSituation(SituationPaiement.classer(lignes.get(0)),
                ((Number) lignes.get(0)[2]).longValue());
    }

    /**
     * Situations de toute une page, en une seule requête. La valeur renvoyée
     * alimente la colonne « Situation » des listes : elle est calculée par la
     * même classification que le filtre {@code situation}, pour que les deux
     * ne puissent pas diverger.
     */
    private Map<Integer, InfoSituation> situations(List<DemandeService> demandes) {
        List<Integer> ids = demandes.stream()
                .map(DemandeService::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return demandeRepository.situations(ids, LocalDate.now()).stream()
                .collect(Collectors.toMap(
                        ligne -> ((Number) ligne[0]).intValue(),
                        ligne -> new InfoSituation(
                                SituationPaiement.classer(ligne),
                                ((Number) ligne[2]).longValue())));
    }

    /** Situation de règlement d'une demande et nombre total d'échéances. */
    private record InfoSituation(String situation, Long nombreEcheances) {
        static final InfoSituation NON_PLANIFIE =
                new InfoSituation(SituationPaiement.NON_PLANIFIE, 0L);
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
