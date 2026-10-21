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

import java.time.LocalTime;
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
    void updateHeureValide() {
        when(configurationRepository.findByCle("HEURE_SAUVEGARDE_AUTOMATIQUE"))
                .thenReturn(Optional.of(Configuration.builder().id(3)
                        .cle("HEURE_SAUVEGARDE_AUTOMATIQUE").valeur("02:00")
                        .typeValeur("TIME").modifiable(true).build()));
        when(configurationRepository.save(any(Configuration.class))).thenAnswer(i -> i.getArgument(0));

        var resultat = configurationService.update(
                "HEURE_SAUVEGARDE_AUTOMATIQUE", new UpdateConfigurationRequest("03:30"));

        assertEquals("03:30", resultat.valeur());
    }

    @Test
    void updateHeureInvalide() {
        when(configurationRepository.findByCle("HEURE_SAUVEGARDE_AUTOMATIQUE"))
                .thenReturn(Optional.of(Configuration.builder().id(3)
                        .cle("HEURE_SAUVEGARDE_AUTOMATIQUE").valeur("02:00")
                        .typeValeur("TIME").modifiable(true).build()));

        assertThrows(BadRequestException.class, () -> configurationService.update(
                "HEURE_SAUVEGARDE_AUTOMATIQUE", new UpdateConfigurationRequest("25:00")));
    }

    @Test
    void lireHeureAvecRepli() {
        when(configurationRepository.findByCle("HEURE_SAUVEGARDE_AUTOMATIQUE"))
                .thenReturn(Optional.of(Configuration.builder().id(3)
                        .cle("HEURE_SAUVEGARDE_AUTOMATIQUE").valeur("03:15")
                        .typeValeur("TIME").build()));
        when(configurationRepository.findByCle("MAL_FORME"))
                .thenReturn(Optional.of(Configuration.builder().id(4)
                        .cle("MAL_FORME").valeur("25:00").typeValeur("TIME").build()));
        when(configurationRepository.findByCle("ABSENTE")).thenReturn(Optional.empty());

        assertEquals(LocalTime.of(3, 15),
                configurationService.lireHeure("HEURE_SAUVEGARDE_AUTOMATIQUE", LocalTime.of(2, 0)));
        assertEquals(LocalTime.of(2, 0),
                configurationService.lireHeure("MAL_FORME", LocalTime.of(2, 0)));
        assertEquals(LocalTime.of(2, 0),
                configurationService.lireHeure("ABSENTE", LocalTime.of(2, 0)));
    }

    @Test
    void updateCleInconnue() {
        when(configurationRepository.findByCle("INCONNUE")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> configurationService.update(
                "INCONNUE", new UpdateConfigurationRequest("x")));
    }

    @Test
    void lireValeurAvecRepli() {
        when(configurationRepository.findByCle("NOM_APPLICATION"))
                .thenReturn(Optional.of(Configuration.builder().id(2).cle("NOM_APPLICATION")
                        .valeur("Mon Cabinet").typeValeur("STRING").build()));
        when(configurationRepository.findByCle("ABSENTE")).thenReturn(Optional.empty());

        assertEquals("Mon Cabinet", configurationService.lireValeur("NOM_APPLICATION", "Défaut"));
        assertEquals("Défaut", configurationService.lireValeur("ABSENTE", "Défaut"));
    }

    @Test
    void lireEntierAvecRepli() {
        when(configurationRepository.findByCle("JOURS_AVANCE_ALERTE_ECHEANCE"))
                .thenReturn(Optional.of(parametre("7", "INTEGER", true)));
        when(configurationRepository.findByCle("MAL_FORME"))
                .thenReturn(Optional.of(parametre("abc", "INTEGER", true)));
        when(configurationRepository.findByCle("ABSENTE")).thenReturn(Optional.empty());

        assertEquals(7, configurationService.lireEntier("JOURS_AVANCE_ALERTE_ECHEANCE", 3));
        assertEquals(3, configurationService.lireEntier("MAL_FORME", 3));
        assertEquals(3, configurationService.lireEntier("ABSENTE", 3));
    }
}
