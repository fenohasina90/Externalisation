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
        return classer(ligne, null);
    }

    /** Variante avec le statut de la demande (gardien « Payée seulement si terminée »). */
    public static String classer(Object[] ligne, String statutDemande) {
        int nombrePlans = ((Number) ligne[1]).intValue();
        if (nombrePlans > 0) {
            return avecGateTerminaison(classerEcheance(((Number) ligne[2]).longValue(),
                    ((Number) ligne[3]).longValue(),
                    ((Number) ligne[4]).longValue(),
                    ((Number) ligne[5]).longValue()), statutDemande);
        }
        return avecGateTerminaison(classerLignes(((Number) ligne[6]).longValue(),
                ((Number) ligne[7]).longValue(),
                ((Number) ligne[8]).longValue()), statutDemande);
    }

    /** Situation dérivée des échéances, avec gardien « Payée seulement si terminée ». */
    public static String classer(int nombrePlans,
                                 long nombreEcheances,
                                 long nombreAvecReste,
                                 long nombreEnRetard,
                                 long nombrePayees,
                                 String statutDemande) {
        return avecGateTerminaison(
                classer(nombrePlans, nombreEcheances, nombreAvecReste, nombreEnRetard, nombrePayees),
                statutDemande);
    }

    /** Gardien « Payée si et seulement si la demande est TERMINEE ». */
    private static String avecGateTerminaison(String classement, String statutDemande) {
        if (PAYE.equals(classement) && !"TERMINEE".equals(statutDemande)) {
            return PARTIEL;
        }
        return classement;
    }

    /**
     * Situation de règlement déduite des factures validées et des paiements actifs
     * (source de vérité depuis le passage au modèle « paiement toujours lié à une
     * facture validée »).
     * <ul>
     *   <li>{@code NON_PLANIFIE} : aucune facture validée et aucun paiement ;</li>
     *   <li>{@code EN_RETARD} : reste dû et au moins une facture validée non soldée
     *       datant de plus de {@code joursPrestationDebours} (hors facture mensuelle,
     *       ou valideLe utilisée comme date d'émission) ou {@code joursAbonnement}
     *       (facture mensuelle) ;</li>
     *   <li>{@code PARTIEL} : reste dû, aucun retard, mais au moins un paiement
     *       enregistré (ou tout réglé mais demande encore non terminée) ;</li>
     *   <li>{@code PAYE} : tout réglé ET demande {@code TERMINEE} ;</li>
     *   <li>{@code EN_ATTENTE} : rien payé et reste dû.</li>
     * </ul>
     *
     * @param lignes colonnes 0..5 issues de {@code facturesAgregees(...)} :
     *               demande.id, f.id, montant, paye (COALESCE 0), valideLe, mois
     * @param aujourdhui  date du jour (retards)
     */
    public static String classerFacture(java.util.List<Object[]> lignes,
                                        int joursPrestationDebours,
                                        int joursAbonnement,
                                        java.time.LocalDate aujourdhui,
                                        String statutDemande) {
        if (lignes == null || lignes.isEmpty()) {
            return NON_PLANIFIE;
        }
        java.math.BigDecimal totalFacture = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPaye = java.math.BigDecimal.ZERO;
        long nbRetards = 0;
        for (Object[] ligne : lignes) {
            java.math.BigDecimal montant = toBigDecimal(ligne[2]);
            java.math.BigDecimal paye = toBigDecimal(ligne[3]);
            totalFacture = totalFacture.add(montant);
            totalPaye = totalPaye.add(paye);
            if (paye.compareTo(montant) < 0 && ligne[4] != null) {
                java.time.LocalDate emission = toLocalDate(ligne[4]);
                java.time.LocalDate mois = ligne[5] != null && ligne[5] instanceof java.time.LocalDate m
                        ? m : null;
                int limite = mois != null ? joursAbonnement : joursPrestationDebours;
                if (limite >= 0 && aujourdhui.isAfter(emission.plusDays(limite))) {
                    nbRetards++;
                }
            }
        }
        java.math.BigDecimal reste = totalFacture.subtract(totalPaye);
        if (reste.signum() <= 0) {
            // Tout est réglé : Payée si et seulement si la demande est terminée.
            return "TERMINEE".equals(statutDemande) ? PAYE : PARTIEL;
        }
        if (nbRetards > 0) {
            return EN_RETARD;
        }
        return totalPaye.signum() > 0 ? PARTIEL : EN_ATTENTE;
    }

    private static java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (value instanceof java.math.BigDecimal bd) {
            return bd;
        }
        return new java.math.BigDecimal(value.toString());
    }

    private static java.time.LocalDate toLocalDate(Object value) {
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.toLocalDate();
        }
        if (value instanceof java.time.LocalDate ld) {
            return ld;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        throw new IllegalArgumentException("Type de date inattendu pour la facture validée : " + value.getClass());
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
