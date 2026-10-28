package com.cabex.backend.service.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.dto.UpdateServiceRequest;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.mapper.ServiceMapper;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import com.cabex.backend.service.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class ServiceGestionService {

    private final ServiceRepository serviceRepository;
    private final DomaineServiceRepository domaineRepository;
    private final AuditService auditService;

    public ServiceGestionService(ServiceRepository serviceRepository,
                          DomaineServiceRepository domaineRepository,
                          AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.domaineRepository = domaineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAll(String recherche, Boolean actif, String typeService,
                                        Integer domaineId, Pageable pageable) {
        return serviceRepository.rechercher(recherche, actif, normaliserTypeFiltre(typeService), domaineId, pageable)
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

        DomaineService domaine = getDomaine(request.domaineId());
        Service service = Service.builder()
                .nomService(request.nomService().trim())
                .codeService(genererCode(domaine != null ? domaine.getCodeDomaine() : null))
                .typeService(normaliserType(request.typeService()))
                .description(request.description())
                .domaine(domaine)
                .actif(true)
                .build();
        service = serviceRepository.save(service);

        auditService.log(AuditActions.CREATE, "SERVICE", "Service", service.getId().longValue(), null, service.getCodeService() + " : " + service.getNomService());

        return ServiceMapper.toResponse(service);
    }

    @Transactional
    public ServiceResponse update(Integer id, UpdateServiceRequest request) {
        Service service = getEntity(id);
        service.setNomService(request.nomService().trim());
        service.setTypeService(normaliserType(request.typeService()));
        service.setDescription(request.description());
        service.setDomaine(getDomaine(request.domaineId()));
        Service saved = serviceRepository.save(service);
        auditService.log(AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), null, saved.getCodeService() + " : " + saved.getNomService());
        return ServiceMapper.toResponse(saved);
    }

    @Transactional
    public ServiceResponse archive(Integer id, boolean archive) {
        Service service = getEntity(id);
        boolean previousActif = service.getActif();
        service.setActif(!archive);
        service.setArchivedAt(archive ? LocalDateTime.now() : null);
        Service saved = serviceRepository.save(service);
        auditService.log(archive ? AuditActions.ARCHIVE : AuditActions.UPDATE, "SERVICE", "Service", saved.getId().longValue(), "actif : " + previousActif, "actif : " + saved.getActif());
        return ServiceMapper.toResponse(saved);
    }

    private Service getEntity(Integer id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Service", id)));
    }

    private DomaineService getDomaine(Integer id) {
        if (id == null) {
            return null;
        }
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Domaine", id)));
    }

    /**
     * Normalise un filtre de type : vide signifie « tous », toute valeur inconnue
     * est rejetée comme pour la création et la modification.
     */
    String normaliserTypeFiltre(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalise = type.trim().toUpperCase(java.util.Locale.ROOT);
        if (!com.cabex.backend.dossier.DossierRegles.TYPES_SERVICE.contains(normalise)) {
            throw new BusinessException(ErrorMessages.formatInvalide("type de service"));
        }
        return normalise;
    }

    /**
     * Normalise le type (STANDARD par défaut) ; toute autre valeur est rejetée.
     */
    String normaliserType(String type) {
        if (type == null || type.isBlank()) {
            return "STANDARD";
        }
        String normalise = type.trim().toUpperCase(java.util.Locale.ROOT);
        if (!com.cabex.backend.dossier.DossierRegles.TYPES_SERVICE.contains(normalise)) {
            throw new BusinessException(ErrorMessages.formatInvalide("type de service"));
        }
        return normalise;
    }

    /**
     * Génère le prochain code libre au format {@code SRV-{codeDomaine}-XXXX}
     * (ex. {@code SRV-COMP-0007} pour le domaine dont le code est COMP).
     * Si aucun domaine n'est renseigné, le format historique {@code SRV-XXXX}
     * est conservé.
     * Le test d'existence final protège des doublons en cas d'accès concurrents
     * (la colonne reste UNIQUE en base).
     */
    String genererCode(String codeDomaine) {
        String prefix;
        if (codeDomaine == null || codeDomaine.isBlank()) {
            prefix = "SRV-";
        } else {
            prefix = "SRV-" + codeDomaine.trim().toUpperCase(java.util.Locale.ROOT) + "-";
        }
        int max = 0;
        for (String code : serviceRepository.findAllCodes()) {
            if (code == null) {
                continue;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^" + java.util.regex.Pattern.quote(prefix) + "(\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(code.trim());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        String candidat = prefix + suivant(max + 1);
        while (serviceRepository.existsByCodeService(candidat)) {
            candidat = prefix + suivant(++max + 1);
        }
        return candidat;
    }

    private String suivant(int numero) {
        return String.format("%04d", numero);
    }
}
