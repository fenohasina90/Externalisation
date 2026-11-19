package com.cabex.backend.facture.dto;

/**
 * Ligne de l'objet d'une facture (onglet Facturation, consultation paiement, reçus PDF) :
 * type de ligne + désignation lisible (mois en fin pour l'abonnement,
 * ex. « Audit fiscal (novembre 2026) »).
 */
public record ObjetLigne(
        /** PRESTATION | DEBOURS | ABONNEMENT_MENSUEL. */
        String type,
        /** Désignation lisible du service / débours (mois inclus pour l'abonnement). */
        String libelle
) {
}
