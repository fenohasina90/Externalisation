package com.cabex.backend.demande.service;

/**
 * Situation de règlement d'une demande, déduite des factures validées et des
 * paiements actifs (source de vérité). Pour les demandes sans facture, repli
 * sur l'avancement des lignes.
 *
 * <p>Ordre de précédence : PAYE &gt; EN_RETARD &gt; PARTIEL &gt; EN_ATTENTE ou NON_PLANIFIE.</p>
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
     * Situation dérivée des factures validées et des paiements actifs.
     *<ul>
     *   <li>{@code NON_PLANIFIE} : aucune facture validée et aucun paiement enregistré ;</li>
     *   <li>{@code EN_RETARD} : reste dû et au moins une facture validée non soldée
     *       datant de plus de {@code joursPrestationDebours} (hors facture mensuelle,
     *       ou valideLe utilisée comme date d'émission) ou {@code joursAbonnement}
     *       (facture mensuelle) ;</li>
     *   <li>{@code PARTIEL} : reste dû, aucun retard, mais au moins un paiement enregistré ;</li>
     *   <li>{@code PAYE} : tout réglé ET demande {@code TERMINEE} ;</li>
     *   <li>{@code EN_ATTENTE} : rien payé et reste dû.</li>
     *</ul>
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
            return "TERMINEE".equals(statutDemande) ? PAYE : PARTIEL;
        }
        if (nbRetards > 0) {
            return EN_RETARD;
        }
        return totalPaye.signum() > 0 ? PARTIEL : EN_ATTENTE;
    }

    /**
     * Situation dérivée des lignes (demande sans facture), avec gardien
     * « Payée seulement si TERMINEE ».
     */
    public static String classerLignes(long nombreLignes,
                                       long nombreLignesPayees,
                                       long nombreLignesPartielles,
                                       String statutDemande) {
        if (nombreLignes == 0) {
            return NON_PLANIFIE;
        }
        if (nombreLignesPayees == nombreLignes) {
            return "TERMINEE".equals(statutDemande) ? PAYE : PARTIEL;
        }
        if (nombreLignesPartielles > 0 || nombreLignesPayees > 0) {
            return PARTIEL;
        }
        return EN_ATTENTE;
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
}
