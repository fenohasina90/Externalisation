package com.cabex.backend.client;

import com.cabex.backend.client.dto.SocieteResponse;
import com.cabex.backend.client.service.SocieteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routes, statuts HTTP et sécurité de l'API clients (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SocieteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocieteService societeService;

    private SocieteResponse societe() {
        return new SocieteResponse(1, "Atlas Conseil", "Services financiers",
                "+261 34 12 345 67", "contact@atlas-conseil.mg", null, null, null, null, null,
                true, false, "Amina Neme", 15, null, 1, "Comptabilité");
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"CLIENT_VIEW"})
    void getOk200() throws Exception {
        when(societeService.getAll(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(societe()), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nomSociete").value("Atlas Conseil"))
                .andExpect(jsonPath("$.content[0].archive").value(false))
                .andExpect(jsonPath("$.content[0].responsablePrincipal").value("Amina Neme"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = {"CLIENT_CREATE"})
    void postOk201() throws Exception {
        when(societeService.create(any())).thenReturn(societe());

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomSociete":"Atlas Conseil","domaineId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomSociete").value("Atlas Conseil"));
    }

    @Test
    @WithMockUser(authorities = {"CLIENT_CREATE"})
    void postNomManquant400() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomSociete":"  ","domaineId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"CLIENT_UPDATE"})
    void patchArchiveOk200() throws Exception {
        when(societeService.archive(eq(1), eq(true))).thenReturn(societe());

        mockMvc.perform(patch("/api/clients/1/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"archive":true}
                                """))
                .andExpect(status().isOk());
    }
}
