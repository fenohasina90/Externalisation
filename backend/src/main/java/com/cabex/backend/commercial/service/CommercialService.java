package com.cabex.backend.commercial.service;

import com.cabex.backend.commercial.dto.CommercialResponse;
import com.cabex.backend.commercial.dto.CreateCommercialRequest;
import com.cabex.backend.commercial.dto.UpdateCommercialRequest;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.commercial.mapper.CommercialMapper;
import com.cabex.backend.commercial.repository.CommercialRepository;
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

    public CommercialService(CommercialRepository commercialRepository) {
        this.commercialRepository = commercialRepository;
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
        String immatricule = request.immatricule().trim().toUpperCase();
        if (commercialRepository.existsByImmatricule(immatricule)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(immatricule));
        }
        Commercial commercial = Commercial.builder()
                .nomCommercial(request.nomCommercial().trim())
                .immatricule(immatricule)
                .email(request.email())
                .contact(request.contact())
                .actif(true)
                .build();
        return CommercialMapper.toResponse(commercialRepository.save(commercial));
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
        return CommercialMapper.toResponse(commercialRepository.save(commercial));
    }

    @Transactional
    public CommercialResponse toggleActivation(Integer id, boolean actif) {
        Commercial commercial = getEntity(id);
        commercial.setActif(actif);
        return CommercialMapper.toResponse(commercialRepository.save(commercial));
    }

    @Transactional
    public void delete(Integer id) {
        Commercial commercial = getEntity(id);
        commercial.setActif(false);
        commercialRepository.save(commercial);
    }

    private Commercial getEntity(Integer id) {
        return commercialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Commercial", id)));
    }
}
