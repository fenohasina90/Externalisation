package com.cabex.backend.echeance.dto;

/**
 * Cible d'une échéance : permet au frontend d'ouvrir l'échéancier de la demande
 * concernée depuis une notification (dont le referenceId est l'id de l'échéance).
 */
public record EcheanceCibleResponse(
        Integer id,
        Integer numeroEcheance,
        Integer demandeId,
        String referenceDemande
) {
}
