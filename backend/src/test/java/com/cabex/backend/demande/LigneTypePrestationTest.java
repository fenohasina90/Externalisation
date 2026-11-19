package com.cabex.backend.demande;

import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.service.entity.Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sous-type STANDARD : PRESTATION (défaut) / ABONNEMENT_MENSUEL.
 */
class LigneTypePrestationTest {

    private Service service(String type) {
        return Service.builder().typeService(type).build();
    }

    @Test
    void standardDefautPrestation() {
        assertEquals("PRESTATION",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION", service("STANDARD"), null));
    }

    @Test
    void standardAbonnementOk() {
        assertEquals("ABONNEMENT_MENSUEL",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION", service("STANDARD"), "ABONNEMENT_MENSUEL"));
    }

    @Test
    void standardValeurInconnue400() {
        var ex = assertThrows(BusinessException.class, () ->
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION", service("STANDARD"), "INCONNU"));
        assertEquals(ErrorMessages.TYPE_PRESTATION_INVALIDE, ex.getMessage());
    }

    @Test
    void creationCommeStandardDefautPrestation() {
        assertEquals("PRESTATION",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION", service("CREATION"), null));
    }

    @Test
    void creationCommeStandardAbonnementOk() {
        assertEquals("ABONNEMENT_MENSUEL",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION", service("CREATION"), "Abonnement_MENSUEL"));
    }

    @Test
    void transactionCommeStandardDefautPrestation() {
        assertEquals("PRESTATION",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION",
                        service("TRANSACTION_IMMOBILIERE"), null));
    }

    @Test
    void transactionCommeStandardAbonnementOk() {
        assertEquals("ABONNEMENT_MENSUEL",
                LigneServiceService.normaliserTypePrestation("EXTERNALISATION",
                        service("TRANSACTION_IMMOBILIERE"), "ABONNEMENT_MENSUEL"));
    }

    @Test
    void deboursInterditType() {
        var ex = assertThrows(BusinessException.class, () ->
                LigneServiceService.normaliserTypePrestation("DEBOURS", null, "PRESTATION"));
        assertEquals(ErrorMessages.TYPE_PRESTATION_INTERDIT, ex.getMessage());
    }
}
