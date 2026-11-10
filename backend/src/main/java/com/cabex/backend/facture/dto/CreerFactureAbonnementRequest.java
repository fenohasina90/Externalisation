package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Création de la facture du mois d'un abonnement mensuel.
 * Le montant est le tarif mensuel de la ligne d'abonnement.
 * La date d'émission est celle saisie par l'utilisateur
 * (elle date la facture ; l'horodatage réel reste dans created_at).
 */
public record CreerFactureAbonnementRequest(
        @NotNull(message = "La ligne d'abonnement est obligatoire")
        Integer ligneServiceId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        String commentaire
) {
}
