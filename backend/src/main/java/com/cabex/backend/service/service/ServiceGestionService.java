package com.cabex.backend.service.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.dto.UpdateServiceRequest;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.mapper.ServiceMapper;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class ServiceGestionService {

    private final ServiceRepository serviceRepository;
    private final AuditService auditService;

    public ServiceGestionService(ServiceRepository serviceRepository,
                          AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAll(String recherche, Boolean actif, Pageable pageable) {
        return serviceRepository.rechercher(recherche, actif, pageable)
                .map(ServiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getById(Integer id) {
        return ServiceMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public ServiceResponse getByCode(String code) {
        String normalise = code == null ? "" : code.trim().toUpperCase();
        return serviceRepository.findByCodeService(normalise)
                .map(ServiceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Service", code)));
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        if (serviceRepository.existsByNomService(request.nomService().trim())) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(request.nomService().trim()));
        }

        Service service = Service.builder()
                .nomService(request.nomService().trim())
                .codeService(genererCode())
                .description(request.description())
                .actif(true)
                .build();
        service = serviceRepository.save(service);

        auditService.log(AuditActions.CREATE, "SERVICE", "Service", service.getId().longValue(), null, "nom=" + service.getNomService());

        return ServiceMapper.toResponse(service);
    }

    @Transactional
    public ServiceResponse update(Integer id, UpdateServiceRequest request) {
        Service service = getEntity(id);
        service.setNomService(request.nomService().trim());
        service.setDescription(request.description());
        Service saved = serviceRepository.save(service);
        auditService.log(AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), null, "nom=" + saved.getNomService());
        return ServiceMapper.toResponse(saved);
    }

    @Transactional
    public ServiceResponse archive(Integer id, boolean archive) {
        Service service = getEntity(id);
        boolean previousActif = service.getActif();
        service.setActif(!archive);
        service.setArchivedAt(archive ? LocalDateTime.now() : null);
        Service saved = serviceRepository.save(service);
        auditService.log(archive ? AuditActions.ARCHIVE : AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), "actif=" + previousActif, "actif=" + saved.getActif());
        return ServiceMapper.toResponse(saved);
    }

    private Service getEntity(Integer id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Service", id)));
    }

    /**
     * Génère le prochain code libre (SRV-0001, SRV-0002, …).
     * Le test d'existence final protège des doublons en cas d'accès
     * concurrents (la colonne reste UNIQUE en base).
     */
    String genererCode() {
        int max = 0;
        for (String code : serviceRepository.findAllCodes()) {
            if (code == null) {
                continue;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^SRV-(\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(code.trim());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        String candidat = suivant(max + 1);
        while (serviceRepository.existsByCodeService(candidat)) {
            candidat = suivant(++max + 1);
        }
        return candidat;
    }

    private String suivant(int numero) {
        return "SRV-" + String.format("%04d", numero);
    }
}
