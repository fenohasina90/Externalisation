package com.cabex.backend.paiement.dto;

import com.cabex.backend.facture.dto.ObjetLigne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        String montantEnLettres,
        /** Type de facture validée liée (GENERALE | PRESTATION | DEBOURS | ABONNEMENT_MENSUEL). */
        String typeFacture,
        /** Date d'émission de la facture validée liée. */
        LocalDate dateFactureValidee,
        /** Mois facturé (ABONNEMENT_MENSUEL uniquement, sinon null). */
        LocalDate moisFacture,
        /** Toutes les lignes facturées (type + désignation, mois pour l'abonnement). */
        List<ObjetLigne> objets
) {
}
