package com.cabex.backend.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementFicheResponse(
        Integer id,
        String referencePaiement,
        String referenceDemande,
        Integer demandeId,
        String libelleMode,
        LocalDate datePaiement,
        BigDecimal montantPaye,
        String referenceVirement
) {
}
