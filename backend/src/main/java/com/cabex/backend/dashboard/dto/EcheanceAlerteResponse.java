package com.cabex.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheanceAlerteResponse(
        Long echeanceId,
        Integer clientId,
        String nomSociete,
        Integer demandeId,
        String referenceDemande,
        LocalDate dateEcheance,
        BigDecimal montantAttendu,
        BigDecimal montantPaye,
        BigDecimal reste,
        String statut
) {
}
