package com.cabex.backend.reinitialisation;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.reinitialisation.dto.ReinitialisationRequest;
import com.cabex.backend.reinitialisation.dto.ReinitialisationResponse;
import com.cabex.backend.reinitialisation.service.ReinitialisationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReinitialisationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuditService auditService;

    private ReinitialisationService service() {
        return new ReinitialisationService(jdbcTemplate, auditService);
    }

    @Test
    void reinitialisationDemandeNeReferenceAucuneTableSupprimee() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        ReinitialisationResponse reponse = service().reinitialiser(
                new ReinitialisationRequest(List.of("DEMANDE"), ReinitialisationService.CONFIRMATION));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.atLeastOnce()).update(sql.capture());
        List<String> requetes = sql.getAllValues();

        // Les tables echeance_paiement / propos_paiement ont été supprimées par
        // migration_2026_11_19 : aucune requête ne doit encore les cibler.
        assertFalse(requetes.stream().anyMatch(s -> s.contains("echeance_paiement")),
                "La réinitialisation ne doit plus supprimer echeance_paiement");
        assertFalse(requetes.stream().anyMatch(s -> s.contains("propos_paiement")),
                "La réinitialisation ne doit plus supprimer propos_paiement");

        // Les tables réellement présentes doivent bien être nettoyées.
        assertTrue(requetes.contains("DELETE FROM demande_service"));
        assertTrue(requetes.contains("DELETE FROM paiement"));
        assertTrue(requetes.contains("DELETE FROM facture_validee"));
        assertTrue(reponse.compteurs().containsKey("demande_service"));
    }

    @Test
    void confirmationInvalideRefuseeSansSuppression() {
        ReinitialisationRequest requete = new ReinitialisationRequest(List.of("DEMANDE"), "OUI");

        assertThrows(BusinessException.class, () -> service().reinitialiser(requete));
        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void selectionVideRefusee() {
        ReinitialisationRequest requete = new ReinitialisationRequest(List.of(), ReinitialisationService.CONFIRMATION);

        assertThrows(BusinessException.class, () -> service().reinitialiser(requete));
        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void cibleInconnueRefusee() {
        ReinitialisationRequest requete = new ReinitialisationRequest(
                List.of("INCONNUE"), ReinitialisationService.CONFIRMATION);

        assertThrows(BusinessException.class, () -> service().reinitialiser(requete));
        verify(jdbcTemplate, never()).update(anyString());
    }
}
