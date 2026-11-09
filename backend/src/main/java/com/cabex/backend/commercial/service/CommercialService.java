package com.cabex.backend.commercial.service;

import com.cabex.backend.commercial.dto.CommercialResponse;
import com.cabex.backend.commercial.dto.CreateCommercialRequest;
import com.cabex.backend.commercial.dto.UpdateCommercialRequest;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.commercial.mapper.CommercialMapper;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialService {

    private final CommercialRepository commercialRepository;
    private final AuditService auditService;

    public CommercialService(CommercialRepository commercialRepository, AuditService auditService) {
        this.commercialRepository = commercialRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<CommercialResponse> getAll(String recherche, Boolean actif, Pageable pageable) {
        return commercialRepository.rechercher(recherche, actif, pageable)
                .map(CommercialMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CommercialResponse getById(Integer id) {
        return CommercialMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CommercialResponse create(CreateCommercialRequest request) {
        String immatricule = genererImmatricule();
        Commercial commercial = Commercial.builder()
                .nomCommercial(request.nomCommercial().trim())
                .immatricule(immatricule)
                .email(request.email())
                .contact(request.contact())
                .actif(true)
                .build();
        Commercial saved = commercialRepository.save(commercial);
        auditService.log(AuditActions.CREATE, "COMMERCIAL", "Commercial", saved.getId().longValue(), null, saved.getNomCommercial());
        return CommercialMapper.toResponse(saved);
    }

    /**
     * Génère la prochaine immatricule libre (C001, C002, …).
     * Le test d'existence final protège des doublons en cas d'accès concurrents
     * (la colonne reste UNIQUE en base).
     */
    String genererImmatricule() {
        int max = 0;
        for (String immatricule : commercialRepository.findAllImmatricules()) {
            if (immatricule == null) {
                continue;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^C(\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(immatricule.trim());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        String candidate = suivante(max + 1);
        while (commercialRepository.existsByImmatricule(candidate)) {
            candidate = suivante(++max + 1);
        }
        return candidate;
    }

    private String suivante(int numero) {
        return "C" + String.format("%03d", numero);
    }

    @Transactional
    public CommercialResponse update(Integer id, UpdateCommercialRequest request) {
        Commercial commercial = getEntity(id);
        commercial.setNomCommercial(request.nomCommercial().trim());
        if (request.email() != null) {
            commercial.setEmail(request.email());
        }
        if (request.contact() != null) {
            commercial.setContact(request.contact());
        }
        Commercial saved = commercialRepository.save(commercial);
        auditService.log(AuditActions.UPDATE, "COMMERCIAL", "Commercial", saved.getId().longValue(), null, saved.getNomCommercial());
        return CommercialMapper.toResponse(saved);
    }

    @Transactional
    public CommercialResponse toggleActivation(Integer id, boolean actif) {
        Commercial commercial = getEntity(id);
        boolean previousActif = commercial.getActif();
        commercial.setActif(actif);
        Commercial saved = commercialRepository.save(commercial);
        auditService.log(AuditActions.UPDATE, "COMMERCIAL", "Commercial", saved.getId().longValue(), "actif : " + previousActif, "actif : " + actif);
        return CommercialMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Commercial commercial = getEntity(id);
        String nom = commercial.getNomCommercial();
        commercial.setActif(false);
        commercialRepository.save(commercial);
        auditService.log(AuditActions.DELETE, "COMMERCIAL", "Commercial", commercial.getId().longValue(), nom, null);
    }

    private Commercial getEntity(Integer id) {
        return commercialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Commercial", id)));
    }
}
