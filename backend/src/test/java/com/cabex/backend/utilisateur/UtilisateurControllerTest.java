package com.cabex.backend.utilisateur;

import com.cabex.backend.utilisateur.dto.UtilisateurResponse;
import com.cabex.backend.utilisateur.service.UtilisateurService;
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
 * Routes, statuts HTTP et sécurité de l'API utilisateurs (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurService utilisateurService;

    private UtilisateurResponse utilisateur() {
        return new UtilisateurResponse(1, "Martin", "Amine", "amine.martin",
                "amine.martin@cabinet-excellent.mg", true, false, null, null, null, List.of());
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"USER_VIEW"})
    void getOk200() throws Exception {
        when(utilisateurService.getAll(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(utilisateur()), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/api/utilisateurs").param("search", "amine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("amine.martin"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = {"USER_CREATE"})
    void postOk201() throws Exception {
        when(utilisateurService.create(any())).thenReturn(utilisateur());

        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Martin","prenom":"Amine","username":"amine.martin",
                                 "email":"amine.martin@cabinet-excellent.mg","password":"secret123",
                                 "roleId":1,"actif":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("amine.martin"));
    }

    @Test
    @WithMockUser(authorities = {"USER_CREATE"})
    void postMotDePasseCourt400() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Martin","prenom":"Amine","username":"amine.martin",
                                 "email":"amine.martin@cabinet-excellent.mg","password":"abc",
                                 "roleId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"USER_UPDATE"})
    void patchActivationOk200() throws Exception {
        when(utilisateurService.toggleActivation(eq(1), eq(false))).thenReturn(utilisateur());

        mockMvc.perform(patch("/api/utilisateurs/1/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actif":false}
                                """))
                .andExpect(status().isOk());
    }
}
