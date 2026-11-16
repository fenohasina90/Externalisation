package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * Création de la facture du mois d'un abonnement mensuel.
 * Le montant de base est le montant de la facture générale validée.
 * Des éléments impayés (prestation / débours) de la même société peuvent être
 * ajoutés ({@code inclusionLigneIds}) ; la facture est alors multi-lignes.
 * La facture porte le mois facturé (ex. « janvier 2026 ») et la date d'émission
 * saisie par l'utilisateur.
 */
public record CreerFactureAbonnementRequest(
        @NotNull(message = "La ligne d'abonnement est obligatoire")
        Integer ligneServiceId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        /** Mois facturé (format AAAA-MM, ex. « 2026-01 »). */
        @NotNull(message = "Le mois facturé est obligatoire")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Le mois doit être au format AAAA-MM (ex. 2026-01)")
        String mois,

        String commentaire,

        /** IDs des lignes prestation / débours à inclure dans la facture. */
        List<Integer> inclusionLigneIds
) {
}
