package com.cabex.backend.demande.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLigneRequest(
        /**
         * Service du catalogue. Requis si catégorie EXTERNALISATION,
         * interdit si DEBOURS (validation côté service).
         */
        Integer serviceId,

        /**
         * Tarif figé. Requis si EXTERNALISATION, null si DEBOURS
         * (le montant est saisi ensuite via « Valider le montant »).
         */
        BigDecimal tarif,

        LocalDate debutService,

        LocalDate finService,

        /**
         * Catégorie de la ligne : EXTERNALISATION (défaut si null) ou DEBOURS.
         */
        String categorie,

        /**
         * Désignation du débours. Requise si DEBOURS, null si EXTERNALISATION.
         */
        String designationDebours,

        /**
         * Sous-type pour EXTERNALISATION + service STANDARD :
         * PRESTATION (défaut si null) ou ABONNEMENT_MENSUEL.
         * Interdit (null) pour DEBOURS / CREATION / TRANSACTION.
         */
        String typePrestation
) {
}
