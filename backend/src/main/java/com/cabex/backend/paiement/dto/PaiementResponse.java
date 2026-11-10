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
        Integer factureValideeId,
        String referenceFactureValidee,
        /** Échéance réglée (null pour DEBOURS / PRESTATION / ABONNEMENT). */
        Integer echeanceId,
        Integer modePaiementId,
        String libelleMode,
        LocalDate datePaiement,
        BigDecimal montantPaye,
        String referenceVirement,
        /** Banque émettrice du chèque (mode CHÈQUE, sinon null). */
        String banque,
        /** Numéro de chèque (mode CHÈQUE, sinon null). */
        String numeroCheque,
        String commentaire,
        String pieceJustificative,
        Integer createdById,
        String createdByName
) {
}
