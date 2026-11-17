package com.cabex.backend.facture;

import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.controller.FactureController;
import com.cabex.backend.facture.dto.FactureValideeResponse;
import com.cabex.backend.facture.service.FactureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route de résolution d'une facture validée vers sa demande (navigation depuis les notifications).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FactureValideeConsultationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FactureService factureService;

    private FactureValideeResponse reponse(int id) {
        return new FactureValideeResponse(
                id,
                "FV-2026-00042",
                null,
                null,
                null,
                7,
                "DEM-2026-00017",
                null,
                null,
                null,
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                "GENERALE",
                "Administration",
                LocalDateTime.of(2026, 11, 17, 10, 0),
                List.of(),
                null,
                "UNIQUE",
                List.of());
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/factures-validees/42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"DEMANDE_VIEW"})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/factures-validees/42"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"FACTURE_VIEW"})
    void getOk200() throws Exception {
        when(factureService.getFactureValideeById(42)).thenReturn(reponse(42));

        mockMvc.perform(get("/api/factures-validees/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.referenceFactureValidee").value("FV-2026-00042"))
                .andExpect(jsonPath("$.demandeId").value(7))
                .andExpect(jsonPath("$.referenceDemande").value("DEM-2026-00017"))
                .andExpect(jsonPath("$.type").value("GENERALE"));
    }

    @Test
    @WithMockUser(authorities = {"FACTURE_VIEW"})
    void getIntrouvable404() throws Exception {
        when(factureService.getFactureValideeById(anyInt()))
                .thenThrow(new ResourceNotFoundException("Facture validée introuvable"));

        mockMvc.perform(get("/api/factures-validees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Facture validée introuvable"));
    }
}
