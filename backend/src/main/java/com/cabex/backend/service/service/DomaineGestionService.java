package com.cabex.backend.service.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
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

import java.util.Map;

@Service
public class DomaineGestionService {

    private final DomaineServiceRepository domaineRepository;
    private final AuditService auditService;

    public DomaineGestionService(DomaineServiceRepository domaineRepository, AuditService auditService) {
        this.domaineRepository = domaineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DomaineResponse> getAll(String recherche, Boolean actif, Pageable pageable) {
        Map<Object, Long> nbParDomaine = new java.util.HashMap<>();
        for (Object[] o : domaineRepository.compterSocietesParDomaine()) {
            nbParDomaine.put(o[0], (long) o[1]);
        }
        return domaineRepository.rechercher(recherche, actif, pageable)
                .map(d -> DomaineMapper.toResponse(d, nbParDomaine.getOrDefault(d.getId(), 0L)));
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
        DomaineService saved = domaineRepository.save(domaine);
        auditService.log(AuditActions.CREATE, "SERVICE", "Domaine", saved.getId().longValue(), null, "nom=" + saved.getNomDomaine());
        return DomaineMapper.toResponse(saved);
    }

    @Transactional
    public DomaineResponse update(Integer id, UpdateDomaineRequest request) {
        DomaineService domaine = getEntity(id);
        domaine.setNomDomaine(request.nomDomaine().trim());
        domaine.setDescription(request.description());
        DomaineService saved = domaineRepository.save(domaine);
        auditService.log(AuditActions.UPDATE, "SERVICE", "Domaine", saved.getId().longValue(), null, "nom=" + saved.getNomDomaine());
        return DomaineMapper.toResponse(saved);
    }

    @Transactional
    public DomaineResponse toggleActivation(Integer id, boolean actif) {
        DomaineService domaine = getEntity(id);
        boolean previousActif = domaine.getActif();
        domaine.setActif(actif);
        DomaineService saved = domaineRepository.save(domaine);
        auditService.log(AuditActions.UPDATE, "SERVICE", "Domaine", saved.getId().longValue(), "actif=" + previousActif, "actif=" + actif);
        return DomaineMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        DomaineService domaine = getEntity(id);
        String nom = domaine.getNomDomaine();
        domaine.setActif(false);
        domaineRepository.save(domaine);
        auditService.log(AuditActions.DELETE, "SERVICE", "Domaine", domaine.getId().longValue(), "nom=" + nom, null);
    }

    private DomaineService getEntity(Integer id) {
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Domaine", id)));
    }
}
