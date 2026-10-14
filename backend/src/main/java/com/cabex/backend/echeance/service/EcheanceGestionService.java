package com.cabex.backend.echeance.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.echeance.dto.CreateEcheanceRequest;
import com.cabex.backend.echeance.dto.EcheanceResponse;
import com.cabex.backend.echeance.dto.UpdateEcheanceRequest;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.mapper.EcheanceMapper;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
public class EcheanceGestionService {

    private static final int JOURS_ALERTE = 3;

    private final EcheancePaiementRepository echeanceRepository;
    private final ProposPaiementRepository proposRepository;
    private final AuditService auditService;

    public EcheanceGestionService(EcheancePaiementRepository echeanceRepository,
                                  ProposPaiementRepository proposRepository,
                                  AuditService auditService) {
        this.echeanceRepository = echeanceRepository;
        this.proposRepository = proposRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EcheanceResponse> getByPlan(Integer proposId) {
        getPropos(proposId);
        return echeanceRepository.findByProposPaiementIdOrderByNumeroEcheanceAsc(proposId).stream()
                .map(e -> EcheanceMapper.toEcheanceResponse(e, LocalDate.now(), JOURS_ALERTE))
                .toList();
    }

    @Transactional
    public EcheanceResponse create(Integer proposId, CreateEcheanceRequest request) {
        ProposPaiement propos = getPropos(proposId);

        if (echeanceRepository.existsByProposPaiementIdAndNumeroEcheance(
                proposId, request.numeroEcheance())) {
            throw new BusinessException(
                    "L'échéance n°" + request.numeroEcheance() + " existe déjà pour ce plan");
        }

        validerDateDansIntervalle(propos, request.dateEcheance());

        EcheancePaiement echeance = EcheancePaiement.builder()
                .proposPaiement(propos)
                .numeroEcheance(request.numeroEcheance())
                .dateEcheance(request.dateEcheance())
                .montantAttendu(request.montantAttendu())
                .montantPaye(BigDecimal.ZERO)
                .commentaire(request.commentaire())
                .build();
        echeance = echeanceRepository.save(echeance);

        recalculerPlan(propos);

        auditService.log(AuditActions.CREATE, "PAIEMENT", "EcheancePaiement",
                Long.valueOf(echeance.getId()), null,
                "plan=" + propos.getId() + ", echeance=" + echeance.getNumeroEcheance());
        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), JOURS_ALERTE);
    }

    @Transactional
    public EcheanceResponse update(Integer echeanceId, UpdateEcheanceRequest request) {
        EcheancePaiement echeance = getEcheance(echeanceId);

        if (echeance.getMontantPaye().compareTo(request.montantAttendu()) > 0) {
            throw new BusinessException(
                    "Le montant attendu ne peut pas être inférieur au montant déjà payé");
        }

        validerDateDansIntervalle(echeance.getProposPaiement(), request.dateEcheance());

        echeance.setDateEcheance(request.dateEcheance());
        echeance.setMontantAttendu(request.montantAttendu());
        echeance.setCommentaire(request.commentaire());
        echeance = echeanceRepository.save(echeance);

        recalculerPlan(echeance.getProposPaiement());

        auditService.log(AuditActions.UPDATE, "PAIEMENT", "EcheancePaiement",
                Long.valueOf(echeance.getId()),
                "plan=" + echeance.getProposPaiement().getId(),
                "plan=" + echeance.getProposPaiement().getId()
                        + ", echeance=" + echeance.getNumeroEcheance());
        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), JOURS_ALERTE);
    }

    @Transactional(readOnly = true)
    public Page<EcheanceResponse> getEnRetard(Pageable pageable) {
        return echeanceRepository.findEnRetardPagine(LocalDate.now(), pageable)
                .map(e -> EcheanceMapper.toEcheanceResponse(e, LocalDate.now(), JOURS_ALERTE));
    }

    @Transactional(readOnly = true)
    public Page<EcheanceResponse> getAVenir(Pageable pageable) {
        return echeanceRepository.findAVenirPagine(LocalDate.now(), pageable)
                .map(e -> EcheanceMapper.toEcheanceResponse(e, LocalDate.now(), JOURS_ALERTE));
    }

    private void validerDateDansIntervalle(ProposPaiement propos, LocalDate date) {
        DemandeService demande = propos.getDemande();
        if (demande != null && demande.getDebutService() != null && demande.getFinService() != null
                && (date.isBefore(demande.getDebutService()) || date.isAfter(demande.getFinService()))) {
            throw new BusinessException(
                    "L'échéance doit être comprise entre le début et la fin du service ("
                            + demande.getDebutService() + " → " + demande.getFinService() + ")");
        }
    }

    private void recalculerPlan(ProposPaiement propos) {
        List<EcheancePaiement> echeances = echeanceRepository
                .findByProposPaiementIdOrderByNumeroEcheanceAsc(propos.getId());
        BigDecimal totalAttendu = echeances.stream()
                .map(EcheancePaiement::getMontantAttendu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaye = echeances.stream()
                .map(EcheancePaiement::getMontantPaye)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        propos.setMontantTotal(totalAttendu);
        propos.setRestePayer(totalAttendu.subtract(totalPaye).max(BigDecimal.ZERO));
        proposRepository.save(propos);
    }

    private ProposPaiement getPropos(Integer id) {
        return proposRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Plan de paiement", id)));
    }

    private EcheancePaiement getEcheance(Integer id) {
        return echeanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Échéance", id)));
    }
}
