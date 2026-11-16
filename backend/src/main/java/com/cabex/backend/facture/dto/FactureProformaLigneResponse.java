package com.cabex.backend.facture.dto;

import java.math.BigDecimal;

/** Ligne persistée de la proforma (mode de vue IDENTIQUE au PDF). */
public record FactureProformaLigneResponse(
        Integer ordre,
        Integer ligneServiceId,
        String designation,
        String categorie,
        String typePrestation,
        BigDecimal pu,
        BigDecimal quantite,
        BigDecimal montantHtva,
        BigDecimal remise,
        BigDecimal netCommercial
) {
}
