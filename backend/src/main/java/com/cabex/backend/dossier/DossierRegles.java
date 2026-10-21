package com.cabex.backend.dossier;

import java.util.Set;

/**
 * Règles partagées du dossier de création d'entreprise ou de société.
 * Un dossier ne peut être lié qu'à une demande dont le service est de
 * type CREATION (colonne explicite, insensible à la casse).
 */
public final class DossierRegles {

    private DossierRegles() {
    }

    public static final Set<String> TYPES_SERVICE = Set.of(
            "STANDARD", "CREATION", "TRANSACTION_IMMOBILIERE");

    public static final Set<String> FORMES_JURIDIQUES = Set.of(
            "INDIVIDUELLE", "SARL", "SARLU", "SA", "AUTRE");

    public static final Set<String> TYPES_HEBERGEMENT = Set.of(
            "DOMICILE_GERANT", "LOCATION_CABINET", "AUTRE");

    public static final Set<String> REGIMES_ACTIVITE = Set.of(
            "PERMANENTE", "SAISONNIERE", "AMBULANT");

    public static boolean estTypeCreation(String typeService) {
        return typeService != null && "CREATION".equalsIgnoreCase(typeService.trim());
    }

    /** Un service de transaction immobilière porte une fiche dédiée. */
    public static boolean estTypeTransactionImmobiliere(String typeService) {
        return typeService != null
                && "TRANSACTION_IMMOBILIERE".equalsIgnoreCase(typeService.trim());
    }

    /** Une société (tout sauf Individuelle) exige des associés. */
    public static boolean estSociete(String formeJuridique) {
        return formeJuridique != null && !"INDIVIDUELLE".equalsIgnoreCase(formeJuridique.trim());
    }
}
