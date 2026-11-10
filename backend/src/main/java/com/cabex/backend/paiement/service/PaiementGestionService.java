package com.cabex.backend.paiement.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
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
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@org.springframework.stereotype.Service
public class PaiementGestionService {

    private static final String MODE_VIREMENT = "VIREMENT";
    private static final String MODE_CHEQUE = "CHEQUE";

    private final PaiementRepository paiementRepository;
    private final DemandeServiceRepository demandeRepository;
    private final ModePaiementRepository modeRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final ProposPaiementRepository proposRepository;
    private final FactureValideeRepository factureValideeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DemandeGestionService demandeGestionService;
    private final LigneServiceRepository ligneRepository;
    private final LigneServiceService ligneServiceService;

    public PaiementGestionService(PaiementRepository paiementRepository,
                                  DemandeServiceRepository demandeRepository,
                                  ModePaiementRepository modeRepository,
                                  EcheancePaiementRepository echeanceRepository,
                                  ProposPaiementRepository proposRepository,
                                  FactureValideeRepository factureValideeRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  NotificationService notificationService,
                                  AuditService auditService,
                                  DemandeGestionService demandeGestionService,
                                  LigneServiceRepository ligneRepository,
                                  LigneServiceService ligneServiceService) {
        this.paiementRepository = paiementRepository;
        this.demandeRepository = demandeRepository;
        this.modeRepository = modeRepository;
        this.echeanceRepository = echeanceRepository;
        this.proposRepository = proposRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.demandeGestionService = demandeGestionService;
        this.ligneRepository = ligneRepository;
        this.ligneServiceService = ligneServiceService;
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

    @Transactional(readOnly = true)
    public PaiementDetailResponse getByReference(String reference) {
        Paiement paiement = paiementRepository
                .findByReferencePaiementAndActifTrue(reference.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paiement", reference)));
        BigDecimal somme = paiement.getFactureValidee() != null
                ? paiementRepository.sommePayeeParFactureValidee(paiement.getFactureValidee().getId())
                : BigDecimal.ZERO;
        return PaiementMapper.toDetailResponse(paiement, somme != null ? somme : BigDecimal.ZERO);
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

        ModePaiement mode = modeRepository.findById(request.modePaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Mode de paiement", request.modePaiementId())));

        // Paiement toujours rattaché à une facture validée.
        com.cabex.backend.facture.entity.FactureValidee factureValidee =
                factureValideeRepository.findById(request.factureValideeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorMessages.introuvable("Facture validée", request.factureValideeId())));

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

        // Échéance réglée (EXTERNALISATION avec plan de paiement) : elle doit
        // appartenir à la demande et correspondre à la facture validée payée.
        EcheancePaiement echeance = null;
        if (request.echeanceId() != null) {
            echeance = echeanceRepository.findById(request.echeanceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Échéance", request.echeanceId())));
            ProposPaiement propos = echeance.getProposPaiement();
            if (propos == null || propos.getDemande() == null
                    || !propos.getDemande().getId().equals(demande.getId())) {
                throw new BusinessException("L'échéance n'appartient pas à cette demande");
            }
            LigneService ligneEcheance = propos.getLigneService();
            boolean factureCorrespond = false;
            if (ligneEcheance != null && ligneEcheance.getFacture() != null) {
                var fvLigne = factureValideeRepository
                        .findByFactureProformaId(ligneEcheance.getFacture().getId())
                        .orElse(null);
                factureCorrespond = fvLigne != null && fvLigne.getId().equals(factureValidee.getId());
            }
            if (!factureCorrespond) {
                throw new BusinessException(
                        "L'échéance ne correspond pas à la facture validée sélectionnée");
            }
            BigDecimal dejaSurEcheance = echeance.getMontantPaye() != null
                    ? echeance.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal resteEcheance = echeance.getMontantAttendu().subtract(dejaSurEcheance);
            if (request.montantPaye() != null
                    && request.montantPaye().compareTo(resteEcheance) > 0) {
                throw new BusinessException(
                        "Le montant dépasse le reste de l'échéance (" + resteEcheance + ")");
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
                .echeance(echeance)
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

        // Solder l'échéance réglée (le statut PARTIELLE/PAYEE est recalculé
        // par les lecteurs à partir de montant_paye).
        if (echeance != null) {
            BigDecimal dejaSurEcheance = echeance.getMontantPaye() != null
                    ? echeance.getMontantPaye() : BigDecimal.ZERO;
            echeance.setMontantPaye(dejaSurEcheance.add(request.montantPaye()));
            echeanceRepository.save(echeance);
            ProposPaiement propos = echeance.getProposPaiement();
            if (propos != null && propos.getRestePayer() != null) {
                propos.setRestePayer(propos.getRestePayer().subtract(request.montantPaye())
                        .max(BigDecimal.ZERO));
                proposRepository.save(propos);
            }
        }

        // Mettre à jour le statut de la ligne de service liée si c'est un paiement
        // sur une ligne EXTERNALISATION ou DEBOURS.
        LigneService ligneService = lignePourFactureValidee(factureValidee, demande);
        if (ligneService != null) {
            ligneServiceService.actualiserStatutApresPaiement(ligneService);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(demande.getId());
        }

        notificationService.notifierPaiementEnregistre(paiement);

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(paiement.getId()), null,
                paiement.getMontantPaye() + " : " + paiement.getReferencePaiement());

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

        // Échéance réglée : ajuster le montant payé de l'échéance selon le delta.
        EcheancePaiement echeance = paiement.getEcheance();
        if (echeance != null) {
            BigDecimal ancien = paiement.getMontantPaye() != null ? paiement.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal nouveau = request.montantPaye() != null ? request.montantPaye() : BigDecimal.ZERO;
            BigDecimal delta = nouveau.subtract(ancien);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal dejaSurEcheance = echeance.getMontantPaye() != null
                        ? echeance.getMontantPaye() : BigDecimal.ZERO;
                BigDecimal nouveauEcheance = dejaSurEcheance.add(delta);
                if (nouveauEcheance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException(
                            "Le montant réduit ne peut pas rendre le payé de l'échéance négatif");
                }
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal resteEcheance = echeance.getMontantAttendu().subtract(dejaSurEcheance);
                    if (delta.compareTo(resteEcheance) > 0) {
                        throw new BusinessException(
                                "Le montant dépasse le reste de l'échéance (" + resteEcheance + ")");
                    }
                }
                echeance.setMontantPaye(nouveauEcheance);
                echeanceRepository.save(echeance);
                ProposPaiement propos = echeance.getProposPaiement();
                if (propos != null && propos.getRestePayer() != null) {
                    propos.setRestePayer(propos.getRestePayer().subtract(delta).max(BigDecimal.ZERO));
                    proposRepository.save(propos);
                }
            }
        }

        paiement.setDatePaiement(request.datePaiement());
        paiement.setMontantPaye(request.montantPaye());
        paiement.setReferenceVirement(request.referenceVirement());
        paiement.setBanque(trimOuNull(request.banque()));
        paiement.setNumeroCheque(trimOuNull(request.numeroCheque()));
        paiement.setCommentaire(request.commentaire());

        PaiementResponse response = PaiementMapper.toResponse(paiementRepository.save(paiement));

        // Mettre à jour le statut de la ligne liée si nécessaire.
        LigneService ligneService = lignePourFactureValidee(factureValidee, demande);
        if (ligneService != null) {
            ligneServiceService.actualiserStatutApresPaiement(ligneService);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(demande.getId());
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

        // Rendre la part d'échéance payée par ce paiement (le paiement est archivé).
        EcheancePaiement echeance = paiement.getEcheance();
        if (echeance != null) {
            BigDecimal montant = paiement.getMontantPaye() != null
                    ? paiement.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal dejaSurEcheance = echeance.getMontantPaye() != null
                    ? echeance.getMontantPaye() : BigDecimal.ZERO;
            echeance.setMontantPaye(dejaSurEcheance.subtract(montant).max(BigDecimal.ZERO));
            echeanceRepository.save(echeance);
            ProposPaiement propos = echeance.getProposPaiement();
            if (propos != null && propos.getRestePayer() != null) {
                BigDecimal nouveauReste = propos.getRestePayer().add(montant);
                propos.setRestePayer(propos.getMontantTotal() != null
                        ? nouveauReste.min(propos.getMontantTotal()) : nouveauReste);
                proposRepository.save(propos);
            }
        }

        // Mettre à jour le statut de la ligne liée.
        LigneService ligneService = lignePourFactureValidee(paiement.getFactureValidee(),
                paiement.getDemande());
        if (ligneService != null) {
            ligneServiceService.actualiserStatutApresPaiement(ligneService);
            // La demande passe TERMINEE quand toutes ses lignes EXTERNALISATION sont soldées.
            demandeGestionService.cloturerSiTerminee(paiement.getDemande().getId());
        }

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(id), "supprimé (archivé) : " + ref, null);
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
