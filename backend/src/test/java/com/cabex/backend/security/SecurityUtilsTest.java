package com.cabex.backend.security;

import com.cabex.backend.exception.ForbiddenException;
import com.cabex.backend.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilsTest {

    @AfterEach
    void nettoyerContexte() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier(Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(
                1, "admin", "admin@cabex.mg", "hash", true,
                1, "ADMIN", permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void permissionAbsenteLeveForbidden403EtNon401() {
        authentifier(Set.of("DEMANDE_LIRE"));

        assertThrows(ForbiddenException.class,
                () -> SecurityUtils.requirePermission("EXPORT_EXCEL"));
    }

    @Test
    void permissionPresenteAutorise() {
        authentifier(Set.of("EXPORT_EXCEL"));

        assertDoesNotThrow(() -> SecurityUtils.requirePermission("EXPORT_EXCEL"));
    }

    @Test
    void utilisateurNonAuthentifieLeveToujours401() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, SecurityUtils::currentUser);
    }
}
