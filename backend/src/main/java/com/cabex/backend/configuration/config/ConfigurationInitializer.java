package com.cabex.backend.configuration.config;

import com.cabex.backend.backup.service.BackupService;
import com.cabex.backend.configuration.entity.Configuration;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/** Garantit la présence des paramètres ajoutés après l'initialisation de la base. */
@org.springframework.context.annotation.Configuration
public class ConfigurationInitializer {

    @Bean
    public CommandLineRunner initHeureSauvegarde(ConfigurationRepository repository) {
        return args -> {
            if (!repository.existsByCle(BackupService.CLE_HEURE_SAUVEGARDE)) {
                repository.save(Configuration.builder()
                        .cle(BackupService.CLE_HEURE_SAUVEGARDE)
                        .valeur("02:00")
                        .typeValeur("TIME")
                        .description("Heure de la sauvegarde automatique quotidienne (heure du serveur, HH:mm)")
                        .modifiable(true)
                        .build());
            }
        };
    }
}
