package com.cabex.backend.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fiche de facturation d'un client (onglet « Facturation » de la fiche 360°).
 * Liste toutes les factures validées liées au client depuis son enregistrement,
 * chaque facture étant rendue avec les paiements qui lui sont liés.
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
        List<PaiementFicheItem> paiements,
        /** Détail des services facturés dans cette facture (type + désignation). */
        List<ObjetLigne> objets
) {

    /** Ligne d'objet d'une facture : type de ligne + désignation lisible (mois inclus pour l'abonnement). */
    public record ObjetLigne(
            /** PRESTATION | DEBOURS | ABONNEMENT_MENSUEL. */
            String type,
            /** Désignation lisible du service / débours (mois en fin pour l'abonnement, ex. « audit fiscal (novembre 2026) »). */
            String libelle
    ) {
    }

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
