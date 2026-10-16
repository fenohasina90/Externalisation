package com.cabex.backend.demande.dto;

/**
 * Modification d'une demande EN_ATTENTE : commercial et commentaire.
 * Les services (tarifs, périodes) se modifient via les lignes de service.
 */
public record UpdateDemandeRequest(
        Integer commercialId,

        String commentaire
) {
}
