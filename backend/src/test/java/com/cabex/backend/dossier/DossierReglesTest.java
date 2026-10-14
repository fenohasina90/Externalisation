package com.cabex.backend.dossier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DossierReglesTest {

    @Test
    void serviceCreationDetecte() {
        assertTrue(DossierRegles.estServiceCreation("Création d'entreprise"));
        assertTrue(DossierRegles.estServiceCreation("CREATION D'ENTREPRISE"));
        assertTrue(DossierRegles.estServiceCreation("Création de société"));
        assertTrue(DossierRegles.estServiceCreation("  création d’Entreprise  "));
    }

    @Test
    void serviceNonCreationRejete() {
        assertFalse(DossierRegles.estServiceCreation("Audit comptable"));
        assertFalse(DossierRegles.estServiceCreation("Création"));
        assertFalse(DossierRegles.estServiceCreation("Entreprise"));
        assertFalse(DossierRegles.estServiceCreation(null));
        assertFalse(DossierRegles.estServiceCreation(""));
    }

    @Test
    void societeExigeAssociesSaufIndividuelle() {
        assertTrue(DossierRegles.estSociete("SARL"));
        assertTrue(DossierRegles.estSociete("sarlu"));
        assertTrue(DossierRegles.estSociete("SA"));
        assertTrue(DossierRegles.estSociete("AUTRE"));
        assertFalse(DossierRegles.estSociete("INDIVIDUELLE"));
        assertFalse(DossierRegles.estSociete("individuelle"));
        assertFalse(DossierRegles.estSociete(null));
    }
}
