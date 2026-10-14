package com.cabex.backend.demande.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDemandeRequest(
        @NotNull(message = "La date de demande est obligatoire")
        LocalDate dateDemande,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate debutService,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate finService,

        // NULL autorisé : création d'entreprise (société inexistante).
        Integer societeId,

        Integer commercialId,

        Integer clientResponsableId,

        @NotNull(message = "Le service est obligatoire")
        Integer serviceId,

        @NotNull(message = "Le montant total est obligatoire")
        BigDecimal tarif,

        String commentaire
) {
}
