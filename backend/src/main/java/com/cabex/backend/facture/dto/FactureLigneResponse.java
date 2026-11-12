package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureLigneResponse(
        Integer ligneServiceId,
        Integer serviceId,
        String nomService,
        BigDecimal tarif,
        /** Tarif figé à la génération de la proforma (la ligne peut ensuite évoluer). */
        BigDecimal tarifProforma,
        LocalDate debutService,
        LocalDate finService,
        String statutLigne,
        /** Désignation affichée : « Prestation », « Abonnement mensuel » ou nom du service. */
        String libelleLigne,
        /** EXTERNALISATION ou DEBOURS. */
        String categorie
) {
}
