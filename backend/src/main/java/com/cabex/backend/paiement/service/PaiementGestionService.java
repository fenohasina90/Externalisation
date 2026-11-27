package com.cabex.backend.paiement.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.mail.ClientMailNotifier;
import com.cabex.backend.mail.PaiementMailEvent;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.dto.CreatePaiementRequest;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.dto.PaiementResponse;
import com.cabex.backend.paiement.dto.UpdatePaiementRequest;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.mapper.PaiementMapper;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.dto.ObjetLigne;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

@org.springframework.stereotype.Service
public class PaiementGestionService {

    private static final String MODE_VIREMENT = "VIREMENT";
    private static final String MODE_CHEQUE = "CHEQUE";

    private final PaiementRepository paiementRepository;
    private final DemandeServiceRepository demandeRepository;
    private final ModePaiementRepository modeRepository;
    private final FactureValideeRepository factureValideeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DemandeGestionService demandeGestionService;
    private final LigneServiceRepository ligneRepository;
    private final LigneServiceService ligneServiceService;
    private final FactureService factureService;
    private final ApplicationEventPublisher eventPublisher;
    private final ClientMailNotifier clientMailNotifier;

    public PaiementGestionService(PaiementRepository paiementRepository,
                                  DemandeServiceRepository demandeRepository,
                                  ModePaiementRepository modeRepository,
                                  FactureValideeRepository factureValideeRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  NotificationService notificationService,
                                  AuditService auditService,
                                  DemandeGestionService demandeGestionService,
                                  LigneServiceRepository ligneRepository,
                                  LigneServiceService ligneServiceService,
                                  FactureService factureService,
                                  ApplicationEventPublisher eventPublisher,
                                  ClientMailNotifier clientMailNotifier) {
        this.paiementRepository = paiementRepository;
        this.demandeRepository = demandeRepository;
        this.modeRepository = modeRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.demandeGestionService = demandeGestionService;
        this.ligneRepository = ligneRepository;
        this.ligneServiceService = ligneServiceService;
        this.factureService = factureService;
        this.eventPublisher = eventPublisher;
        this.clientMailNotifier = clientMailNotifier;
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> getAll(String recherche, Integer demandeId, Integer modeId,
                                         LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        Pageable tri = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Order.desc("datePaiement"), Sort.Order.desc("id")));
        return paiementRepository.rechercher(recherche, demandeId, modeId, dateDebut, dateFin, tri)
                .map(PaiementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaiementResponse getById(Integer id) {
        return PaiementMapper.toResponse(getEntity(id));
    }

    /** Envoi manuel, depuis l'historique, de la facture liée et du reçu. */
    @Transactional(readOnly = true)
    public void envoyerEmailPaiement(Integer id) {
        Paiement paiement = getEntity(id);
        if (paiement.getFactureValidee() == null || paiement.getDemande() == null) {
            throw new BusinessException(
                    "Ce paiement n'a pas de facture validée ou de demande rattachée.");
        }
        clientMailNotifier.envoyerPaiement(
                paiement.getId(),
                paiement.getReferencePaiement(),
                paiement.getFactureValidee().getId(),
                paiement.getDemande());
    }

    @Transactional(readOnly = true)
    public PaiementDetailResponse getByReference(String reference) {
        Paiement paiement = paiementRepository
                .findByReferencePaiementAndActifTrue(reference.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paiement", reference)));
        BigDecimal somme = paiement.getFactureValidee() != null
                ? paiementRepository.sommePayeeParFactureValidee(paiement.getFactureValidee().getId())
                : BigDecimal.ZERO;
        List<ObjetLigne> objets = paiement.getFactureValidee() != null
                ? factureService.objetsFactureValidee(paiement.getFactureValidee())
                : List.of();
        return PaiementMapper.toDetailResponse(paiement, somme != null ? somme : BigDecimal.ZERO, objets);
    }

    @Transactional(readOnly = true)
    public List<ModePaiementResponse> getModes() {
        return modeRepository.findAll().stream()
                .map(PaiementMapper::toModeResponse)
                .toList();
    }

    @Transactional
    public PaiementResponse create(CreatePaiementRequest request) {
        DemandeService demande = demandeRepository.findById(request.demandeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", request.demandeId())));

        // Une demande TERMINEE ou ANNULEE ne peut plus être payée :
        // elle se consulte seulement (changement de statut manuel).
        String statutDemande = demandeGestionService.statutDemandeActuel(demande.getId());
        if ("TERMINEE".equals(statutDemande) || "ANNULEE".equals(statutDemande)) {
            throw new BusinessException(
                    "La demande est " + statutDemande + " : aucun paiement n'est possible");
        }
        // Aucun paiement en pièce avant création de la facture (demande EN_ATTENTE).
        if ("EN_ATTENTE".equals(statutDemande)) {
            throw new BusinessException(
                    "La demande est EN_ATTENTE : validez la facture avant d'enregistrer un paiement");
        }

        ModePaiement mode = modeRepository.findById(request.modePaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Mode de paiement", request.modePaiementId())));

        // Paiement toujours rattaché à une facture validée.
        com.cabex.backend.facture.entity.FactureValidee factureValidee =
                factureValideeRepository.findById(request.factureValideeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorMessages.introuvable("Facture validée", request.factureValideeId())));

        // Mensuelle 1 : la facture validée devient la facture du 1er mois dès
        // la création de la facture du mois (mois choisi). Avant ça,Aucun paiement.
        boolean demandeAvecAbonnement = false;
        for (LigneService ligneDemande : ligneRepository.findByDemandeId(demande.getId())) {
            if (LigneServiceService.CATEGORIE_EXTERNALISATION.equalsIgnoreCase(ligneDemande.getCategorie())
                    && LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(ligneDemande.getTypePrestation())) {
                demandeAvecAbonnement = true;
                break;
            }
        }
        boolean mensuelleCreee = factureValideeRepository.findByDemandeIdAndMoisIsNotNull(demande.getId())
                .stream().findAny().isPresent();
        if (demandeAvecAbonnement && !mensuelleCreee) {
            throw new BusinessException(
                    "La facture du mois n'est pas encore créée pour cette demande : "
                            + "créez la facture du mois (onglet Factures mensuelles) avant d'enregistrer un paiement");
        }

        if (factureValidee.getDemande() == null
                || factureValidee.getDemande().getId() == null
                || !factureValidee.getDemande().getId().equals(demande.getId())) {
            throw new BusinessException(
                    "La facture validée n'appartient pas à la demande sélectionnée");
        }

        // Pour un débours (facture validée créée à la validation du montant),
        // la facture porte la ligne de débours : elle en fait foi.
        if (factureValidee.getFactureProforma() == null
                && factureValidee.getFactureGenerale() == null
                && factureValidee.getLigneService() != null) {
            LigneService ligneDebours = factureValidee.getLigneService();
            if (!LigneServiceService.estDebours(ligneDebours)
                    || ligneDebours.getMontantDebours() == null) {
                throw new BusinessException(ErrorMessages.DEBOURS_MONTANT_NON_VALIDE);
            }
        } else if (factureValidee.getFactureProforma() == null
                && factureValidee.getFactureGenerale() == null) {
            // Ancienne donnée / facture sans proforma ni ligne : contrôle de repli.
            LigneService ligneDebours = ligneRepository.findByDemandeId(demande.getId())
                    .stream()
                    .filter(l -> LigneServiceService.estDebours(l))
                    .findFirst().orElse(null);
            if (ligneDebours == null || ligneDebours.getMontantDebours() == null) {
                throw new BusinessException(ErrorMessages.DEBOURS_MONTANT_NON_VALIDE);
            }
        }

        // Le montant payé ne peut pas dépasser le montant de la facture validée
        // (moins ce déjà payé sur cette facture).
        validerReferencesMode(mode, request.referenceVirement(),
                request.banque(), request.numeroCheque());
        validerMontantFactureValidee(factureValidee, request.montantPaye(), BigDecimal.ZERO);

        Paiement paiement = Paiement.builder()
                .referencePaiement(genererReference(LocalDate.now().getYear()))
                .demande(demande)
                .factureValidee(factureValidee)
                .modePaiement(mode)
                .datePaiement(request.datePaiement())
                .montantPaye(request.montantPaye())
                .referenceVirement(request.referenceVirement())
                .banque(trimOuNull(request.banque()))
                .numeroCheque(trimOuNull(request.numeroCheque()))
                .commentaire(request.commentaire())
                .pieceJustificative(request.pieceJustificative())
                .createdBy(currentUtilisateur())
                .build();
        paiement = paiementRepository.save(paiement);
        // Figer le moment des snapshots :
        // - montant déjà payé : somme des paiements AVANT celui-ci (ce paiement exclu)
        // - reste à payer : montant de la facture moins TOUS les paiements (celui-ci inclus)
        if (factureValidee != null) {
            BigDecimal sommeApres = paiementRepository.sommePayeeParFactureValidee(factureValidee.getId());
            if (sommeApres == null) {
                sommeApres = BigDecimal.ZERO;
            }
            BigDecimal sommeAvant = sommeApres.subtract(
                    paiement.getMontantPaye() != null ? paiement.getMontantPaye() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            BigDecimal total = montantValideFactureValidee(factureValidee);
            paiement.setMontantDejaPaye(sommeAvant);
            paiement.setResteAPayer(total.subtract(sommeApres).max(BigDecimal.ZERO));
            paiement = paiementRepository.save(paiement);
        }

        // Mettre à jour le statut des lignes liées (la facture peut couvrir
        // plusieurs lignes d'une même société).
        for (LigneService ligneLiee : lignePourFacturesValidees(factureValidee, demande)) {
            ligneServiceService.actualiserStatutApresPaiement(ligneLiee);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(ligneLiee.getDemande().getId());
        }

        notificationService.notifierPaiementEnregistre(paiement);

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(paiement.getId()), null,
                paiement.getMontantPaye() + " : " + paiement.getReferencePaiement());

        // E-mail au client en arrière-plan, uniquement après validation de la transaction :
        // l'API répond immédiatement et l'encaissement n'est jamais ralenti par SMTP.
        eventPublisher.publishEvent(new PaiementMailEvent(
                paiement.getId(),
                paiement.getReferencePaiement(),
                factureValidee.getId(),
                demande.getId()));

        return PaiementMapper.toResponse(paiement);
    }

    @Transactional
    public PaiementResponse update(Integer id, UpdatePaiementRequest request) {
        Paiement paiement = getEntity(id);
        DemandeService demande = paiement.getDemande();
        ModePaiement mode = paiement.getModePaiement();

        // Le paiement est toujours rattaché à une facture validée.
        com.cabex.backend.facture.entity.FactureValidee factureValidee = paiement.getFactureValidee();
        if (factureValidee == null) {
            throw new BusinessException(ErrorMessages.PAIEMENT_SANS_FACTURE_VALIDEE);
        }

        if (request.factureValideeId() != null
                && !request.factureValideeId().equals(factureValidee.getId())) {
            throw new BusinessException(
                    "Un paiement ne peut pas être déplacé vers une autre facture validée");
        }

        validerReferencesMode(mode, request.referenceVirement(),
                request.banque(), request.numeroCheque());
        validerMontantFactureValidee(factureValidee, request.montantPaye(), paiement.getMontantPaye());

        paiement.setDatePaiement(request.datePaiement());
        paiement.setMontantPaye(request.montantPaye());
        paiement.setReferenceVirement(request.referenceVirement());
        paiement.setBanque(trimOuNull(request.banque()));
        paiement.setNumeroCheque(trimOuNull(request.numeroCheque()));
        paiement.setCommentaire(request.commentaire());

        Paiement saved = paiementRepository.save(paiement);
        if (factureValidee != null) {
            BigDecimal sommeApres = paiementRepository.sommePayeeParFactureValidee(factureValidee.getId());
            if (sommeApres == null) {
                sommeApres = BigDecimal.ZERO;
            }
            BigDecimal sommeAvant = sommeApres.subtract(
                    saved.getMontantPaye() != null ? saved.getMontantPaye() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            BigDecimal total = montantValideFactureValidee(factureValidee);
            saved.setMontantDejaPaye(sommeAvant);
            saved.setResteAPayer(total.subtract(sommeApres).max(BigDecimal.ZERO));
            saved = paiementRepository.save(saved);
        }
        PaiementResponse response = PaiementMapper.toResponse(saved);

        // Mettre à jour le statut des lignes liées.
        for (LigneService ligneLiee : lignePourFacturesValidees(factureValidee, demande)) {
            ligneServiceService.actualiserStatutApresPaiement(ligneLiee);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(ligneLiee.getDemande().getId());
        }

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                paiement.getId().longValue(), paiement.getMontantPaye()
                        + " : " + paiement.getReferencePaiement(),
                request.montantPaye()
                        + (request.referenceVirement() != null ? " : " + request.referenceVirement() : ""));
        return response;
    }

    @Transactional
    public void delete(Integer id) {
        Paiement paiement = getEntity(id);
        String ref = paiement.getReferencePaiement();
        paiement.setActif(false);
        paiement.setArchivedAt(LocalDateTime.now());
        paiementRepository.save(paiement);

        // Mettre à jour le statut des lignes liées.
        for (LigneService ligneLiee : lignePourFacturesValidees(paiement.getFactureValidee(),
                paiement.getDemande())) {
            ligneServiceService.actualiserStatutApresPaiement(ligneLiee);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(ligneLiee.getDemande().getId());
        }

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(id), "supprimé (archivé) : " + ref, null);
    }

    /**
     * Lignes EXTERNALISATION ou DEBOURS concernées par la facture validée :
     * toutes celles portées par la facture combinée (facture du mois multi-lignes),
     * sinon la ligne de la proforma (facture générale) ou la ligne de débours.
     */
    private List<LigneService> lignePourFacturesValidees(com.cabex.backend.facture.entity.FactureValidee fv,
                                                         DemandeService demande) {
        if (fv == null) {
            return List.of();
        }
        // Facture combinée du mois (mensuelle 1 et factures suivantes) : seul le
        // règlement des lignes PRINCIPALE est imputé aux lignes — les lignes
        // RAPPEL ne servent que d'information.
        if (fv.getFactureGenerale() != null) {
            if (fv.getLignes() != null && !fv.getLignes().isEmpty()) {
                return fv.getLignes().stream()
                        .filter(l -> !"RAPPEL".equalsIgnoreCase(l.getType()))
                        .map(com.cabex.backend.facture.entity.FactureValideeLigne::getLigneService)
                        .filter(java.util.Objects::nonNull)
                        .toList();
            }
            return fv.getLigneService() != null ? List.of(fv.getLigneService()) : List.of();
        }
        List<LigneService> lignes = new ArrayList<>();
        if (fv.getLignes() != null && !fv.getLignes().isEmpty()) {
            fv.getLignes().stream()
                    .filter(l -> !"RAPPEL".equalsIgnoreCase(l.getType()))
                    .map(com.cabex.backend.facture.entity.FactureValideeLigne::getLigneService)
                    .filter(java.util.Objects::nonNull)
                    .forEach(lignes::add);
            return lignes;
        }
        LigneService unique = lignePourFactureValidee(fv, demande);
        // Facture générale sans lignes persistées (demande non abonnée, mode UNIQUE) :
        // toutes les lignes du proforma de cette demande sont concernées par le paiement.
        if (fv.getFactureProforma() != null && fv.getFactureProforma().getLignes() != null
                && demande != null) {
            List<LigneService> lignesProforma = fv.getFactureProforma().getLignes().stream()
                    .filter(l -> l.getDemande() != null
                            && l.getDemande().getId().equals(demande.getId()))
                    .toList();
            if (!lignesProforma.isEmpty()) {
                return lignesProforma;
            }
        }
        return unique != null ? List.of(unique) : List.of();
    }

    /**
     * Ligne EXTERNALISATION ou DEBOURS concernée par la facture validée.
     * Débours / abonnement mensuel : la facture porte la ligne directement.
     * Sinon (facture générale issue d'une proforma) : ligne de la proforma
     * liée à cette demande.
     */
    private LigneService lignePourFactureValidee(com.cabex.backend.facture.entity.FactureValidee fv,
                                                 DemandeService demande) {
        if (fv == null) {
            return null;
        }
        if (fv.getLigneService() != null) {
            return fv.getLigneService();
        }
        com.cabex.backend.facture.entity.FactureValidee generale = fv.getFactureGenerale();
        if (fv.getFactureProforma() == null && generale != null && generale.getLigneService() != null) {
            return generale.getLigneService();
        }
        if (fv.getFactureProforma() != null && demande != null) {
            return fv.getFactureProforma().getLignes().stream()
                    .filter(l -> l.getDemande() != null
                            && l.getDemande().getId().equals(demande.getId()))
                    .findFirst().orElse(null);
        }
        // Débours sans ligne rattachée (ancienne facture sans lien) : première ligne débours.
        if (demande != null) {
            return ligneRepository.findByDemandeId(demande.getId()).stream()
                    .filter(LigneServiceService::estDebours)
                    .findFirst().orElse(null);
        }
        return null;
    }

    private void validerReferencesMode(ModePaiement mode, String referenceVirement,
                                       String banque, String numeroCheque) {
        if (MODE_VIREMENT.equalsIgnoreCase(mode.getCode())
                && (referenceVirement == null || referenceVirement.isBlank())) {
            throw new BusinessException(
                    "La référence du virement est obligatoire pour un paiement par virement");
        }
        if (MODE_CHEQUE.equalsIgnoreCase(mode.getCode())) {
            if (banque == null || banque.isBlank()) {
                throw new BusinessException(
                        "La banque est obligatoire pour un paiement par chèque");
            }
            if (numeroCheque == null || numeroCheque.isBlank()) {
                throw new BusinessException(
                        "Le numéro de chèque est obligatoire pour un paiement par chèque");
            }
        }
    }

    /**
     * Plafond de la facture validée : montant de la facture moins somme déjà payée
     * sur cette facture (+ l'ancien montant si on modifie un paiement existant).
     */
    private void validerMontantFactureValidee(com.cabex.backend.facture.entity.FactureValidee facture,
                                              BigDecimal montant, BigDecimal ancienMontant) {
        if (montant == null || montant.signum() <= 0) {
            throw new BusinessException("Le montant doit être supérieur à 0");
        }
        // Pour un débours (sans facture proforma), le montant de la facture validée
        // est le montant du débours. S'il est nul, le débours n'est pas encore initialisé.
        if (facture.getFactureProforma() == null && facture.getMontant() == null) {
            throw new BusinessException(ErrorMessages.DEBOURS_MONTANT_NON_VALIDE);
        }
        BigDecimal dejaPayer = paiementRepository.sommePayeeParFactureValidee(facture.getId());
        if (dejaPayer == null) {
            dejaPayer = BigDecimal.ZERO;
        }
        BigDecimal total = facture.getMontant() != null ? facture.getMontant() : BigDecimal.ZERO;
        BigDecimal reste = total.subtract(dejaPayer)
                .add(ancienMontant != null ? ancienMontant : BigDecimal.ZERO);
        if (montant.compareTo(reste) > 0) {
            throw new BusinessException(
                    "Le montant ne peut pas dépasser le reste à payer (" + reste + ")");
        }
    }

    private String genererReference(int annee) {
        String prefix = "PAY-" + annee + "-";
        long seq = paiementRepository.count() + 1;
        String reference;
        do {
            reference = prefix + String.format("%05d", seq);
            seq++;
        } while (paiementRepository.existsByReferencePaiement(reference));
        return reference;
    }

    /** Montant de la facture validée (facture générale / enfant / mensuel). */
    private static BigDecimal montantValideFactureValidee(
            com.cabex.backend.facture.entity.FactureValidee fv) {
        return fv != null && fv.getMontant() != null ? fv.getMontant() : BigDecimal.ZERO;
    }

    private static String trimOuNull(String valeur) {
        if (valeur == null) {
            return null;
        }
        String trim = valeur.trim();
        return trim.isEmpty() ? null : trim;
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Paiement getEntity(Integer id) {
        return paiementRepository.findByIdAndActifTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paiement", id)));
    }
}
