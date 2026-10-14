package com.cabex.backend.service;

import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.ServiceResponse;
import com.cabex.backend.service.service.ServiceGestionService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Routes, statuts HTTP et sécurité de l'API services (service mocké).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceGestionService serviceGestionService;

    private ServiceResponse service() {
        return new ServiceResponse(1, "Audit comptable", "SRV-0001", "STANDARD",
                "Vérification des comptes", true);
    }

    @Test
    void sansAuthentification401() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {})
    void sansPermission403() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"SERVICE_VIEW"})
    void getByCodeOk200() throws Exception {
        when(serviceGestionService.getByCode("SRV-0001")).thenReturn(service());

        mockMvc.perform(get("/api/services/code/SRV-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomService").value("Audit comptable"))
                .andExpect(jsonPath("$.codeService").value("SRV-0001"));
    }

    @Test
    @WithMockUser(authorities = {"SERVICE_VIEW"})
    void getByCodeInconnu404() throws Exception {
        when(serviceGestionService.getByCode("SRV-9999")).thenThrow(
                new ResourceNotFoundException(ErrorMessages.introuvable("Service", "SRV-9999")));

        mockMvc.perform(get("/api/services/code/SRV-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"SERVICE_VIEW"})
    void rechercheParNomOuCode200() throws Exception {
        when(serviceGestionService.getAll(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(service()), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/api/services").param("recherche", "SRV-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codeService").value("SRV-0001"));
    }

    @Test
    @WithMockUser(authorities = {"SERVICE_CREATE"})
    void postOk201CodeGenere() throws Exception {
        when(serviceGestionService.create(any())).thenReturn(service());

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomService":"Audit comptable"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codeService").value("SRV-0001"));
    }
}
