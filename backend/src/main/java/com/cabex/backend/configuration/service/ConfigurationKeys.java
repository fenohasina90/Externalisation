package com.cabex.backend.configuration.service;

/** Clés de configuration applicative utilisées par les traitements métier. */
public final class ConfigurationKeys {

    public static final String JOURS_AVANCE_ALERTE_ECHEANCE = "JOURS_AVANCE_ALERTE_ECHEANCE";
    public static final int JOURS_AVANCE_ALERTE_ECHEANCE_DEFAUT = 3;

    /**
     * Alerte de retard de paiement (prestation et débours) : nombre de jours
     * après la validation de la facture avant de déclencher la notification.
     */
    public static final String JOURS_RETARD_PRESTATION_DEBOURS = "JOURS_RETARD_PRESTATION_DEBOURS";
    public static final int JOURS_RETARD_PRESTATION_DEBOURS_DEFAUT = 5;

    /**
     * Alerte de retard de paiement (abonnement mensuel) : nombre de jours
     * après la date d'émission de la facture mensuelle avant de déclencher la notification.
     */
    public static final String JOURS_RETARD_ABONNEMENT = "JOURS_RETARD_ABONNEMENT";
    public static final int JOURS_RETARD_ABONNEMENT_DEFAUT = 7;

    /**
     * Jour du mois de création de la facture mensuelle d'abonnement — rappel
     * uniquement ; la facture peut toujours être créée à n'importe quelle date.
     */
    public static final String JOUR_CREATION_FACTURE_MENSUELLE = "JOUR_CREATION_FACTURE_MENSUELLE";
    public static final int JOUR_CREATION_FACTURE_MENSUELLE_DEFAUT = 15;

    /**
     * Nombre de jours avant la date de création mensuelle prévue pour envoyer
     * un rappel de création (le jour J, un rappel est également émis).
     */
    public static final String JOURS_RAPPEL_AVANT_CREATION_MENSUELLE = "JOURS_RAPPEL_AVANT_CREATION_MENSUELLE";
    public static final int JOURS_RAPPEL_AVANT_CREATION_MENSUELLE_DEFAUT = 5;

    public static final String HEURE_SAUVEGARDE_AUTOMATIQUE = "HEURE_SAUVEGARDE_AUTOMATIQUE";

    private ConfigurationKeys() {
    }
}
