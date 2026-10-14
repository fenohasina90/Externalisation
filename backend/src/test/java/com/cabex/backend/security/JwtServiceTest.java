package com.cabex.backend.security;

import com.cabex.backend.configuration.entity.Configuration;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La durée de session des jetons suit DUREE_SESSION_MINUTES (prochaines
 * connexions), avec repli sur la propriété app.jwt.expiration-ms.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET =
            "aGVsbG8td29ybGQtc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctMjAyNi1jYWJleA";

    @Mock
    private ConfigurationRepository configurationRepository;

    private org.springframework.security.core.userdetails.UserDetails utilisateur() {
        return new User("sarah.cohen", "hash", List.of());
    }

    private void configDuree(String valeur) {
        when(configurationRepository.findByCle("DUREE_SESSION_MINUTES"))
                .thenReturn(Optional.of(Configuration.builder().id(1)
                        .cle("DUREE_SESSION_MINUTES").valeur(valeur)
                        .typeValeur("INTEGER").build()));
    }

    @Test
    void dureeLueDepuisLaConfiguration() {
        configDuree("120");
        JwtService jwtService = new JwtService(SECRET, 3_600_000L, configurationRepository);

        Date expiration = jwtService.extractExpiration(
                jwtService.generateToken(utilisateur()));

        long minutes = (expiration.getTime() - System.currentTimeMillis()) / 60_000L;
        assertTrue(minutes >= 115 && minutes <= 120, "expiration ~120 min, obtenu " + minutes);
    }

    @Test
    void repliSurLaProprieteSiCleAbsente() {
        when(configurationRepository.findByCle("DUREE_SESSION_MINUTES"))
                .thenReturn(Optional.empty());
        JwtService jwtService = new JwtService(SECRET, 3_600_000L, configurationRepository);

        Date expiration = jwtService.extractExpiration(
                jwtService.generateToken(utilisateur()));

        long minutes = (expiration.getTime() - System.currentTimeMillis()) / 60_000L;
        assertTrue(minutes >= 55 && minutes <= 60, "expiration ~60 min, obtenu " + minutes);
    }
}
