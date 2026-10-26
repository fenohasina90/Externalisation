package com.cabex.backend.demande.service;

/**
 * Situation de règlement d'une demande, déduite de l'ensemble de ses plans
 * de paiement et de leurs échéances (une demande multi-services est jugée sur
 * l'ensemble de ses lignes, pas sur le premier plan).
 *
 * <p>Le classement reprend l'ordre de précédence appliqué aux échéances
 * ({@code EcheanceMapper.calculerStatutEcheance}) : soldé, puis en retard,
 * puis partiel, puis en attente. Les situations sont mutuellement exclusives
 * et couvrent toutes les demandes — y compris celles sans plan de paiement.
 */
public final class SituationPaiement {

    public static final String PAYE = "PAYE";
    public static final String EN_RETARD = "EN_RETARD";
    public static final String PARTIEL = "PARTIEL";
    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String NON_PLANIFIE = "NON_PLANIFIE";

    private SituationPaiement() {
    }

    /**
     * @param nombrePlans         plans de paiement rattachés à la demande
     * @param nombreEcheances     échéances, tous plans confondus
     * @param nombreAvecReste     échéances dont le montant payé est inférieur à l'attendu
     * @param nombreEnRetard      échéances dépassées avec un reste à payer
     * @param nombrePayees        échéances ayant déjà reçu un paiement
     */
    public static String classer(int nombrePlans,
                                 long nombreEcheances,
                                 long nombreAvecReste,
                                 long nombreEnRetard,
                                 long nombrePayees) {
        if (nombrePlans == 0) {
            return NON_PLANIFIE;
        }
        if (nombreEcheances == 0) {
            // Planifié mais aucun découpage : rien à régler pour l'instant.
            return EN_ATTENTE;
        }
        if (nombreAvecReste == 0) {
            return PAYE;
        }
        if (nombreEnRetard > 0) {
            return EN_RETARD;
        }
        return nombrePayees > 0 ? PARTIEL : EN_ATTENTE;
    }

    /** Situation d'une ligne agrégée par la requête {@code situations}. */
    public static String classer(Object[] ligne) {
        return classer(
                ((Number) ligne[1]).intValue(),
                ((Number) ligne[2]).longValue(),
                ((Number) ligne[3]).longValue(),
                ((Number) ligne[4]).longValue(),
                ((Number) ligne[5]).longValue());
    }
}