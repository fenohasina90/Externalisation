package com.cabex.backend.security;

import com.cabex.backend.exception.ForbiddenException;
import com.cabex.backend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentification requise");
        }
        return principal;
    }

    public static Integer currentUserId() {
        return currentUser().getId();
    }

    public static boolean isSuperAdmin() {
        UserPrincipal principal = currentUser();
        return "SUPER_ADMIN".equalsIgnoreCase(principal.getRoleName());
    }

    public static boolean isAdmin() {
        UserPrincipal principal = currentUser();
        return "SUPER_ADMIN".equalsIgnoreCase(principal.getRoleName())
                || "ADMIN".equalsIgnoreCase(principal.getRoleName());
    }

    public static String currentRoleName() {
        return currentUser().getRoleName();
    }

    /**
     * Vérifie que l'utilisateur authentifié possède la permission demandée.
     * L'utilisateur étant déjà authentifié (sinon {@link #currentUser()} lève
     * un 401), l'absence de permission doit produire un 403 et non un 401 :
     * sinon le frontend déconnecte la session au lieu d'afficher un refus.
     */
    public static void requirePermission(String codePermission) {
        UserPrincipal principal = currentUser();
        if (!principal.hasPermission(codePermission)) {
            throw new ForbiddenException("Permission requise: " + codePermission);
        }
    }
}
