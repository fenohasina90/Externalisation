package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Création de la facture du mois d'un abonnement mensuel.
 * Le montant de base est le tarif mensuel de la ligne d'abonnement.
 * Des éléments impayés (prestation / débours) de la même société peuvent être
 * ajoutés ({@code inclusionLigneIds}) ; la facture est alors multi-lignes.
 * La date d'émission est celle saisie par l'utilisateur.
 */
public record CreerFactureAbonnementRequest(
        @NotNull(message = "La ligne d'abonnement est obligatoire")
        Integer ligneServiceId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        String commentaire,

        /** IDs des lignes prestation / débours à inclure dans la facture. */
        List<Integer> inclusionLigneIds
) {
}
