package com.cabex.backend.service.service;

import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.service.dto.CreateServiceRequest;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.DomaineServiceRepository;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.service.service.ServiceGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private DomaineServiceRepository domaineRepository;
    @Mock
    private AuditService auditService;

    private ServiceGestionService serviceGestionService;

    @BeforeEach
    void setUp() {
        serviceGestionService = new ServiceGestionService(serviceRepository, domaineRepository, auditService);
    }

    @Test
    void premierCodeSrv0001() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of());

        assertEquals("SRV-0001", serviceGestionService.genererCode(null));
    }

    @Test
    void suiteApresLeMaxExistant() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-0001", "SRV-0007"));

        assertEquals("SRV-0008", serviceGestionService.genererCode(null));
    }

    @Test
    void collisionResolueParAvance() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-0003"));
        when(serviceRepository.existsByCodeService("SRV-0004")).thenReturn(true);
        when(serviceRepository.existsByCodeService("SRV-0005")).thenReturn(false);

        assertEquals("SRV-0005", serviceGestionService.genererCode(null));
    }

    @Test
    void premierCodeAvecDomaine() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of());

        assertEquals("SRV-COMP-0001", serviceGestionService.genererCode("COMP"));
    }

    @Test
    void codeAvecDomaineSequenceParDomaine() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-COMP-0007", "SRV-COMP-0012", "SRV-FISC-0003"));

        assertEquals("SRV-COMP-0013", serviceGestionService.genererCode("COMP"));
    }

    @Test
    void codeAvecDomaineCollisionResolue() {
        when(serviceRepository.findAllCodes()).thenReturn(List.of("SRV-COMP-0001"));
        when(serviceRepository.existsByCodeService("SRV-COMP-0002")).thenReturn(true);
        when(serviceRepository.existsByCodeService("SRV-COMP-0003")).thenReturn(false);

        assertEquals("SRV-COMP-0003", serviceGestionService.genererCode("COMP"));
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
                new CreateServiceRequest("Audit comptable", null, null, null));

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

    @Test
    void getAllFiltreTypeNormalise() {
        Service service = Service.builder().id(1).nomService("Transaction immobilière")
                .codeService("SRV-0025").typeService("TRANSACTION_IMMOBILIERE").actif(true).build();
        when(serviceRepository.rechercher(null, null, "TRANSACTION_IMMOBILIERE", null,
                PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(service), PageRequest.of(0, 8), 1));

        var page = serviceGestionService.getAll(null, null, " transaction_immobiliere ", null,
                PageRequest.of(0, 8));

        assertEquals(1, page.getTotalElements());
        assertEquals("TRANSACTION_IMMOBILIERE", page.getContent().getFirst().typeService());
    }

    @Test
    void getAllSansTypeNeFiltrePas() {
        when(serviceRepository.rechercher(null, null, null, null, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        var page = serviceGestionService.getAll(null, null, "  ", null, PageRequest.of(0, 8));

        assertTrue(page.isEmpty());
    }

    @Test
    void getAllTypeInvalide400() {
        var ex = assertThrows(BusinessException.class,
                () -> serviceGestionService.getAll(null, null, "INCONNU", null, PageRequest.of(0, 8)));

        assertEquals(ErrorMessages.formatInvalide("type de service"), ex.getMessage());
    }

    @Test
    void createTypeCreationNormalise() {
        when(serviceRepository.existsByNomService("Création d'entreprise")).thenReturn(false);
        when(serviceRepository.findAllCodes()).thenReturn(List.of());
        when(serviceRepository.existsByCodeService("SRV-0001")).thenReturn(false);
        when(serviceRepository.save(any())).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            s.setId(2);
            return s;
        });

        var reponse = serviceGestionService.create(
                new CreateServiceRequest("Création d'entreprise", "creation", null, null));

        assertEquals("CREATION", reponse.typeService());
    }

    @Test
    void createTypeInvalide400() {
        var ex = assertThrows(BusinessException.class,
                () -> serviceGestionService.create(
                        new CreateServiceRequest("X", "INCONNU", null, null)));
        assertEquals(ErrorMessages.formatInvalide("type de service"), ex.getMessage());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void createSansTypeDefautStandard() {
        when(serviceRepository.existsByNomService("Audit comptable")).thenReturn(false);
        when(serviceRepository.findAllCodes()).thenReturn(List.of());
        when(serviceRepository.existsByCodeService("SRV-0001")).thenReturn(false);
        when(serviceRepository.save(any())).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            s.setId(3);
            return s;
        });

        var reponse = serviceGestionService.create(
                new CreateServiceRequest("Audit comptable", null, null, null));

        assertEquals("STANDARD", reponse.typeService());
    }
}
