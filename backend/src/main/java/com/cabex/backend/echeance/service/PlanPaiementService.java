package com.cabex.backend.echeance.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.echeance.dto.CreatePlanPaiementRequest;
import com.cabex.backend.echeance.dto.PlanPaiementResponse;
import com.cabex.backend.echeance.dto.UpdatePlanPaiementRequest;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.mapper.EcheanceMapper;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@org.springframework.stereotype.Service
public class PlanPaiementService {

    private static final int JOURS_ALERTE = 3;

    private final ProposPaiementRepository proposRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public PlanPaiementService(ProposPaiementRepository proposRepository,
                               EcheancePaiementRepository echeanceRepository,
                               DemandeServiceRepository demandeRepository,
                               HistoriqueDemandeRepository historiqueRepository,
                               UtilisateurRepository utilisateurRepository,
                               AuditService auditService) {
        this.proposRepository = proposRepository;
        this.echeanceRepository = echeanceRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PlanPaiementResponse getByDemande(Integer demandeId) {
        DemandeService demande = getDemande(demandeId);
        ProposPaiement propos = proposRepository
                .findFirstByDemandeIdAndStatutOrderByCreatedAtDesc(demandeId, "ACTIF")
                .orElseGet(() -> proposRepository.findByDemandeIdOrderByCreatedAtDesc(demandeId)
                        .stream().findFirst().orElse(null));
        if (propos == null) {
            throw new ResourceNotFoundException(
                    "Aucun plan de paiement pour la demande " + demande.getReferenceDemande());
        }
        return toResponse(propos);
    }

    @Transactional
    public PlanPaiementResponse create(Integer demandeId, CreatePlanPaiementRequest request) {
        DemandeService demande = getDemande(demandeId);

        if (proposRepository.existsByDemandeId(demandeId)) {
            throw new BusinessException("Un plan de paiement existe déjà pour cette demande");
        }

        String statutActuel = statutActuel(demandeId);
        if ("TERMINEE".equals(statutActuel) || "ANNULEE".equals(statutActuel)) {
            throw new BusinessException(
                    "Le plan de paiement ne peut pas être créé pour une demande terminée ou annulée");
        }

        if (request.dateFin() != null && request.dateDebut() != null
                && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }

        if (request.dateDebut() != null && demande.getDebutService() != null
                && request.dateDebut().isBefore(demande.getDebutService())) {
            throw new BusinessException(
                    "Le plan de paiement doit commencer après le début du service ("
                            + demande.getDebutService() + ")");
        }
        if (request.dateFin() != null && demande.getFinService() != null
                && request.dateFin().isAfter(demande.getFinService())) {
            throw new BusinessException(
                    "Le plan de paiement doit se terminer avant la fin du service ("
                            + demande.getFinService() + ")");
        }

        ProposPaiement propos = ProposPaiement.builder()
                .demande(demande)
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .montantTotal(request.montantTotal())
                .restePayer(request.montantTotal())
                .nombreEcheances(request.nombreEcheances())
                .commentaire(request.commentaire())
                .statut("ACTIF")
                .createdBy(currentUtilisateur())
                .build();
        propos = proposRepository.save(propos);

        propos.setEcheances(genererEcheances(propos));
        propos = proposRepository.save(propos);

        auditService.log(AuditActions.CREATE, "PAIEMENT", "PlanPaiement",
                Long.valueOf(propos.getId()), null, "demande=" + demande.getReferenceDemande());
        return toResponse(propos);
    }

    @Transactional
    public PlanPaiementResponse update(Integer proposId, UpdatePlanPaiementRequest request) {
        ProposPaiement propos = proposRepository.findById(proposId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Plan de paiement", proposId)));

        if (request.dateFin() != null && request.dateDebut() != null
                && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }

        DemandeService demandeCourante = propos.getDemande();
        if (request.dateDebut() != null && demandeCourante.getDebutService() != null
                && request.dateDebut().isBefore(demandeCourante.getDebutService())) {
            throw new BusinessException(
                    "Le plan de paiement doit commencer après le début du service ("
                            + demandeCourante.getDebutService() + ")");
        }
        if (request.dateFin() != null && demandeCourante.getFinService() != null
                && request.dateFin().isAfter(demandeCourante.getFinService())) {
            throw new BusinessException(
                    "Le plan de paiement doit se terminer avant la fin du service ("
                            + demandeCourante.getFinService() + ")");
        }

        propos.setDateDebut(request.dateDebut());
        propos.setDateFin(request.dateFin());
        propos.setMontantTotal(request.montantTotal());
        propos.setCommentaire(request.commentaire());

        BigDecimal totalPaye = echeanceRepository.findByProposPaiementIdOrderByNumeroEcheanceAsc(proposId).stream()
                .map(EcheancePaiement::getMontantPaye)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        propos.setRestePayer(request.montantTotal().subtract(totalPaye).max(BigDecimal.ZERO));

        ProposPaiement saved = proposRepository.save(propos);
        auditService.log(AuditActions.UPDATE, "PAIEMENT", "PlanPaiement",
                Long.valueOf(saved.getId()), "demande=" + saved.getDemande().getReferenceDemande(),
                "demande=" + saved.getDemande().getReferenceDemande());
        return toResponse(saved);
    }

    private Set<EcheancePaiement> genererEcheances(ProposPaiement propos) {
        int n = propos.getNombreEcheances();
        LocalDate debut = propos.getDateDebut();
        LocalDate fin = propos.getDateFin();
        BigDecimal total = propos.getMontantTotal();

        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        Set<EcheancePaiement> echeances = new HashSet<>();
        long totalJours = Math.max(0, ChronoUnit.DAYS.between(debut, fin));

        for (int i = 0; i < n; i++) {
            BigDecimal montant = (i == n - 1)
                    ? total.subtract(base.multiply(BigDecimal.valueOf(n - 1)))
                    : base;

            LocalDate date = fin;
            if (n > 1) {
                date = debut.plusDays(totalJours * i / (n - 1));
            }

            echeances.add(EcheancePaiement.builder()
                    .proposPaiement(propos)
                    .numeroEcheance(i + 1)
                    .dateEcheance(date)
                    .montantAttendu(montant)
                    .montantPaye(BigDecimal.ZERO)
                    .build());
        }
        return echeances;
    }

    private String statutActuel(Integer demandeId) {
        return historiqueRepository.findFirstByDemandeIdOrderByIdDesc(demandeId)
                .map(HistoriqueDemande::getStatut)
                .map(s -> s.getStatut())
                .orElse(null);
    }

    private PlanPaiementResponse toResponse(ProposPaiement propos) {
        return EcheanceMapper.toPlanResponse(propos, LocalDate.now(), JOURS_ALERTE);
    }

    private DemandeService getDemande(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", id)));
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
