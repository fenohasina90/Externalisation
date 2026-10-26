package com.cabex.backend.service;

import com.cabex.backend.service.dto.DomaineResponse;
import com.cabex.backend.service.service.DomaineGestionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routes, statuts HTTP et sécurité de l'API domaines (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DomaineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DomaineGestionService domaineService;

    private DomaineResponse domaine() {
        return new DomaineResponse(1, "Comptabilité", null, true, 14L);
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/domaines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/domaines"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"DOMAINE_VIEW"})
    void getOk200() throws Exception {
        when(domaineService.getAll(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(domaine()), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/api/domaines").param("recherche", "compta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nomDomaine").value("Comptabilité"))
                .andExpect(jsonPath("$.content[0].nombreSocietes").value(14))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = {"DOMAINE_CREATE"})
    void postOk201() throws Exception {
        when(domaineService.create(any())).thenReturn(domaine());

        mockMvc.perform(post("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomDomaine":"Comptabilité"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomDomaine").value("Comptabilité"));
    }

    @Test
    @WithMockUser(authorities = {"DOMAINE_CREATE"})
    void postNomManquant400() throws Exception {
        mockMvc.perform(post("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomDomaine":"  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"DOMAINE_UPDATE"})
    void patchActivationOk200() throws Exception {
        when(domaineService.toggleActivation(eq(1), eq(false)))
                .thenReturn(new DomaineResponse(1, "Comptabilité", null, false, 14L));

        mockMvc.perform(patch("/api/domaines/1/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actif":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
    }
}
