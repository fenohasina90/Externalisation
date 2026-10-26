package com.cabex.backend.configuration.dto;

/** Paramètres d'affichage publics (accessibles sans authentification). */
public record ConfigurationPubliqueResponse(
        String nomApplication,
        String devise,
        int taillePagination
) {
}
