package com.cabex.backend.auth.config;

import com.cabex.backend.utilisateur.entity.RoleUtilisateur;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.RoleUtilisateurRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Crée un compte SUPER_ADMIN par défaut si aucun utilisateur n'existe.
 * Les identifiants par défaut doivent être changés après la première connexion.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initAdmin(UtilisateurRepository utilisateurRepository,
                                       RoleUtilisateurRepository roleRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            if (utilisateurRepository.count() == 0) {
                RoleUtilisateur superAdminRole = roleRepository.findByNomRole("SUPER_ADMIN")
                        .orElseThrow(() -> new IllegalStateException(
                                "Le rôle SUPER_ADMIN n'existe pas en base. Vérifiez le script d'initialisation."));

                String defaultPassword = "admin123";
                Utilisateur admin = Utilisateur.builder()
                        .nom("Administrateur")
                        .prenom("Système")
                        .email("admin@cabex.mg")
                        .mdpHash(passwordEncoder.encode(defaultPassword))
                        .role(superAdminRole)
                        .actif(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                utilisateurRepository.save(admin);
                log.info("Compte administrateur par défaut créé : admin@cabex.mg / {}", defaultPassword);
            }
        };
    }
}
