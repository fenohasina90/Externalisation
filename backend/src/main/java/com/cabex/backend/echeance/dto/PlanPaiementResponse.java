package com.cabex.backend.echeance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanPaiementResponse(
        Integer id,
        Integer demandeId,
        String referenceDemande,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal montantTotal,
        BigDecimal restePayer,
        Integer nombreEcheances,
        String commentaire,
        String statut,
        List<EcheanceResponse> echeances
) {
}
