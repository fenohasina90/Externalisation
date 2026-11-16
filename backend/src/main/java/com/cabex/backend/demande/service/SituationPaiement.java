package com.cabex.backend.demande.service;

/**
 * Situation de règlement d'une demande, déduite de l'ensemble de ses plans
 * de paiement et de leurs échéances (une demande multi-services est jugée sur
 * l'ensemble de ses lignes, pas sur le premier plan) ou, en l'absence de
 * plan, de l'avancement de ses lignes.
 *
 * <p>Ordre de précédence — du plus grave au plus favorable :
 * <ol>
 *   <li>PAYE</li>
 *   <li>EN_RETARD</li>
 *   <li>PARTIEL</li>
 *   <li>EN_ATTENTE</li>
 *   <li>NON_PLANIFIE</li>
 * </ol>
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
            // Pas de plan : la demande est encore en attente ; le calcul
            // qui considère aussi les avances sur lignes est fourni par
            // {@link #classer(Object[])}, auquel cas lignes terminées
            // seront transmises comme {@code nombreAvecReste = 0}.
            return EN_ATTENTE;
        }
        return classerEcheance(nombreEcheances, nombreAvecReste, nombreEnRetard, nombrePayees);
    }

    /** Situation d'une ligne agrégée par la requête
     * {@code situations(...)} (colonnes 0..8). */
    public static String classer(Object[] ligne) {
        int nombrePlans = ((Number) ligne[1]).intValue();
        if (nombrePlans > 0) {
            return classerEcheance(((Number) ligne[2]).longValue(),
                    ((Number) ligne[3]).longValue(),
                    ((Number) ligne[4]).longValue(),
                    ((Number) ligne[5]).longValue());
        }
        return classerLignes(((Number) ligne[6]).longValue(),
                ((Number) ligne[7]).longValue(),
                ((Number) ligne[8]).longValue());
    }

    /** Situation dérivée des échéances (préférée dès qu'un plan existe). */
    public static String classerEcheance(long nombreEcheances,
                                         long nombreAvecReste,
                                         long nombreEnRetard,
                                         long nombrePayees) {
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

    /**
     * Situation dérivée des lignes (autre source de vérité, sans plan).
     * PAYE si toutes terminées ; PARTIEL si au moins une ligne soldée mais pas toutes
     * (les autres encore en cours) ou partiellement réglée ; sinon EN_ATTENTE.
     */
    public static String classerLignes(long nombreLignes,
                                        long nombreLignesPayees,
                                        long nombreLignesPartielles) {
        if (nombreLignes == 0) {
            return NON_PLANIFIE;
        }
        if (nombreLignesPayees == nombreLignes) {
            return PAYE;
        }
        if (nombreLignesPartielles > 0 || nombreLignesPayees > 0) {
            return PARTIEL;
        }
        return EN_ATTENTE;
    }
}
