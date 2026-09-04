package com.cabex.backend.audit.service;

import com.cabex.backend.audit.dto.AuditLogResponse;
import com.cabex.backend.audit.entity.AuditLog;
import com.cabex.backend.audit.mapper.AuditLogMapper;
import com.cabex.backend.audit.repository.AuditLogRepository;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@org.springframework.stereotype.Service
public class AuditService {

    private final AuditLogRepository auditRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AuditService(AuditLogRepository auditRepository,
                        UtilisateurRepository utilisateurRepository) {
        this.auditRepository = auditRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Integer utilisateurId, String action, String module,
                                       String entite, LocalDate dateDebut, LocalDate dateFin,
                                       Pageable pageable) {
        LocalDateTime debut = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime fin = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;
        return auditRepository.rechercher(utilisateurId, action, module, entite, debut, fin, pageable)
                .map(AuditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return AuditLogMapper.toResponse(auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Trace d'audit", id))));
    }

    @Transactional
    public AuditLog log(String action, String module, String entite, Long entiteId,
                        String ancienneValeur, String nouvelleValeur) {
        return log(currentUtilisateur(), action, module, entite, entiteId,
                ancienneValeur, nouvelleValeur);
    }

    @Transactional
    public AuditLog log(Utilisateur utilisateur, String action, String module, String entite,
                        Long entiteId, String ancienneValeur, String nouvelleValeur) {
        AuditLog audit = AuditLog.builder()
                .utilisateur(utilisateur)
                .action(action)
                .module(module)
                .entite(entite)
                .entiteId(entiteId)
                .ancienneValeur(toJson(ancienneValeur))
                .nouvelleValeur(toJson(nouvelleValeur))
                .adresseIp(extractIp())
                .userAgent(extractUserAgent())
                .build();
        return auditRepository.save(audit);
    }

    private String toJson(String value) {
        if (value == null) {
            return null;
        }
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"valeur\":\"" + escaped + "\"}";
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String extractIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }
}
