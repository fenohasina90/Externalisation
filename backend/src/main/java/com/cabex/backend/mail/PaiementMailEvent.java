package com.cabex.backend.mail;

/**
 * Événement publié après l'enregistrement d'un paiement. L'e-mail au client
 * (facture liée + reçu) est envoyé en arrière-plan une fois la transaction
 * validée, pour ne pas ralentir la confirmation de l'encaissement.
 */
public record PaiementMailEvent(
        Integer paiementId,
        String referencePaiement,
        Integer factureValideeId,
        Integer demandeId
) {
}
