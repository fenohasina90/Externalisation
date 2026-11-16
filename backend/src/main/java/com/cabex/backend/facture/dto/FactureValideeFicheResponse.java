package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fiche de facturation d'un client (onglet « Facturation » de la fiche 360°).
 * Liste toutes les factures validées liées au client depuis son enregistrement,
 * groupées côté frontend en :
 * <ul>
 *   <li>facture « générale » (type {@link #GENERALE}) ;</li>
 *   <li>factures « de paiement » enfants ({@link #PRESTATION},
 *       {@link #DEBOURS}, {@link #ABONNEMENT_MENSUEL}) ;</li>
 *   <li>paiements rattachés à chaque facture (sous-contenu).</li>
 * </ul>
 */
public record FactureValideeFicheResponse(
        Integer id,
        String referenceFactureValidee,
        /** Type : GENERALE | PRESTATION | DEBOURS | ABONNEMENT_MENSUEL. */
        String type,
        Integer factureGeneraleId,
        String referenceFactureGenerale,
        Integer demandeId,
        String referenceDemande,
        /** Objet précis : nom du service, désignation du débours ou « Facture générale ». */
        String objet,
        /** Type sous-jacent : PRESTATION | DEBOURS | ABONNEMENT_MENSUEL (ou GENERALE si indéterminée). */
        String objetType,
        /** Mois facturé au format AAAA-MM (ABONNEMENT_MENSUEL uniquement). */
        String mois,
        BigDecimal montant,
        BigDecimal montantPaye,
        BigDecimal resteAPayer,
        LocalDate dateFactureValidee,
        List<PaiementFicheItem> paiements
) {

    public record PaiementFicheItem(
            Integer id,
            String referencePaiement,
            String libelleMode,
            /** Code du mode (ESPECES, VIREMENT, CHEQUE, ...). */
            String codeMode,
            LocalDate datePaiement,
            BigDecimal montantPaye,
            String referenceVirement,
            String banque,
            String numeroCheque,
            String commentaire,
            String createdByName
    ) {
    }
}
