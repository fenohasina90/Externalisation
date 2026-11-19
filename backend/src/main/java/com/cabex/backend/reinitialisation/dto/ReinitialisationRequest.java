package com.cabex.backend.reinitialisation.dto;

import java.util.List;

/**
 * Requête de réinitialisation de données : la liste des cibles à supprimer et
 * la confirmation explicite (le mot « SUPPRIMER ») côté client.
 */
public record ReinitialisationRequest(
        List<String> cibles,
        String confirmation
) {
}
