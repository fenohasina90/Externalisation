package com.cabex.backend.service.service;

import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateDomaineRequest;
import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.dto.UpdateDomaineRequest;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.mapper.DomaineMapper;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomaineGestionService {

    private final DomaineServiceRepository domaineRepository;

    public DomaineGestionService(DomaineServiceRepository domaineRepository) {
        this.domaineRepository = domaineRepository;
    }

    @Transactional(readOnly = true)
    public Page<DomaineResponse> getAll(String recherche, Boolean actif, Pageable pageable) {
        return domaineRepository.rechercher(recherche, actif, pageable)
                .map(DomaineMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DomaineResponse getById(Integer id) {
        return DomaineMapper.toResponse(getEntity(id));
    }

    @Transactional
    public DomaineResponse create(CreateDomaineRequest request) {
        String nom = request.nomDomaine().trim();
        if (domaineRepository.existsByNomDomaine(nom)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(nom));
        }
        DomaineService domaine = DomaineService.builder()
                .nomDomaine(nom)
                .description(request.description())
                .actif(true)
                .build();
        return DomaineMapper.toResponse(domaineRepository.save(domaine));
    }

    @Transactional
    public DomaineResponse update(Integer id, UpdateDomaineRequest request) {
        DomaineService domaine = getEntity(id);
        domaine.setNomDomaine(request.nomDomaine().trim());
        domaine.setDescription(request.description());
        return DomaineMapper.toResponse(domaineRepository.save(domaine));
    }

    @Transactional
    public DomaineResponse toggleActivation(Integer id, boolean actif) {
        DomaineService domaine = getEntity(id);
        domaine.setActif(actif);
        return DomaineMapper.toResponse(domaineRepository.save(domaine));
    }

    @Transactional
    public void delete(Integer id) {
        DomaineService domaine = getEntity(id);
        domaine.setActif(false);
        domaineRepository.save(domaine);
    }

    private DomaineService getEntity(Integer id) {
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Domaine", id)));
    }
}
