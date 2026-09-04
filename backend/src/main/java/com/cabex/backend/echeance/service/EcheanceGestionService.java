package com.cabex.backend.echeance.service;

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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
public class EcheanceGestionService {

    private static final int JOURS_ALERTE = 3;

    private final EcheancePaiementRepository echeanceRepository;
    private final ProposPaiementRepository proposRepository;

    public EcheanceGestionService(EcheancePaiementRepository echeanceRepository,
                                  ProposPaiementRepository proposRepository) {
        this.echeanceRepository = echeanceRepository;
        this.proposRepository = proposRepository;
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

        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), JOURS_ALERTE);
    }

    @Transactional
    public EcheanceResponse update(Integer echeanceId, UpdateEcheanceRequest request) {
        EcheancePaiement echeance = getEcheance(echeanceId);

        if (echeance.getMontantPaye().compareTo(request.montantAttendu()) > 0) {
            throw new BusinessException(
                    "Le montant attendu ne peut pas être inférieur au montant déjà payé");
        }

        echeance.setDateEcheance(request.dateEcheance());
        echeance.setMontantAttendu(request.montantAttendu());
        echeance.setCommentaire(request.commentaire());
        echeance = echeanceRepository.save(echeance);

        recalculerPlan(echeance.getProposPaiement());

        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), JOURS_ALERTE);
    }

    @Transactional(readOnly = true)
    public List<EcheanceResponse> getEnRetard() {
        return echeanceRepository.findEnRetard(LocalDate.now()).stream()
                .map(e -> EcheanceMapper.toEcheanceResponse(e, LocalDate.now(), JOURS_ALERTE))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EcheanceResponse> getAVenir() {
        return echeanceRepository.findAVenir(LocalDate.now()).stream()
                .map(e -> EcheanceMapper.toEcheanceResponse(e, LocalDate.now(), JOURS_ALERTE))
                .toList();
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
