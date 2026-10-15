package com.cabex.backend.audit;

import com.cabex.backend.audit.entity.AuditLog;
import com.cabex.backend.audit.repository.AuditLogRepository;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Les valeurs d'audit sont stockées en texte brut (pas d'enveloppe JSON).
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Test
    void valeursStockeesEnTexteBrut() {
        AuditService auditService = new AuditService(auditRepository, utilisateurRepository);
        when(auditRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.log("UPDATE", "UTILISATEUR", "Utilisateur", 3L, "actif=true", "actif=false");

        ArgumentCaptor<AuditLog> capteur = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(capteur.capture());
        assertEquals("actif=true", capteur.getValue().getAncienneValeur());
        assertEquals("actif=false", capteur.getValue().getNouvelleValeur());
    }
}
