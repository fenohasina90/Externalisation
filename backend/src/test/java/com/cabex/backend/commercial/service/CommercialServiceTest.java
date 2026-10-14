package com.cabex.backend.commercial.service;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.commercial.dto.CreateCommercialRequest;
import com.cabex.backend.commercial.entity.Commercial;
import com.cabex.backend.commercial.repository.CommercialRepository;
import com.cabex.backend.commercial.service.CommercialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Immatricule générée automatiquement et unique (C001, C002, …).
 */
@ExtendWith(MockitoExtension.class)
class CommercialServiceTest {

    @Mock
    private CommercialRepository commercialRepository;
    @Mock
    private AuditService auditService;

    private CommercialService commercialService;

    @BeforeEach
    void setUp() {
        commercialService = new CommercialService(commercialRepository, auditService);
    }

    private CreateCommercialRequest requete() {
        return new CreateCommercialRequest("Jean Dupont", "jean@cabex.mg", "+261 34 00 000 00");
    }

    @Test
    void premiereImmatriculeC001() {
        when(commercialRepository.findAllImmatricules()).thenReturn(List.of());

        assertEquals("C001", commercialService.genererImmatricule());
    }

    @Test
    void suiteApresLeMaxExistant() {
        when(commercialRepository.findAllImmatricules())
                .thenReturn(List.of("C001", "C002", "C007"));

        assertEquals("C008", commercialService.genererImmatricule());
    }

    @Test
    void ignoreLesFormatsNonConformes() {
        java.util.List<String> existantes = new java.util.ArrayList<>();
        existantes.add(null);
        existantes.add("  ");
        existantes.add("X12");
        existantes.add("C003");
        when(commercialRepository.findAllImmatricules()).thenReturn(existantes);

        assertEquals("C004", commercialService.genererImmatricule());
    }

    @Test
    void collisionResolueParAvance() {
        // C004 déjà prise entre le calcul et l'enregistrement (concurrence).
        when(commercialRepository.findAllImmatricules()).thenReturn(List.of("C001", "C003"));
        when(commercialRepository.existsByImmatricule("C004")).thenReturn(true);
        when(commercialRepository.existsByImmatricule("C005")).thenReturn(false);

        assertEquals("C005", commercialService.genererImmatricule());
    }

    @Test
    void createAttribueImmatriculeGeneree() {
        when(commercialRepository.findAllImmatricules()).thenReturn(List.of("C001"));
        when(commercialRepository.existsByImmatricule("C002")).thenReturn(false);
        when(commercialRepository.save(any())).thenAnswer(inv -> {
            Commercial c = inv.getArgument(0);
            c.setId(9);
            return c;
        });

        var reponse = commercialService.create(requete());

        ArgumentCaptor<Commercial> captor = ArgumentCaptor.forClass(Commercial.class);
        verify(commercialRepository).save(captor.capture());
        assertEquals("C002", captor.getValue().getImmatricule());
        assertEquals("Jean Dupont", captor.getValue().getNomCommercial());
        assertTrue(captor.getValue().getActif());
        assertEquals("C002", reponse.immatricule());
    }
}
