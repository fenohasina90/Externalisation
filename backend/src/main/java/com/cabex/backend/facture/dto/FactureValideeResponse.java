package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Facture validée, telle qu'affichée à la saisie d'un paiement.
 *
 * <p>Références à afficher selon le cas :
 * <ul>
 *   <li>EXTERNALISATION / débours simple : référence de la facture validée,
 *       référence proforma (si issue d'une proforma) et référence demande ;</li>
 *   <li>DEBOURS : pas de proforma ;</li>
 *   <li>ABONNEMENT_MENSUEL (facture du mois) : référence de la facture
 *       mensuelle, référence de la facture générale validée, référence
 *       proforma et référence demande.</li>
 * </ul>
 */
public record FactureValideeResponse(
        Integer id,
        String referenceFactureValidee,
        /** Référence proforma (null pour débours / factures mensuelles). */
        String referenceFactureProforma,
        /** Référence de la facture générale validée (ABONNEMENT_MENSUEL). */
        String referenceFactureGenerale,
        Integer factureGeneraleId,
        Integer demandeId,
        String referenceDemande,
        Integer ligneServiceId,
        String libelleLigne,
        BigDecimal montant,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        /** GENERALE (une proforma validée), ABONNEMENT_MENSUEL (facture du mois) ou DEBOURS. */
        String type,
        String valideParNom,
        LocalDateTime valideLe
) {
}
