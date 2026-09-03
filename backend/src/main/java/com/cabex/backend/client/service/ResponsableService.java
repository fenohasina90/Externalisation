package com.cabex.backend.client.service;

import com.cabex.backend.client.dto.CreateResponsableRequest;
import com.cabex.backend.client.dto.ResponsableResponse;
import com.cabex.backend.client.dto.UpdateResponsableRequest;
import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.mapper.ResponsableMapper;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResponsableService {

    private final ClientResponsableRepository responsableRepository;
    private final SocieteRepository societeRepository;

    public ResponsableService(ClientResponsableRepository responsableRepository,
                              SocieteRepository societeRepository) {
        this.responsableRepository = responsableRepository;
        this.societeRepository = societeRepository;
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
        Societe societe = societeRepository.findById(request.societeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Société", request.societeId())));
        ClientResponsable responsable = ClientResponsable.builder()
                .societe(societe)
                .nomClient(request.nomClient().trim())
                .contact(request.contact())
                .email(request.email())
                .fonction(request.fonction())
                .actif(true)
                .build();
        return ResponsableMapper.toResponse(responsableRepository.save(responsable));
    }

    @Transactional
    public ResponsableResponse update(Integer id, UpdateResponsableRequest request) {
        ClientResponsable responsable = getEntity(id);
        responsable.setNomClient(request.nomClient().trim());
        responsable.setContact(request.contact());
        responsable.setEmail(request.email());
        responsable.setFonction(request.fonction());
        return ResponsableMapper.toResponse(responsableRepository.save(responsable));
    }

    @Transactional
    public void delete(Integer id) {
        ClientResponsable responsable = getEntity(id);
        responsable.setActif(false);
        responsableRepository.save(responsable);
    }

    private ClientResponsable getEntity(Integer id) {
        return responsableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Responsable", id)));
    }
}
