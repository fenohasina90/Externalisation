package com.cabex.backend.dossier;

import java.text.Normalizer;
import java.util.Set;

/**
 * Règles partagées du dossier de création d'entreprise ou de société.
 * Un dossier ne peut être lié qu'à une demande dont le service est de
 * type création (détection sur le nom du service, insensible aux
 * accents et à la casse).
 */
public final class DossierRegles {

    private DossierRegles() {
    }

    public static final Set<String> FORMES_JURIDIQUES = Set.of(
            "INDIVIDUELLE", "SARL", "SARLU", "SA", "AUTRE");

    public static final Set<String> TYPES_HEBERGEMENT = Set.of(
            "DOMICILE_GERANT", "LOCATION_CABINET", "AUTRE");

    public static final Set<String> REGIMES_ACTIVITE = Set.of(
            "PERMANENTE", "SAISONNIERE", "AMBULANT");

    public static boolean estServiceCreation(String nomService) {
        if (nomService == null) {
            return false;
        }
        String normalise = Normalizer.normalize(nomService.toLowerCase(java.util.Locale.ROOT),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalise.contains("creation")
                && (normalise.contains("entreprise") || normalise.contains("societe"));
    }

    /** Une société (tout sauf Individuelle) exige des associés. */
    public static boolean estSociete(String formeJuridique) {
        return formeJuridique != null && !"INDIVIDUELLE".equalsIgnoreCase(formeJuridique.trim());
    }
}
