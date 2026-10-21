package com.cabex.backend.configuration.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.dto.ConfigurationResponse;
import com.cabex.backend.configuration.dto.UpdateConfigurationRequest;
import com.cabex.backend.configuration.entity.Configuration;
import com.cabex.backend.configuration.mapper.ConfigurationMapper;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public ConfigurationService(ConfigurationRepository configurationRepository,
                                UtilisateurRepository utilisateurRepository,
                                AuditService auditService) {
        this.configurationRepository = configurationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ConfigurationResponse> getAll() {
        return configurationRepository.findAll().stream()
                .map(ConfigurationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfigurationResponse getByCle(String cle) {
        return ConfigurationMapper.toResponse(getEntity(cle));
    }

    /**
     * Lecture en repli silencieux : renvoie la valeur brute si la clé existe,
     * sinon {@code defaut}. Utilisé par les traitements internes pour que toute
     * modification depuis l'administration s'applique sans redémarrage.
     */
    @Transactional(readOnly = true)
    public String lireValeur(String cle, String defaut) {
        return configurationRepository.findByCle(cle)
                .map(Configuration::getValeur)
                .orElse(defaut);
    }

    /** Lecture d'un entier avec repli sur {@code defaut} si absent ou mal formé. */
    @Transactional(readOnly = true)
    public int lireEntier(String cle, int defaut) {
        try {
            return Integer.parseInt(lireValeur(cle, String.valueOf(defaut)).trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    /** Lecture d'un entier long avec repli sur {@code defaut} si absent ou mal formé. */
    @Transactional(readOnly = true)
    public long lireEntierLong(String cle, long defaut) {
        try {
            return Long.parseLong(lireValeur(cle, String.valueOf(defaut)).trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    /** Lecture d'une heure HH:mm avec repli si la clé est absente ou mal formée. */
    @Transactional(readOnly = true)
    public LocalTime lireHeure(String cle, LocalTime defaut) {
        try {
            return LocalTime.parse(lireValeur(cle, defaut.toString()).trim());
        } catch (DateTimeParseException e) {
            return defaut;
        }
    }

    @Transactional
    public ConfigurationResponse update(String cle, UpdateConfigurationRequest request) {
        Configuration config = getEntity(cle);

        if (Boolean.FALSE.equals(config.getModifiable())) {
            throw new BadRequestException(
                    "La configuration '" + cle + "' n'est pas modifiable");
        }

        String valeur = request.valeur().trim();
        validerValeur(config.getTypeValeur(), cle, valeur);

        String ancienne = config.getValeur();
        config.setValeur(valeur);
        config.setUpdatedBy(currentUtilisateur());
        Configuration saved = configurationRepository.save(config);

        auditService.log(AuditActions.UPDATE, "CONFIGURATION", "Configuration",
                saved.getId().longValue(), ancienne, valeur);

        return ConfigurationMapper.toResponse(saved);
    }

    private void validerValeur(String typeValeur, String cle, String valeur) {
        if (typeValeur == null || "STRING".equalsIgnoreCase(typeValeur)) {
            return;
        }
        try {
            switch (typeValeur.toUpperCase()) {
                case "INTEGER" -> Integer.parseInt(valeur);
                case "DECIMAL", "NUMERIC" -> new BigDecimal(valeur);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(valeur) && !"false".equalsIgnoreCase(valeur)) {
                        throw new NumberFormatException("booléen attendu");
                    }
                }
                case "DATE" -> java.time.LocalDate.parse(valeur);
                case "TIME" -> LocalTime.parse(valeur);
                case "DATETIME", "TIMESTAMP" -> java.time.LocalDateTime.parse(valeur);
                default -> {
                }
            }
        } catch (Exception e) {
            throw new BadRequestException(ErrorMessages.formatInvalide(cle));
        }
    }

    private Configuration getEntity(String cle) {
        return configurationRepository.findByCle(cle)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paramètre de configuration", cle)));
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
