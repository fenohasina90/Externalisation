package com.cabex.backend.demande.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.dto.CreateStatutRequest;
import com.cabex.backend.demande.dto.StatutResponse;
import com.cabex.backend.demande.dto.UpdateStatutRequest;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.mapper.StatutMapper;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatutService {

    private final StatutDemandeRepository statutRepository;
    private final AuditService auditService;

    public StatutService(StatutDemandeRepository statutRepository, AuditService auditService) {
        this.statutRepository = statutRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<StatutResponse> getAll(Boolean actif, Pageable pageable) {
        Page<StatutDemande> page = actif != null
                ? statutRepository.findByActif(actif, pageable)
                : statutRepository.findAll(pageable);
        return page.map(StatutMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StatutResponse getById(Integer id) {
        return StatutMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StatutResponse create(CreateStatutRequest request) {
        String nom = request.statut().trim().toUpperCase(java.util.Locale.ROOT);
        if (statutRepository.existsByStatut(nom)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(nom));
        }
        StatutDemande statut = StatutDemande.builder()
                .statut(nom)
                .description(request.description())
                .ordre(request.ordre() != null ? request.ordre() : 0)
                .actif(true)
                .build();
        StatutDemande saved = statutRepository.save(statut);
        auditService.log(AuditActions.CREATE, "DEMANDE", "StatutDemande",
                saved.getId().longValue(), null, "statut=" + saved.getStatut());
        return StatutMapper.toResponse(saved);
    }

    @Transactional
    public StatutResponse update(Integer id, UpdateStatutRequest request) {
        StatutDemande statut = getEntity(id);
        String ancien = statut.getStatut();
        String nom = request.statut().trim().toUpperCase(java.util.Locale.ROOT);
        if (!nom.equals(statut.getStatut()) && statutRepository.existsByStatut(nom)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(nom));
        }
        statut.setStatut(nom);
        statut.setDescription(request.description());
        if (request.ordre() != null) {
            statut.setOrdre(request.ordre());
        }
        StatutDemande saved = statutRepository.save(statut);
        auditService.log(AuditActions.UPDATE, "DEMANDE", "StatutDemande",
                saved.getId().longValue(), "statut=" + ancien, "statut=" + nom);
        return StatutMapper.toResponse(saved);
    }

    @Transactional
    public StatutResponse toggleActivation(Integer id, boolean actif) {
        StatutDemande statut = getEntity(id);
        boolean previous = statut.getActif();
        statut.setActif(actif);
        StatutDemande saved = statutRepository.save(statut);
        auditService.log(AuditActions.UPDATE, "DEMANDE", "StatutDemande",
                saved.getId().longValue(), "actif=" + previous, "actif=" + actif);
        return StatutMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        StatutDemande statut = getEntity(id);
        statut.setActif(false);
        statutRepository.save(statut);
        auditService.log(AuditActions.DELETE, "DEMANDE", "StatutDemande",
                statut.getId().longValue(), "statut=" + statut.getStatut(), null);
    }

    private StatutDemande getEntity(Integer id) {
        return statutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Statut", id)));
    }
}
