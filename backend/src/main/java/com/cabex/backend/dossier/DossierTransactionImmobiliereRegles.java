package com.cabex.backend.dossier;

import java.util.Locale;
import java.util.Set;

public final class DossierTransactionImmobiliereRegles {

    private DossierTransactionImmobiliereRegles() {
    }

    public static final Set<String> OBJETS = Set.of("TERRAIN", "MAISON", "AUTRE");

    public static final Set<String> TYPES_TERRAIN = Set.of(
            "COMMUNAL_DOMAINE", "PRIVE", "HERITAGE_DON");

    public static final Set<String> SITUATIONS_TERRAIN = Set.of(
            "TITRE_BORNE", "RECONNAISSANCE");

    public static final Set<String> TYPES_PRESTATION_DOSSIER = Set.of(
            "PRESTATION", "ABONNEMENT_MENSUEL");

    /**
     * Sous-type de prestation du dossier transaction (suivi) :
     * PRESTATION (défaut, payable après facture validée) ou ABONNEMENT_MENSUEL.
     * Renvoie PRESTATION si absent, rejette toute autre valeur.
     */
    public static String normaliserTypePrestationDossier(String valeur) {
        String normalise = normaliser(valeur);
        if (normalise == null) {
            return "PRESTATION";
        }
        return TYPES_PRESTATION_DOSSIER.contains(normalise) ? normalise : null;
    }

    public static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank()
                ? null
                : valeur.trim().toUpperCase(Locale.ROOT);
    }
}
