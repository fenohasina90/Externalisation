package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FactureResponse(
        Integer id,
        String referenceFacture,
        String referenceFactureValidee,
        /** ID de la facture validée liée (null tant que la proforma n'est pas validée). */
        Integer factureValideeId,
        Integer demandeId,
        String referenceDemande,
        String modeFacturation,
        /** Montant proforma (modifiable tant que PROFORMA, conservé après validation). */
        BigDecimal montantProforma,
        /** Montant validé (null tant que PROFORMA). */
        BigDecimal montantValidee,
        String statut,
        Integer valideParId,
        String valideParNom,
        LocalDateTime valideLe,
        List<FactureLigneResponse> lignes,
        /** Lignes persistées de la proforma (ordre d'affichage, figées à la génération). */
        List<FactureProformaLigneResponse> lignesProforma
) {
}
