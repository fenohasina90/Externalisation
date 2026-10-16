package com.cabex.backend.paiement.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.service.DemandeGestionService;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.paiement.dto.CreatePaiementRequest;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.PaiementResponse;
import com.cabex.backend.paiement.dto.UpdatePaiementRequest;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.mapper.PaiementMapper;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@org.springframework.stereotype.Service
public class PaiementGestionService {

    private static final String MODE_VIREMENT = "VIREMENT";

    private final PaiementRepository paiementRepository;
    private final DemandeServiceRepository demandeRepository;
    private final ModePaiementRepository modeRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DemandeGestionService demandeGestionService;

    public PaiementGestionService(PaiementRepository paiementRepository,
                                  DemandeServiceRepository demandeRepository,
                                  ModePaiementRepository modeRepository,
                                  EcheancePaiementRepository echeanceRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  NotificationService notificationService,
                                  AuditService auditService,
                                  DemandeGestionService demandeGestionService) {
        this.paiementRepository = paiementRepository;
        this.demandeRepository = demandeRepository;
        this.modeRepository = modeRepository;
        this.echeanceRepository = echeanceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.demandeGestionService = demandeGestionService;
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> getAll(String recherche, Integer demandeId, Integer modeId,
                                         LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        return paiementRepository.rechercher(recherche, demandeId, modeId, dateDebut, dateFin, pageable)
                .map(PaiementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaiementResponse getById(Integer id) {
        return PaiementMapper.toResponse(getEntity(id));
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

        ModePaiement mode = modeRepository.findById(request.modePaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Mode de paiement", request.modePaiementId())));

        EcheancePaiement echeance = null;
        if (request.echeanceId() != null) {
            echeance = chargerEcheanceDeDemande(request.echeanceId(), demande);
        } else {
            throw new BusinessException(
                    "L'échéance est obligatoire : rattachez le paiement à une échéance du service concerné");
        }

        validerVirement(mode, request.referenceVirement());
        validerMontant(demande, echeance, request.montantPaye(), BigDecimal.ZERO, null);
        validerDatePaiement(ligneDe(echeance), request.datePaiement());

        Paiement paiement = Paiement.builder()
                .referencePaiement(genererReference(LocalDate.now().getYear()))
                .demande(demande)
                .echeance(echeance)
                .modePaiement(mode)
                .datePaiement(request.datePaiement())
                .montantPaye(request.montantPaye())
                .referenceVirement(request.referenceVirement())
                .commentaire(request.commentaire())
                .pieceJustificative(request.pieceJustificative())
                .createdBy(currentUtilisateur())
                .build();
        paiement = paiementRepository.save(paiement);

        ajouterAEcheance(echeance, request.montantPaye());

        demandeGestionService.promouvoirEnCoursParLigne(ligneDe(echeance),
                "paiement " + paiement.getReferencePaiement()
                        + " (échéance n°" + echeance.getNumeroEcheance() + ")");

        notificationService.notifierPaiementEnregistre(paiement);

        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(paiement.getId()), null,
                "montant=" + paiement.getMontantPaye() + ", réf=" + paiement.getReferencePaiement());

        return PaiementMapper.toResponse(paiement);
    }

    @Transactional
    public PaiementResponse update(Integer id, UpdatePaiementRequest request) {
        Paiement paiement = getEntity(id);
        DemandeService demande = paiement.getDemande();
        ModePaiement mode = paiement.getModePaiement();

        EcheancePaiement nouvelleEcheance = null;
        if (request.echeanceId() != null) {
            nouvelleEcheance = chargerEcheanceDeDemande(request.echeanceId(), demande);
        }

        validerVirement(mode, request.referenceVirement());
        validerMontant(demande, nouvelleEcheance, request.montantPaye(),
                paiement.getMontantPaye(), paiement.getEcheance());
        validerDatePaiement(
                nouvelleEcheance != null ? ligneDe(nouvelleEcheance) : ligneDe(paiement.getEcheance()),
                request.datePaiement());

        if (paiement.getEcheance() != null) {
            soustraireDeEcheance(paiement.getEcheance(), paiement.getMontantPaye());
        }
        ajouterAEcheance(nouvelleEcheance, request.montantPaye());

        paiement.setEcheance(nouvelleEcheance);
        paiement.setDatePaiement(request.datePaiement());
        paiement.setMontantPaye(request.montantPaye());
        paiement.setReferenceVirement(request.referenceVirement());
        paiement.setCommentaire(request.commentaire());

        PaiementResponse response = PaiementMapper.toResponse(paiementRepository.save(paiement));
        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                paiement.getId().longValue(), "montant=" + paiement.getMontantPaye()
                        + ", réf=" + paiement.getReferencePaiement(),
                "montant=" + request.montantPaye()
                        + (request.referenceVirement() != null ? ", réf=" + request.referenceVirement() : ""));
        return response;
    }

    @Transactional
    public void delete(Integer id) {
        Paiement paiement = getEntity(id);
        if (paiement.getEcheance() != null) {
            soustraireDeEcheance(paiement.getEcheance(), paiement.getMontantPaye());
        }
        String ref = paiement.getReferencePaiement();
        paiement.setActif(false);
        paiement.setArchivedAt(LocalDateTime.now());
        paiementRepository.save(paiement);
        auditService.log(AuditActions.PAYMENT, "PAIEMENT", "Paiement",
                Long.valueOf(id), "supprimé (archivé), réf=" + ref, null);
    }

    private void validerVirement(ModePaiement mode, String referenceVirement) {
        if (MODE_VIREMENT.equalsIgnoreCase(mode.getCode())
                && (referenceVirement == null || referenceVirement.isBlank())) {
            throw new BusinessException(
                    "La référence du virement est obligatoire pour un paiement par virement");
        }
    }

    private EcheancePaiement chargerEcheanceDeDemande(Integer echeanceId, DemandeService demande) {
        EcheancePaiement echeance = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Échéance", echeanceId)));
        if (echeance.getProposPaiement() == null
                || echeance.getProposPaiement().getDemande() == null
                || !echeance.getProposPaiement().getDemande().getId().equals(demande.getId())) {
            throw new BusinessException(
                    "L'échéance n'appartient pas à la demande sélectionnée");
        }
        return echeance;
    }

    private LigneService ligneDe(EcheancePaiement echeance) {
        if (echeance == null || echeance.getProposPaiement() == null) {
            return null;
        }
        return echeance.getProposPaiement().getLigneService();
    }

    private void validerDatePaiement(LigneService ligne, LocalDate date) {
        LocalDate debut = ligne != null ? ligne.getDebutService() : null;
        LocalDate fin = ligne != null ? ligne.getFinService() : null;
        if (date != null && debut != null && fin != null
                && (date.isBefore(debut) || date.isAfter(fin))) {
            throw new BusinessException(
                    "Le paiement doit être effectué entre le début et la fin du service ("
                            + debut + " → " + fin + ")");
        }
    }

    private void validerMontant(DemandeService demande, EcheancePaiement echeance,
                                BigDecimal montant, BigDecimal ancienMontant,
                                EcheancePaiement ancienneEcheance) {
        if (montant.signum() <= 0) {
            throw new BusinessException("Le montant doit être supérieur à 0");
        }
        BigDecimal reste;
        if (echeance != null) {
            BigDecimal libere = (ancienneEcheance != null
                    && ancienneEcheance.getId().equals(echeance.getId()))
                    ? ancienMontant : BigDecimal.ZERO;
            reste = echeance.getMontantAttendu().subtract(echeance.getMontantPaye()).add(libere);
        } else {
            // Paiements historiques sans échéance : plafond au total des lignes.
            BigDecimal dejaPayer = paiementRepository.sommePayeeParDemande(demande.getId());
            if (dejaPayer == null) {
                dejaPayer = BigDecimal.ZERO;
            }
            BigDecimal total = demande.getLignes() != null
                    ? demande.getLignes().stream()
                            .map(l -> l.getTarif() != null ? l.getTarif() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            reste = total.subtract(dejaPayer).add(ancienMontant);
        }
        if (montant.compareTo(reste) > 0) {
            throw new BusinessException(
                    "Le montant ne peut pas dépasser le reste à payer (" + reste + ")");
        }
    }

    private void ajouterAEcheance(EcheancePaiement echeance, BigDecimal montant) {
        if (echeance == null) {
            return;
        }
        echeance.setMontantPaye(echeance.getMontantPaye().add(montant));
        echeanceRepository.save(echeance);
    }

    private void soustraireDeEcheance(EcheancePaiement echeance, BigDecimal montant) {
        if (echeance == null) {
            return;
        }
        BigDecimal nouveau = echeance.getMontantPaye().subtract(montant).max(BigDecimal.ZERO);
        echeance.setMontantPaye(nouveau);
        echeanceRepository.save(echeance);
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
