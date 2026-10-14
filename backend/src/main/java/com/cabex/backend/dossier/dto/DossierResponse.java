package com.cabex.backend.dossier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DossierResponse(
        Integer id,
        Integer demandeId,
        String referenceDemande,
        // Lus depuis la demande d'origine (non stockés dans le dossier).
        LocalDate dateDemande,
        String numeroIm,
        String formeJuridique,
        String formeJuridiquePrecision,
        String denomination,
        String siegeSocialActuel,
        BigDecimal capitalSocial,
        Integer nombreAssociesDeclare,
        String nomGerant,
        String fonctionGerant,
        Boolean locationGerance,
        String secteurActivite,
        String autorisationSpeciale,
        String adresseSiege,
        String ville,
        String commune,
        String district,
        String region,
        String typeHebergement,
        String hebergementPrecision,
        Boolean contratBail,
        String contratBailDetail,
        String objetSocial,
        LocalDate dateSouhaiteeDebut,
        String regimeActivite,
        String activitesEtablissement,
        String observations,
        String assujettissementTva,
        String autreSociete,
        String situationMatrimoniale,
        String regimeImposition,
        List<AssocieResponse> associes
) {
}
