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

    public static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank()
                ? null
                : valeur.trim().toUpperCase(Locale.ROOT);
    }
}
