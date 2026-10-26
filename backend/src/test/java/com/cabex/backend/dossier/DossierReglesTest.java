package com.cabex.backend.dossier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DossierReglesTest {

    @Test
    void serviceCreationDetecteParType() {
        assertTrue(DossierRegles.estTypeCreation("CREATION"));
        assertTrue(DossierRegles.estTypeCreation("creation"));
        assertTrue(DossierRegles.estTypeCreation("  Creation  "));
    }

    @Test
    void serviceNonCreationRejete() {
        assertFalse(DossierRegles.estTypeCreation("STANDARD"));
        assertFalse(DossierRegles.estTypeCreation("Audit comptable"));
        assertFalse(DossierRegles.estTypeCreation(null));
        assertFalse(DossierRegles.estTypeCreation(""));
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
