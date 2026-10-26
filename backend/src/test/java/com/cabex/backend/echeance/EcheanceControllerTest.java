package com.cabex.backend.echeance;

import com.cabex.backend.echeance.dto.EcheanceCibleResponse;
import com.cabex.backend.echeance.service.EcheanceGestionService;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route de résolution d'une échéance vers sa demande (navigation depuis les notifications).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EcheanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EcheanceGestionService echeanceService;

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/echeances/42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"PAIEMENT_CREATE"})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/echeances/42"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DEMANDE_VIEW"})
    void getCibleOk200() throws Exception {
        when(echeanceService.getCible(42))
                .thenReturn(new EcheanceCibleResponse(42, 2, 7, "DEM-2026-00017"));

        mockMvc.perform(get("/api/echeances/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.numeroEcheance").value(2))
                .andExpect(jsonPath("$.demandeId").value(7))
                .andExpect(jsonPath("$.referenceDemande").value("DEM-2026-00017"));
    }

    @Test
    @WithMockUser(authorities = {"DEMANDE_VIEW"})
    void getCibleIntrouvable404() throws Exception {
        when(echeanceService.getCible(999))
                .thenThrow(new ResourceNotFoundException("Échéance introuvable"));

        mockMvc.perform(get("/api/echeances/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Échéance introuvable"));
    }
}
