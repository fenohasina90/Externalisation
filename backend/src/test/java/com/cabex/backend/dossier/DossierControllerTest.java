package com.cabex.backend.dossier;

import com.cabex.backend.dossier.dto.AssocieResponse;
import com.cabex.backend.dossier.dto.DossierResponse;
import com.cabex.backend.dossier.service.DossierService;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routes, statuts HTTP et sécurité de l'API dossier (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DossierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DossierService dossierService;

    private DossierResponse reponse() {
        return new DossierResponse(
                1, 126, "REF-2026-126", LocalDate.of(2026, 9, 1), "C001",
                "SARL", null, "Sahan Agro SARL", null, new BigDecimal("5000000"), 2,
                "Nirina Rasoanaivo", "Gérante", false, "Agroalimentaire", null,
                null, null, null, null, null, "LOCATION_CABINET", null, true, "Bail 3/6/9",
                "Négoce", LocalDate.of(2026, 11, 1), "PERMANENTE", null, null, null,
                "Non", null, null,
                List.of(new AssocieResponse(1, 0, "Nirina Rasoanaivo", "60 %", null, null)));
    }

    private String corpsCreation() {
        return """
                {"formeJuridique":"SARL","denomination":"Sahan Agro SARL",
                 "associes":[{"nom":"Nirina Rasoanaivo","partSociale":"60 %"}]}""";
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/demandes/126/dossier"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/demandes/126/dossier"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void getOk200() throws Exception {
        when(dossierService.getByDemandeId(126)).thenReturn(reponse());

        mockMvc.perform(get("/api/demandes/126/dossier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceDemande").value("REF-2026-126"))
                .andExpect(jsonPath("$.numeroIm").value("C001"))
                .andExpect(jsonPath("$.associes[0].nom").value("Nirina Rasoanaivo"));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void getDossierManquant404() throws Exception {
        when(dossierService.getByDemandeId(126)).thenThrow(
                new ResourceNotFoundException(ErrorMessages.introuvable("Dossier de création", "demande 126")));

        mockMvc.perform(get("/api/demandes/126/dossier"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorMessages.introuvable("Dossier de création", "demande 126")));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_CREATE"})
    void postOk201() throws Exception {
        when(dossierService.create(eq(126), any())).thenReturn(reponse());

        mockMvc.perform(post("/api/demandes/126/dossier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.denomination").value("Sahan Agro SARL"));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_CREATE"})
    void postDenominationManquante400() throws Exception {
        mockMvc.perform(post("/api/demandes/126/dossier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formeJuridique":"SARL","denomination":"  ",
                                 "associes":[{"nom":"Nirina Rasoanaivo"}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void postSansPermissionCreation403() throws Exception {
        mockMvc.perform(post("/api/demandes/126/dossier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_UPDATE"})
    void putOk200() throws Exception {
        when(dossierService.update(eq(126), any())).thenReturn(reponse());

        mockMvc.perform(put("/api/demandes/126/dossier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
