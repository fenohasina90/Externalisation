package com.cabex.backend.demande.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
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
    private final ConfigurationService configurationService;

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
                                 ProposPaiementRepository proposRepository,
                                 ConfigurationService configurationService) {
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
        this.configurationService = configurationService;
    }

    @Transactional(readOnly = true)
    public Page<DemandeResponse> getAll(String recherche, Integer societeId, Integer serviceId,
                                        Integer commercialId, String statut,
                                        LocalDate dateDebut, LocalDate dateFin,
                                        Boolean actif, String situation,
                                        String categorie, String typePrestation,
                                        Pageable pageable) {
        Pageable tri = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Order.desc("dateDemande"), Sort.Order.desc("id")));
        LocalDate aujourdhui = LocalDate.now();
        int pdJours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS,
                ConfigurationKeys.JOURS_RETARD_PRESTATION_DEBOURS_DEFAUT));
        int aboJours = Math.max(0, configurationService.lireEntier(
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT,
                ConfigurationKeys.JOURS_RETARD_ABONNEMENT_DEFAUT));
        java.time.LocalDateTime deadlinePrestationDebours =
                aujourdhui.minusDays(pdJours).atTime(java.time.LocalTime.MAX);
        java.time.LocalDateTime deadlineAbonnement =
                aujourdhui.minusDays(aboJours).atTime(java.time.LocalTime.MAX);

        Page<DemandeService> page = demandeRepository.rechercher(recherche, societeId, serviceId,
                commercialId, statut, dateDebut, dateFin, actif, situation,
                categorie, typePrestation,
                aujourdhui, deadlinePrestationDebours, deadlineAbonnement, tri);

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
                : ligneIds.stream()
                        .collect(Collectors.toMap(
                                id -> id,
                                id -> ligneServiceService.montantPayeParLigne(id)));
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
        BigDecimal montantPaye = paiementsParDemande.getOrDefault(demande.getId(), BigDecimal.ZERO);
        List<LigneService> lignes = lignesParDemande.getOrDefault(demande.getId(), List.of());
        BigDecimal montantTotal = lignes.stream()
                .map(ligneServiceService::montantAttendu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = montantTotal.subtract(montantPaye).max(BigDecimal.ZERO);
        List<LigneServiceResponse> lignesReponse = lignes.stream()
                .map(l -> {
                    BigDecimal attendu = ligneServiceService.montantAttendu(l);
                    BigDecimal paye = paiementsParLigne.getOrDefault(l.getId(), BigDecimal.ZERO);
                    return DemandeMapper.toLigneResponse(l, paye,
                            attendu.subtract(paye).max(BigDecimal.ZERO));
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
                    request.debutService(), request.finService(),
                    LigneServiceService.CATEGORIE_EXTERNALISATION, null, null, null));
        }

        // Validation de la catégorie de chaque ligne (EXTERNALISATION / DEBOURS).
        for (CreateLigneRequest ligne : lignesDemandees) {
            String categorie = LigneServiceService.normaliserCategorie(ligne.categorie());
            if (LigneServiceService.CATEGORIE_DEBOURS.equals(categorie)) {
                if (ligne.designationDebours() == null || ligne.designationDebours().isBlank()) {
                    throw new BusinessException(ErrorMessages.DESIGNATION_DEBOURS_OBLIGATOIRE);
                }
                if (ligne.montantDebours() != null && ligne.montantDebours().signum() < 0) {
                    throw new BusinessException("Le montant du débours doit être positif ou nul");
                }
                if (ligne.serviceId() != null || ligne.tarif() != null
                        || ligne.debutService() != null || ligne.finService() != null) {
                    throw new BusinessException(ErrorMessages.DEBOURS_SERVICE_INTERDIT);
                }
                LigneServiceService.normaliserTypePrestation(
                        categorie, null, ligne.typePrestation());
            } else {
                if (ligne.designationDebours() != null && !ligne.designationDebours().isBlank()) {
                    throw new BusinessException(
                            "La désignation du débours ne s'applique qu'aux lignes DEBOURS");
                }
                if (ligne.montantDebours() != null) {
                    throw new BusinessException(
                            "Le montant du débours ne s'applique qu'aux lignes DEBOURS");
                }
                if (ligne.serviceId() == null || ligne.tarif() == null) {
                    throw new BusinessException(ErrorMessages.DONNEES_OBLIGATOIRES);
                }
                // Validation des dates selon le type de prestation :
                // - PRESTATION : début et fin optionnels (tous deux peuvent être null)
                // - ABONNEMENT_MENSUEL : début obligatoire, fin optionnel
                // - Autre (null) : pas de contrainte ici, validée plus tard dans ajouter()
                if (LigneServiceService.PRESTATION.equalsIgnoreCase(
                        ligne.typePrestation() != null ? ligne.typePrestation() : null)) {
                    // Pas de contrainte de dates pour PRESTATION.
                } else if (LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(
                        ligne.typePrestation() != null ? ligne.typePrestation() : null)) {
                    if (ligne.debutService() == null) {
                        throw new BusinessException(ErrorMessages.DEBUT_SERVICE_OBLIGATOIRE_ABONNEMENT);
                    }
                    // La date fin est optionnelle pour un abonnement mensuel.
                } else {
                    // Cas sans typePrestation précisé : pas de contrainte ici.
                }
            }
        }

        List<CreateLigneRequest> lignesExte = lignesDemandees.stream()
                .filter(l -> !LigneServiceService.CATEGORIE_DEBOURS.equals(
                        LigneServiceService.normaliserCategorie(l.categorie())))
                .toList();

        List<com.cabex.backend.service.entity.Service> servicesDemandes = lignesExte.stream()
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
        if (typeSolo && !lignesDemandees.isEmpty() && lignesDemandees.size() > 1) {
            throw new BusinessException(ErrorMessages.SERVICE_SOLO_CREATION_TRANSACTION);
        }
        if (societe == null && nbCreation == 0) {
            throw new BusinessException(ErrorMessages.DEMANDE_SOCIETE_REQUISE);
        }
        for (CreateLigneRequest ligne : lignesExte) {
            validateDates(ligne.debutService(), ligne.finService());
            if (ligne.tarif().signum() < 0) {
                throw new BusinessException(ErrorMessages.TARIF_POSITIF);
            }
        }
        long distincts = lignesExte.stream().map(CreateLigneRequest::serviceId).distinct().count();
        if (distincts < lignesExte.size()) {
            throw new BusinessException("Un service ne peut pas être sélectionné deux fois dans une demande");
        }

        LocalDate dateDemandeEffective = request.dateDemande() != null
                ? request.dateDemande()
                : LocalDate.now();
        DemandeService demande = DemandeService.builder()
                .referenceDemande(genererReference(dateDemandeEffective))
                .dateDemande(dateDemandeEffective)
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
            String categorie = LigneServiceService.normaliserCategorie(ligne.categorie());
            if (LigneServiceService.CATEGORIE_DEBOURS.equals(categorie)) {
                LigneService creee = LigneService.builder()
                        .demande(demande)
                        .categorie(LigneServiceService.CATEGORIE_DEBOURS)
                        .designationDebours(ligne.designationDebours().trim())
                        .statut(LigneServiceService.LIGNE_EN_ATTENTE)
                        .build();
                // Si un montant est annoncé à la création, le débours est directement
                // considéré validé (aucun « Valider le montant » à faire ensuite) : sa
                // facture validée est créée immédiatement, sans facture proforma.
                BigDecimal montantInitial = ligne.montantDebours();
                if (montantInitial != null) {
                    creee.setMontantDebours(montantInitial);
                    creee.setStatut(LigneServiceService.LIGNE_EN_COURS);
                }
                creee = ligneRepository.save(creee);
                if (montantInitial != null) {
                    // Un débours n'a pas de facture proforma : la facture validée
                    // du débours est créée directement, comme à la « validation
                    // du montant » sur une fiche demande.
                    com.cabex.backend.facture.entity.FactureValidee fvDebours =
                            factureService.creerFactureValideePourDebours(creee, montantInitial);
                    ligneServiceService.ajouterHistoriqueCreation(creee,
                            "Débours créé (montant annoncé : " + montantInitial + ") — "
                                    + creee.getDesignationDebours()
                                    + " — facture validée créée : "
                                    + fvDebours.getReferenceFactureValidee());
                } else {
                    ligneServiceService.ajouterHistoriqueCreation(creee,
                            "Débours ajouté à la création : " + creee.getDesignationDebours());
                }
                continue;
            }
            Service service = serviceRepository.findById(ligne.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Service", ligne.serviceId())));
            String typePrestation = LigneServiceService.normaliserTypePrestation(
                    categorie, service, ligne.typePrestation());
            LigneService creee = LigneService.builder()
                    .demande(demande)
                    .service(service)
                    .tarif(ligne.tarif())
                    .debutService(ligne.debutService())
                    .finService(ligne.finService())
                    .categorie(LigneServiceService.CATEGORIE_EXTERNALISATION)
                    .typePrestation(typePrestation)
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
        // Les statuts de ligne DEBOURS (NON_PAYEE/PARTIEL/PAYEE) ne passent
        // jamais par l'historique de la demande (statut global inchangé).
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
            case "EN_ATTENTE" -> STATUT_VALIDATION.equals(nouveau) || STATUT_EN_COURS.equals(nouveau) || STATUT_ANNULEE.equals(nouveau);
            case "VALIDATION" -> STATUT_EN_COURS.equals(nouveau);
            case "EN_COURS" -> STATUT_TERMINEE.equals(nouveau);
            default -> false;
        };
    }

    /**
     * Garde du passage à VALIDATION : toutes les factures validées et
     * un plan de paiement établi pour chaque ligne EXTERNALISATION
     * (les lignes DEBOURS sont ignorées par cette garde).
     */
    private void verifierPassageValidation(DemandeService demande) {
        List<LigneService> lignes = ligneRepository.findByDemandeId(demande.getId());
        // Seules les lignes EXTERNALISATION STANDARD (pas DEBOURS, pas PRESTATION, pas ABONNEMENT_MENSUEL)
        // nécessitent un plan de paiement et une facture validée.
        List<LigneService> externalisationStandard = lignes.stream()
                .filter(l -> !LigneServiceService.estDebours(l)
                        && !LigneServiceService.PRESTATION.equalsIgnoreCase(l.getTypePrestation())
                        && !LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(l.getTypePrestation()))
                .toList();
        if (externalisationStandard.isEmpty()) {
            throw new BusinessException("Une demande contient au moins un service");
        }
        if (!factureService.toutesValidees(demande.getId())) {
            throw new BusinessException(
                    "Toutes les factures de la demande doivent être validées avant le passage à VALIDATION");
        }
        for (LigneService ligne : externalisationStandard) {
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
        if (LigneServiceService.estDebours(ligne)) {
            return;
        }
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
     * sont TERMINEES (EXTERNALISATION full paiement / abonnement terminée
     * manuellement) ou PAYEE (DEBOURS complet).
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
                .allMatch(l -> LigneServiceService.LIGNE_TERMINE.equals(l.getStatut())
                        || LigneServiceService.LIGNE_PAYEE.equals(l.getStatut()));
        if (!toutesTerminees) {
            return false;
        }
        enviereHistorique(demande, STATUT_TERMINEE,
                "Tous les services sont terminés ou payés (clôture automatique)");
        notificationService.notifierChangementStatut(demande, STATUT_TERMINEE);
        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                Long.valueOf(demande.getId()), actuel, STATUT_TERMINEE);
        return true;
    }

    /**
     * Génère une référence au format DEM-JJ-MM-AAAA-EXT-XXXXX.
     * EXT-XXXXX se réinitialise chaque année (séquence par année).
     * Exemple : 01/01/2026 → DEM-01-01-2026-EXT-00001.
     */
    private String genererReference(LocalDate dateDemande) {
        int annee = dateDemande.getYear();
        String jj = String.format("%02d", dateDemande.getDayOfMonth());
        String mm = String.format("%02d", dateDemande.getMonthValue());
        String aaaa = String.valueOf(annee);
        String prefix = "DEM-" + jj + "" + mm + "" + aaaa + "EXT";
        long seq = demandeRepository.countByAnnee(annee) + 1;
        String reference;
        do {
            reference = prefix + String.format("%05d", seq);
            seq++;
        } while (demandeRepository.existsByReferenceDemande(reference));
        return reference;
    }

    /** Statut actuel de la demande (EN_ATTENTE / VALIDATION / EN_COURS / TERMINEE / ANNULEE). */
    @Transactional(readOnly = true)
    public String statutDemandeActuel(Integer demandeId) {
        return statutActuel(demandeId);
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
