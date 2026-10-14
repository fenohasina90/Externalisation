package com.cabex.backend.configuration;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.dto.UpdateConfigurationRequest;
import com.cabex.backend.configuration.entity.Configuration;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.exception.BadRequestException;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mise à jour des paramètres : contrôle « modifiable », validation du type, clé inconnue.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private ConfigurationRepository configurationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AuditService auditService;

    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new ConfigurationService(
                configurationRepository, utilisateurRepository, auditService);
    }

    private Configuration parametre(String valeur, String type, boolean modifiable) {
        return Configuration.builder().id(1).cle("JOURS_AVANCE_ALERTE_ECHEANCE")
                .valeur(valeur).typeValeur(type).modifiable(modifiable).build();
    }

    @Test
    void updateOk() {
        when(configurationRepository.findByCle("JOURS_AVANCE_ALERTE_ECHEANCE"))
                .thenReturn(Optional.of(parametre("3", "INTEGER", true)));
        when(configurationRepository.save(any(Configuration.class))).thenAnswer(i -> i.getArgument(0));

        var resultat = configurationService.update(
                "JOURS_AVANCE_ALERTE_ECHEANCE", new UpdateConfigurationRequest("7"));

        assertEquals("7", resultat.valeur());
    }

    @Test
    void updateNonModifiable() {
        when(configurationRepository.findByCle("JOURS_AVANCE_ALERTE_ECHEANCE"))
                .thenReturn(Optional.of(parametre("3", "INTEGER", false)));
        assertThrows(BadRequestException.class, () -> configurationService.update(
                "JOURS_AVANCE_ALERTE_ECHEANCE", new UpdateConfigurationRequest("7")));
    }

    @Test
    void updateValeurInvalidePourLeType() {
        when(configurationRepository.findByCle("JOURS_AVANCE_ALERTE_ECHEANCE"))
                .thenReturn(Optional.of(parametre("3", "INTEGER", true)));
        assertThrows(BadRequestException.class, () -> configurationService.update(
                "JOURS_AVANCE_ALERTE_ECHEANCE", new UpdateConfigurationRequest("abc")));
    }

    @Test
    void updateCleInconnue() {
        when(configurationRepository.findByCle("INCONNUE")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> configurationService.update(
                "INCONNUE", new UpdateConfigurationRequest("x")));
    }
}
