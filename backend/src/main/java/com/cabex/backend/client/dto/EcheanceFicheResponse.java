package com.cabex.backend.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheanceFicheResponse(
        Integer id,
        String referenceDemande,
        Integer demandeId,
        Integer numeroEcheance,
        LocalDate dateEcheance,
        BigDecimal montantAttendu,
        BigDecimal montantPaye,
        BigDecimal reste,
        String statut
) {
}
