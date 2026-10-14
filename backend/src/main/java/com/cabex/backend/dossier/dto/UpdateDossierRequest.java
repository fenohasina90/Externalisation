package com.cabex.backend.dossier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateDossierRequest(
        @NotBlank(message = "La forme juridique est obligatoire")
        @Size(max = 20, message = "La forme juridique ne doit pas dépasser 20 caractères")
        String formeJuridique,

        @Size(max = 100, message = "La précision ne doit pas dépasser 100 caractères")
        String formeJuridiquePrecision,

        @NotBlank(message = "La dénomination est obligatoire")
        @Size(max = 255, message = "La dénomination ne doit pas dépasser 255 caractères")
        String denomination,

        @Size(max = 255, message = "Le siège social ne doit pas dépasser 255 caractères")
        String siegeSocialActuel,

        @PositiveOrZero(message = "Le capital social doit être positif")
        BigDecimal capitalSocial,

        @PositiveOrZero(message = "Le nombre d'associés doit être positif")
        Integer nombreAssociesDeclare,

        @Size(max = 150, message = "Le nom du gérant ne doit pas dépasser 150 caractères")
        String nomGerant,

        @Size(max = 100, message = "La fonction du gérant ne doit pas dépasser 100 caractères")
        String fonctionGerant,

        Boolean locationGerance,

        @Size(max = 150, message = "Le secteur d'activité ne doit pas dépasser 150 caractères")
        String secteurActivite,

        @Size(max = 255, message = "L'autorisation spéciale ne doit pas dépasser 255 caractères")
        String autorisationSpeciale,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresseSiege,

        @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
        String ville,

        @Size(max = 100, message = "La commune ne doit pas dépasser 100 caractères")
        String commune,

        @Size(max = 100, message = "Le district ne doit pas dépasser 100 caractères")
        String district,

        @Size(max = 100, message = "La région ne doit pas dépasser 100 caractères")
        String region,

        @Size(max = 20, message = "Le type d'hébergement ne doit pas dépasser 20 caractères")
        String typeHebergement,

        @Size(max = 255, message = "La précision ne doit pas dépasser 255 caractères")
        String hebergementPrecision,

        Boolean contratBail,

        @Size(max = 255, message = "Le détail du bail ne doit pas dépasser 255 caractères")
        String contratBailDetail,

        String objetSocial,

        LocalDate dateSouhaiteeDebut,

        @Size(max = 20, message = "Le régime d'activité ne doit pas dépasser 20 caractères")
        String regimeActivite,

        String activitesEtablissement,

        String observations,

        @Size(max = 100, message = "L'assujettissement TVA ne doit pas dépasser 100 caractères")
        String assujettissementTva,

        @Size(max = 255, message = "Le champ autre société ne doit pas dépasser 255 caractères")
        String autreSociete,

        @Size(max = 50, message = "La situation matrimoniale ne doit pas dépasser 50 caractères")
        String situationMatrimoniale,

        @Size(max = 100, message = "Le régime d'imposition ne doit pas dépasser 100 caractères")
        String regimeImposition,

        List<@Valid AssocieRequest> associes
) {
    /** Réutilise la logique de validation et d'application de la création. */
    public CreateDossierRequest versCreation() {
        return new CreateDossierRequest(
                formeJuridique(), formeJuridiquePrecision(), denomination(),
                siegeSocialActuel(), capitalSocial(), nombreAssociesDeclare(),
                nomGerant(), fonctionGerant(), locationGerance(),
                secteurActivite(), autorisationSpeciale(), adresseSiege(),
                ville(), commune(), district(), region(),
                typeHebergement(), hebergementPrecision(), contratBail(),
                contratBailDetail(), objetSocial(), dateSouhaiteeDebut(),
                regimeActivite(), activitesEtablissement(), observations(),
                assujettissementTva(), autreSociete(), situationMatrimoniale(),
                regimeImposition(), associes());
    }
}
