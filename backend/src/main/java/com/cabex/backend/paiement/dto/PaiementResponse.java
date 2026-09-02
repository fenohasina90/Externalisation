package com.cabex.backend.paiement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementResponse(
        Integer id,
        String referencePaiement,
        Integer demandeId,
        String referenceDemande,
        Integer societeId,
        String nomSociete,
        Integer echeanceId,
        Integer modePaiementId,
        String libelleMode,
        LocalDate datePaiement,
        BigDecimal montantPaye,
        String referenceVirement,
        String commentaire,
        String pieceJustificative,
        Integer createdById,
        String createdByName
) {
}
