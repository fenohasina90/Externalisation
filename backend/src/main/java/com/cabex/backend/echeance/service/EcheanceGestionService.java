package com.cabex.backend.echeance.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.echeance.dto.CreateEcheanceRequest;
import com.cabex.backend.echeance.dto.EcheanceCibleResponse;
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

    private final EcheancePaiementRepository echeanceRepository;
    private final ProposPaiementRepository proposRepository;
    private final AuditService auditService;
    private final ConfigurationService configurationService;

    public EcheanceGestionService(EcheancePaiementRepository echeanceRepository,
                                  ProposPaiementRepository proposRepository,
                                  AuditService auditService,
                                  ConfigurationService configurationService) {
        this.echeanceRepository = echeanceRepository;
        this.proposRepository = proposRepository;
        this.auditService = auditService;
        this.configurationService = configurationService;
    }

    private int joursAlerte() {
        return Math.max(configurationService.lireEntier(
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE,
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE_DEFAUT), 1);
    }

    @Transactional(readOnly = true)
    public List<EcheanceResponse> getByPlan(Integer proposId) {
        getPropos(proposId);
        LocalDate aujourdHui = LocalDate.now();
        int joursAlerte = joursAlerte();
        return echeanceRepository
                .findByProposPaiementIdOrderByDateEcheanceDescNumeroEcheanceDesc(proposId).stream()
                .map(e -> EcheanceMapper.toEcheanceResponse(e, aujourdHui, joursAlerte))
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
        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), joursAlerte());
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
        return EcheanceMapper.toEcheanceResponse(echeance, LocalDate.now(), joursAlerte());
    }

    @Transactional(readOnly = true)
    public Page<EcheanceResponse> getEnRetard(Pageable pageable) {
        LocalDate aujourdHui = LocalDate.now();
        int joursAlerte = joursAlerte();
        return echeanceRepository.findEnRetardPagine(aujourdHui, pageable)
                .map(e -> EcheanceMapper.toEcheanceResponse(e, aujourdHui, joursAlerte));
    }

    @Transactional(readOnly = true)
    public Page<EcheanceResponse> getAVenir(Pageable pageable) {
        LocalDate aujourdHui = LocalDate.now();
        int joursAlerte = joursAlerte();
        return echeanceRepository.findAVenirPagine(aujourdHui, pageable)
                .map(e -> EcheanceMapper.toEcheanceResponse(e, aujourdHui, joursAlerte));
    }

    @Transactional(readOnly = true)
    public EcheanceCibleResponse getCible(Integer echeanceId) {
        EcheancePaiement echeance = getEcheance(echeanceId);
        DemandeService demande = echeance.getProposPaiement().getDemande();
        return new EcheanceCibleResponse(
                echeance.getId(),
                echeance.getNumeroEcheance(),
                demande.getId(),
                demande.getReferenceDemande());
    }

    private void validerDateDansIntervalle(ProposPaiement propos, LocalDate date) {
        com.cabex.backend.demande.entity.LigneService ligne = propos.getLigneService();
        LocalDate debut = ligne != null ? ligne.getDebutService() : null;
        LocalDate fin = ligne != null ? ligne.getFinService() : null;
        if (date != null && debut != null && fin != null
                && (date.isBefore(debut) || date.isAfter(fin))) {
            throw new BusinessException(
                    "L'échéance doit être comprise entre le début et la fin du service ("
                            + debut + " → " + fin + ")");
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
