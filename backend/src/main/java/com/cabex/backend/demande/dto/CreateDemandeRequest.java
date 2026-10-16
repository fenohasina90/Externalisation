package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateDemandeRequest(
        @NotNull(message = "La date de demande est obligatoire")
        LocalDate dateDemande,

                LocalDate debutService,

                LocalDate finService,

        // NULL autorisé : création d'entreprise (société inexistante).
        Integer societeId,

        Integer commercialId,

        Integer clientResponsableId,

                Integer serviceId,

                BigDecimal tarif,

        String commentaire,

        /**
         * Lignes de service (multi-services). Au moins une ligne requise,
         * directement ou via les champs historiques.
         */
        List<CreateLigneRequest> lignes,

        /**
         * Mode de facturation : UNIQUE (une proforma pour la demande) ou
         * PAR_SERVICE (une proforma par ligne). Défaut PAR_SERVICE.
         */
        String modeFacturation
) {
}
