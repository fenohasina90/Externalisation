package com.cabex.backend.service;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.DuplicateResourceException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateDomaineRequest;
import com.cabex.backend.service.dto.UpdateDomaineRequest;
import com.cabex.backend.service.entity.DomaineService;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import com.cabex.backend.service.service.DomaineGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Règles métier des domaines : nom unique, trim, activation.
 */
@ExtendWith(MockitoExtension.class)
class DomaineGestionServiceTest {

    @Mock
    private DomaineServiceRepository domaineRepository;
    @Mock
    private AuditService auditService;

    private DomaineGestionService domaineService;

    @BeforeEach
    void setUp() {
        domaineService = new DomaineGestionService(domaineRepository, auditService);
    }

    @Test
    void createTrimEtActifParDefaut() {
        when(domaineRepository.existsByNomDomaine("Comptabilité")).thenReturn(false);
        when(domaineRepository.existsByCodeDomaine("COMPTA")).thenReturn(false);
        when(domaineRepository.save(any())).thenAnswer(inv -> {
            DomaineService d = inv.getArgument(0);
            d.setId(1);
            return d;
        });

        var reponse = domaineService.create(new CreateDomaineRequest("  Comptabilité  ", "COMPTA", null));

        assertEquals("Comptabilité", reponse.nomDomaine());
        assertEquals("COMPTA", reponse.codeDomaine());
        assertTrue(reponse.actif());
        verify(auditService).log(eq("CREATE"), eq("SERVICE"), eq("Domaine"),
                eq(1L), isNull(), contains("Comptabilité"));
    }

    @Test
    void createNomDuplique409() {
        when(domaineRepository.existsByNomDomaine("Comptabilité")).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> domaineService.create(new CreateDomaineRequest("Comptabilité", "COMPTA", null)));
        assertEquals(ErrorMessages.referenceDejaUtilisee("Comptabilité"), ex.getMessage());
        verify(domaineRepository, never()).save(any());
    }

    @Test
    void createCodeDuplique409() {
        when(domaineRepository.existsByNomDomaine("Comptabilité")).thenReturn(false);
        when(domaineRepository.existsByCodeDomaine("COMPTA")).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> domaineService.create(new CreateDomaineRequest("Comptabilité", "COMPTA", null)));
        assertEquals(ErrorMessages.referenceDejaUtilisee("COMPTA"), ex.getMessage());
        verify(domaineRepository, never()).save(any());
    }

    @Test
    void toggleActivationEtGetById() {
        DomaineService domaine = DomaineService.builder()
                .id(2).nomDomaine("Finance").codeDomaine("FINAN").actif(true).build();
        when(domaineRepository.findById(2)).thenReturn(Optional.of(domaine));
        when(domaineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = domaineService.toggleActivation(2, false);

        assertFalse(reponse.actif());
        assertEquals("FINAN", reponse.codeDomaine());
    }

    @Test
    void getByIdInexistant404() {
        when(domaineRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> domaineService.getById(999));
    }

    @Test
    void updateModifieNomEtCode() {
        DomaineService domaine = DomaineService.builder()
                .id(3).nomDomaine("Social").codeDomaine("SOCIAL").actif(true).build();
        when(domaineRepository.findById(3)).thenReturn(Optional.of(domaine));
        when(domaineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var reponse = domaineService.update(3, new UpdateDomaineRequest("Social et RH", "SOC-RH", "desc"));

        assertEquals("Social et RH", reponse.nomDomaine());
        assertEquals("SOC-RH", reponse.codeDomaine());
    }

    @Test
    void updateCodeDuplique409() {
        DomaineService domaine = DomaineService.builder()
                .id(3).nomDomaine("Social").codeDomaine("SOCIAL").actif(true).build();
        when(domaineRepository.findById(3)).thenReturn(Optional.of(domaine));
        when(domaineRepository.existsByCodeDomaineAndIdNot("AUTRE", 3)).thenReturn(true);

        var ex = assertThrows(DuplicateResourceException.class,
                () -> domaineService.update(3, new UpdateDomaineRequest("Social", "AUTRE", null)));
        assertEquals(ErrorMessages.referenceDejaUtilisee("AUTRE"), ex.getMessage());
        verify(domaineRepository, never()).save(any());
    }
}
