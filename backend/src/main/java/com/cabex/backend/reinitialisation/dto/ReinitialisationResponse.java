package com.cabex.backend.reinitialisation.dto;

import java.util.Map;

/**
 * Résultat d'une réinitialisation : lignes supprimées par table et fichiers
 * retirés du disque (pièces jointes).
 */
public record ReinitialisationResponse(
        /** Nombre de lignes supprimées, par table. */
        Map<String, Long> compteurs,
        /** Fichiers de pièces jointes réellement supprimés du disque. */
        long fichiersSupprimes,
        /** Fichiers introuvables sur le disque au moment de la suppression. */
        long fichiersIntrouvables
) {
}
