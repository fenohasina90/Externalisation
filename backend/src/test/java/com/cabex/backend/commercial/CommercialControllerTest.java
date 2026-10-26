package com.cabex.backend.commercial;

import com.cabex.backend.commercial.dto.CommercialResponse;
import com.cabex.backend.commercial.service.CommercialService;
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
 * Routes, statuts HTTP et sécurité de l'API commerciaux (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommercialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommercialService commercialService;

    private CommercialResponse commercial() {
        return new CommercialResponse(1, "Jean Dupont", "C001",
                "jean@cabex.mg", "+261 34 00 000 00", true);
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/commerciaux"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/commerciaux"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"COMMERCIAL_VIEW"})
    void getOk200() throws Exception {
        when(commercialService.getAll(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(commercial()), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/api/commerciaux").param("recherche", "jean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].immatricule").value("C001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = {"COMMERCIAL_CREATE"})
    void postOk201ImmatriculeGeneree() throws Exception {
        when(commercialService.create(any())).thenReturn(commercial());

        mockMvc.perform(post("/api/commerciaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomCommercial":"Jean Dupont","email":"jean@cabex.mg",
                                 "contact":"+261 34 00 000 00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.immatricule").value("C001"));
    }

    @Test
    @WithMockUser(authorities = {"COMMERCIAL_CREATE"})
    void postNomManquant400() throws Exception {
        mockMvc.perform(post("/api/commerciaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomCommercial":"  ","email":"jean@cabex.mg"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"COMMERCIAL_UPDATE"})
    void patchActivationOk200() throws Exception {
        when(commercialService.toggleActivation(eq(1), eq(false)))
                .thenReturn(new CommercialResponse(1, "Jean Dupont", "C001",
                        "jean@cabex.mg", "+261 34 00 000 00", false));

        mockMvc.perform(patch("/api/commerciaux/1/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actif":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
    }
}
