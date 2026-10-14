package com.cabex.backend.service.service;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.service.service.ServiceGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Code auto-généré unique (SRV-0001, …) et lecture par code.
 */
@ExtendWith(MockitoExtension.class)
class ServiceGestionServiceTest {

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private AuditService auditService;

    private ServiceGestionService serviceGestionService;

    @BeforeEach
    void setUp() {
        serviceGestionService = new ServiceGestionService(serviceRepository, auditService);
    }

    @Test
    void premierCodeSrv0001() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of());

        assertEquals("SRV-0001", serviceGestionService.genererCode());
    }

    @Test
    void suiteApresLeMaxExistant() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-0001", "SRV-0007"));

        assertEquals("SRV-0008", serviceGestionService.genererCode());
    }

    @Test
    void collisionResolueParAvance() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-0003"));
        when(serviceRepository.existsByCodeService("SRV-0004")).thenReturn(true);
        when(serviceRepository.existsByCodeService("SRV-0005")).thenReturn(false);

        assertEquals("SRV-0005", serviceGestionService.genererCode());
    }

    @Test
    void createAttribueCodeGenere() {
        when(serviceRepository.existsByNomService("Audit comptable")).thenReturn(false);
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-0001"));
        when(serviceRepository.existsByCodeService("SRV-0002")).thenReturn(false);
        when(serviceRepository.save(any())).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            s.setId(1);
            return s;
        });

        var reponse = serviceGestionService.create(
                new CreateServiceRequest("Audit comptable", null));

        assertEquals("SRV-0002", reponse.codeService());
        verify(auditService).log(eq("CREATE"), eq("SERVICE"), eq("Service"),
                eq(1L), isNull(), contains("Audit comptable"));
    }

    @Test
    void getByCodeOkEtInconnu404() {
        Service service = Service.builder().id(1).nomService("Audit comptable")
                .codeService("SRV-0001").actif(true).build();
        when(serviceRepository.findByCodeService("SRV-0001")).thenReturn(Optional.of(service));

        var reponse = serviceGestionService.getByCode("srv-0001");

        assertEquals("Audit comptable", reponse.nomService());
        assertEquals("SRV-0001", reponse.codeService());

        when(serviceRepository.findByCodeService("SRV-9999")).thenReturn(Optional.empty());
        var ex = assertThrows(ResourceNotFoundException.class,
                () -> serviceGestionService.getByCode("SRV-9999"));
        assertEquals(ErrorMessages.introuvable("Service", "SRV-9999"), ex.getMessage());
    }
}
