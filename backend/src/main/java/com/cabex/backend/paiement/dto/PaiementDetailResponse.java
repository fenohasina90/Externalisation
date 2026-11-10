package com.cabex.backend.paiement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Détail d'un paiement reprenant les informations du reçu PDF
 * (facture validée concernée, montants, montant en lettres).
 */
public record PaiementDetailResponse(
        Integer id,
        String referencePaiement,
        Integer demandeId,
        String referenceDemande,
        Integer societeId,
        String nomSociete,
        Integer factureValideeId,
        String referenceFactureValidee,
        String nomService,
        /** Échéance réglée (null pour DEBOURS / PRESTATION / ABONNEMENT). */
        Integer echeanceId,
        Integer numeroEcheance,
        java.time.LocalDate dateEcheance,
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
        String createdByName,
        BigDecimal montantFactureValidee,
        BigDecimal montantPayeSurFacture,
        BigDecimal resteAPayerFacture,
        String montantEnLettres
) {
}
