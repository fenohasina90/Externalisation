package com.cabex.backend.dossier;

import com.cabex.backend.dossier.dto.DossierTransactionImmobiliereResponse;
import com.cabex.backend.dossier.service.DossierTransactionImmobiliereService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DossierTransactionImmobiliereControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DossierTransactionImmobiliereService dossierService;

    private DossierTransactionImmobiliereResponse reponse() {
        return new DossierTransactionImmobiliereResponse(
                1, 126, "REF-2026-126", LocalDate.of(2026, 10, 1),
                "Jean Dupont", "C001", "Rakoto Immobilier",
                "+261 34 00 000 00", "contact@rakoto.mg", "Rakoto Immobilier",
                "Lot II A 12 Antananarivo", "Rakoto Pierre",
                true, false, false, false, false, false, null,
                "TERRAIN", null,
                new BigDecimal("500.00"), "Plan A", "Axe nord",
                new BigDecimal("1000000.00"), false,
                null, null, null, null, null, null, null,
                "COMMUNAL_DOMAINE", "TITRE_BORNE",
                true, false, true, false, true,
                "Observation importante", 2, new BigDecimal("50000.00"), true);
    }

    private String corpsCreation() {
        return """
                {"actionAchat":true,"objet":"TERRAIN",
                 "achatMesureM2":500,"achatMontant":1000000}""";
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/demandes/126/dossier-transaction-immobiliere"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/demandes/126/dossier-transaction-immobiliere"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void getOk200() throws Exception {
        when(dossierService.getByDemandeId(126)).thenReturn(reponse());

        mockMvc.perform(get("/api/demandes/126/dossier-transaction-immobiliere"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceDemande").value("REF-2026-126"))
                .andExpect(jsonPath("$.nomCommercial").value("Jean Dupont"))
                .andExpect(jsonPath("$.numeroIm").value("C001"))
                .andExpect(jsonPath("$.nomSociete").value("Rakoto Immobilier"))
                .andExpect(jsonPath("$.objet").value("TERRAIN"))
                .andExpect(jsonPath("$.actionAchat").value(true));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void getDossierManquant404() throws Exception {
        when(dossierService.getByDemandeId(126)).thenThrow(
                new ResourceNotFoundException(ErrorMessages.introuvable(
                        "Dossier de transaction immobilière", "demande 126")));

        mockMvc.perform(get("/api/demandes/126/dossier-transaction-immobiliere"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ErrorMessages.introuvable(
                        "Dossier de transaction immobilière", "demande 126")));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_CREATE"})
    void postOk201() throws Exception {
        when(dossierService.create(eq(126), any())).thenReturn(reponse());

        mockMvc.perform(post("/api/demandes/126/dossier-transaction-immobiliere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.objet").value("TERRAIN"));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_CREATE"})
    void postMontantNegatif400() throws Exception {
        mockMvc.perform(post("/api/demandes/126/dossier-transaction-immobiliere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actionAchat":true,"objet":"TERRAIN","achatMontant":-1}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void postSansPermissionCreation403() throws Exception {
        mockMvc.perform(post("/api/demandes/126/dossier-transaction-immobiliere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_UPDATE"})
    void putOk200() throws Exception {
        when(dossierService.update(eq(126), any())).thenReturn(reponse());

        mockMvc.perform(put("/api/demandes/126/dossier-transaction-immobiliere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = {"DOSSIER_VIEW"})
    void putSansPermissionModification403() throws Exception {
        mockMvc.perform(put("/api/demandes/126/dossier-transaction-immobiliere")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsCreation()))
                .andExpect(status().isForbidden());
    }
}
