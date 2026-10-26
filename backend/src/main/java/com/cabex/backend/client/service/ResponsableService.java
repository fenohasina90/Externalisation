package com.cabex.backend.client.service;

import com.cabex.backend.client.dto.CreateResponsableRequest;
import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.dto.UpdateResponsableRequest;
import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.mapper.ResponsableMapper;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResponsableService {

    private final ClientResponsableRepository responsableRepository;
    private final SocieteRepository societeRepository;
    private final AuditService auditService;

    public ResponsableService(ClientResponsableRepository responsableRepository,
                              SocieteRepository societeRepository,
                              AuditService auditService) {
        this.responsableRepository = responsableRepository;
        this.societeRepository = societeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ResponsableResponse> getAll(String recherche, Integer societeId, Boolean actif,
                                            Pageable pageable) {
        return responsableRepository.rechercher(recherche, societeId, actif, pageable)
                .map(ResponsableMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ResponsableResponse> getBySociete(Integer societeId) {
        return responsableRepository.findBySocieteIdAndActifTrue(societeId).stream()
                .map(ResponsableMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponsableResponse getById(Integer id) {
        return ResponsableMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ResponsableResponse create(CreateResponsableRequest request) {
        Societe societe = null;
        if (request.societeId() != null) {
            societe = societeRepository.findById(request.societeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.introuvable("Société", request.societeId())));
        }
        ClientResponsable responsable = ClientResponsable.builder()
                .societe(societe)
                .nomClient(request.nomClient().trim())
                .contact(request.contact())
                .email(request.email())
                .fonction(request.fonction())
                .actif(true)
                .build();
        ClientResponsable saved = responsableRepository.save(responsable);
        auditService.log(AuditActions.CREATE, "CLIENT", "ClientResponsable",
                Long.valueOf(saved.getId()), null, "nom=" + saved.getNomClient());
        return ResponsableMapper.toResponse(saved);
    }

    @Transactional
    public ResponsableResponse update(Integer id, UpdateResponsableRequest request) {
        ClientResponsable responsable = getEntity(id);
        responsable.setNomClient(request.nomClient().trim());
        responsable.setContact(request.contact());
        responsable.setEmail(request.email());
        responsable.setFonction(request.fonction());
        ClientResponsable saved = responsableRepository.save(responsable);
        auditService.log(AuditActions.UPDATE, "CLIENT", "ClientResponsable",
                Long.valueOf(saved.getId()), null, "nom=" + saved.getNomClient());
        return ResponsableMapper.toResponse(saved);
    }

    @Transactional
    public ResponsableResponse toggleActivation(Integer id, boolean actif) {
        ClientResponsable responsable = getEntity(id);
        boolean previousActif = responsable.getActif();
        responsable.setActif(actif);
        ClientResponsable saved = responsableRepository.save(responsable);
        auditService.log(AuditActions.UPDATE, "CLIENT", "ClientResponsable",
                Long.valueOf(saved.getId()), "actif=" + previousActif, "actif=" + actif);
        return ResponsableMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        ClientResponsable responsable = getEntity(id);
        String nom = responsable.getNomClient();
        responsable.setActif(false);
        responsableRepository.save(responsable);
        auditService.log(AuditActions.DELETE, "CLIENT", "ClientResponsable",
                Long.valueOf(responsable.getId()), "nom=" + nom, null);
    }

    private ClientResponsable getEntity(Integer id) {
        return responsableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Responsable", id)));
    }
}
