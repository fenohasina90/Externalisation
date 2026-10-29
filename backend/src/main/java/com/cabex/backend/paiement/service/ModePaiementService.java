package com.cabex.backend.paiement.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.dto.CreateModePaiementRequest;
import com.cabex.backend.paiement.dto.ModePaiementResponse;
import com.cabex.backend.paiement.dto.UpdateModePaiementRequest;
import com.cabex.backend.paiement.entity.ModePaiement;
import com.cabex.backend.paiement.mapper.PaiementMapper;
import com.cabex.backend.paiement.repository.ModePaiementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ModePaiementService {

    private final ModePaiementRepository modeRepository;
    private final AuditService auditService;

    public ModePaiementService(ModePaiementRepository modeRepository, AuditService auditService) {
        this.modeRepository = modeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ModePaiementResponse> getAll(Boolean actif, Pageable pageable) {
        Page<ModePaiement> page = actif != null
                ? modeRepository.findByActif(actif, pageable)
                : modeRepository.findAll(pageable);
        return page.map(PaiementMapper::toModeResponse);
    }

    @Transactional(readOnly = true)
    public ModePaiementResponse getById(Integer id) {
        return PaiementMapper.toModeResponse(getEntity(id));
    }

    @Transactional
    public ModePaiementResponse create(CreateModePaiementRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (modeRepository.existsByCode(code)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(code));
        }
        ModePaiement mode = ModePaiement.builder()
                .code(code)
                .libelle(request.libelle().trim())
                .actif(true)
                .build();
        ModePaiement saved = modeRepository.save(mode);
        auditService.log(AuditActions.CREATE, "PAIEMENT", "ModePaiement",
                saved.getId().longValue(), null, saved.getCode());
        return PaiementMapper.toModeResponse(saved);
    }

    @Transactional
    public ModePaiementResponse update(Integer id, UpdateModePaiementRequest request) {
        ModePaiement mode = getEntity(id);
        String ancien = mode.getCode();
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.equals(mode.getCode()) && modeRepository.existsByCode(code)) {
            throw new DuplicateResourceException(ErrorMessages.referenceDejaUtilisee(code));
        }
        mode.setCode(code);
        mode.setLibelle(request.libelle().trim());
        ModePaiement saved = modeRepository.save(mode);
        auditService.log(AuditActions.UPDATE, "PAIEMENT", "ModePaiement",
                saved.getId().longValue(), ancien, code);
        return PaiementMapper.toModeResponse(saved);
    }

    @Transactional
    public ModePaiementResponse toggleActivation(Integer id, boolean actif) {
        ModePaiement mode = getEntity(id);
        boolean previous = mode.getActif();
        mode.setActif(actif);
        ModePaiement saved = modeRepository.save(mode);
        auditService.log(AuditActions.UPDATE, "PAIEMENT", "ModePaiement",
                saved.getId().longValue(), "actif : " + previous, "actif : " + actif);
        return PaiementMapper.toModeResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        ModePaiement mode = getEntity(id);
        mode.setActif(false);
        modeRepository.save(mode);
        auditService.log(AuditActions.DELETE, "PAIEMENT", "ModePaiement",
                mode.getId().longValue(), mode.getCode(), null);
    }

    private ModePaiement getEntity(Integer id) {
        return modeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Mode de paiement", id)));
    }
}
