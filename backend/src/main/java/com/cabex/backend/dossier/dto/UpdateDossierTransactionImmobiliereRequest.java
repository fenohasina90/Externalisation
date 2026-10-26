package com.cabex.backend.dossier.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateDossierTransactionImmobiliereRequest(
        @Size(max = 255, message = "Le nom du propriétaire précédent ne doit pas dépasser 255 caractères")
        String nomProprietairePrecedent,

        Boolean actionAchat,
        Boolean actionVente,
        Boolean actionLocation,
        Boolean actionRegularisationPapiers,
        Boolean actionGestionImmobiliere,
        Boolean actionAutre,

        @Size(max = 255, message = "La précision de l'action ne doit pas dépasser 255 caractères")
        String actionAutrePrecision,

        @Size(max = 20, message = "L'objet ne doit pas dépasser 20 caractères")
        String objet,

        @Size(max = 255, message = "La précision de l'objet ne doit pas dépasser 255 caractères")
        String objetAutrePrecision,

        @PositiveOrZero(message = "La mesure d'achat doit être positive")
        BigDecimal achatMesureM2,

        @Size(max = 255, message = "La position d'achat ne doit pas dépasser 255 caractères")
        String achatPosition,

        @Size(max = 255, message = "L'axe ou emplacement d'achat ne doit pas dépasser 255 caractères")
        String achatAxeEmplacement,

        @PositiveOrZero(message = "Le montant d'achat doit être positif")
        BigDecimal achatMontant,

        Boolean achatPermisConstruire,

        @PositiveOrZero(message = "La mesure de vente doit être positive")
        BigDecimal venteMesureM2,

        @Size(max = 255, message = "La position de vente ne doit pas dépasser 255 caractères")
        String ventePosition,

        @Size(max = 255, message = "L'axe ou emplacement de vente ne doit pas dépasser 255 caractères")
        String venteAxeEmplacement,

        Boolean ventePrisePhoto,
        Boolean venteVisite,

        @PositiveOrZero(message = "Le montant de vente doit être positif")
        BigDecimal venteMontant,

        Boolean ventePermisConstruire,

        @Size(max = 30, message = "Le type de terrain ne doit pas dépasser 30 caractères")
        String regularisationTypeTerrain,

        @Size(max = 30, message = "La situation du terrain ne doit pas dépasser 30 caractères")
        String regularisationSituationTerrain,

        Boolean documentPvReconnaissance,
        Boolean documentNumeroDemandeAcquisition,
        Boolean documentActeVentePriveeCommunale,
        Boolean documentSituationJuridique,
        Boolean documentActeNotoriete,

        String observations,

        @PositiveOrZero(message = "Le nombre de commerciaux doit être positif")
        Integer nombreCommerciauxTerrain,

        @PositiveOrZero(message = "La commission CN doit être positive")
        BigDecimal commissionCn,

        Boolean prestationAbonnementMensuel
) {
    public CreateDossierTransactionImmobiliereRequest versCreation() {
        return new CreateDossierTransactionImmobiliereRequest(
                nomProprietairePrecedent(),
                actionAchat(), actionVente(), actionLocation(),
                actionRegularisationPapiers(), actionGestionImmobiliere(), actionAutre(),
                actionAutrePrecision(), objet(), objetAutrePrecision(),
                achatMesureM2(), achatPosition(), achatAxeEmplacement(), achatMontant(),
                achatPermisConstruire(),
                venteMesureM2(), ventePosition(), venteAxeEmplacement(),
                ventePrisePhoto(), venteVisite(), venteMontant(), ventePermisConstruire(),
                regularisationTypeTerrain(), regularisationSituationTerrain(),
                documentPvReconnaissance(), documentNumeroDemandeAcquisition(),
                documentActeVentePriveeCommunale(), documentSituationJuridique(),
                documentActeNotoriete(),
                observations(), nombreCommerciauxTerrain(), commissionCn(),
                prestationAbonnementMensuel());
    }
}
