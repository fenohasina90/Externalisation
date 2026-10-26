package com.cabex.backend.paiement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Détail d'un paiement reprenant les informations du reçu PDF
 * (service et échéance concernés, montants de l'échéance, montant en lettres).
 */
public record PaiementDetailResponse(
        Integer id,
        String referencePaiement,
        Integer demandeId,
        String referenceDemande,
        Integer societeId,
        String nomSociete,
        Integer echeanceId,
        Integer numeroEcheance,
        LocalDate dateEcheance,
        String nomService,
        Integer modePaiementId,
        String libelleMode,
        LocalDate datePaiement,
        BigDecimal montantPaye,
        String referenceVirement,
        String commentaire,
        String pieceJustificative,
        Integer createdById,
        String createdByName,
        BigDecimal montantAttendu,
        BigDecimal montantPayeEcheance,
        BigDecimal resteAPayer,
        String montantEnLettres
) {
}
