package com.cabex.backend.paiement;

import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.dto.PaiementDetailResponse;
import com.cabex.backend.paiement.service.PaiementGestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Détail d'un paiement par sa référence (action « Voir détail » et notifications).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaiementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaiementGestionService paiementService;

    private static PaiementDetailResponse detail() {
        return new PaiementDetailResponse(
                20, "PAY-2026-00020", 18, "DEM-2026-00018", 5, "Beta SA",
                7, 1, LocalDate.of(2026, 10, 30), "Déclarations fiscales et sociales",
                1, "Espèces", LocalDate.of(2026, 10, 22),
                new BigDecimal("1300000.00"), null, null, null,
                1, "Système Administrateur",
                new BigDecimal("2000000.00"), new BigDecimal("1300000.00"),
                new BigDecimal("700000.00"), "un million trois cent mille");
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/paiements/reference/PAY-2026-00020"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"PAIEMENT_CREATE"})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/paiements/reference/PAY-2026-00020"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"PAIEMENT_VIEW"})
    void getByReferenceOk200() throws Exception {
        when(paiementService.getByReference("PAY-2026-00020")).thenReturn(detail());

        mockMvc.perform(get("/api/paiements/reference/PAY-2026-00020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referencePaiement").value("PAY-2026-00020"))
                .andExpect(jsonPath("$.referenceDemande").value("DEM-2026-00018"))
                .andExpect(jsonPath("$.nomSociete").value("Beta SA"))
                .andExpect(jsonPath("$.nomService").value("Déclarations fiscales et sociales"))
                .andExpect(jsonPath("$.numeroEcheance").value(1))
                .andExpect(jsonPath("$.dateEcheance").value("2026-10-30"))
                .andExpect(jsonPath("$.montantAttendu").value(2000000.0))
                .andExpect(jsonPath("$.montantPayeEcheance").value(1300000.0))
                .andExpect(jsonPath("$.resteAPayer").value(700000.0))
                .andExpect(jsonPath("$.montantEnLettres").value("un million trois cent mille"))
                .andExpect(jsonPath("$.createdByName").value("Système Administrateur"));
    }

    @Test
    @WithMockUser(authorities = {"PAIEMENT_VIEW"})
    void getByReferenceIntrouvable404() throws Exception {
        when(paiementService.getByReference("PAY-2026-99999"))
                .thenThrow(new ResourceNotFoundException("Paiement introuvable"));

        mockMvc.perform(get("/api/paiements/reference/PAY-2026-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paiement introuvable"));
    }
}
