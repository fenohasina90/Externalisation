package com.cabex.backend.notification;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.notification.repository.NotificationRepository;
import com.cabex.backend.notification.service.NotificationScheduler;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le scheduler contre les vraies requêtes et le vrai service de
 * notification : chaque échéance est détachée après la transaction du
 * repository, donc l'accès aux associations LAZY
 * ({@code proposPaiement.demande.societe}) doit être couvert par la
 * transaction du scheduler, sinon {@code LazyInitializationException}.
 */
@SpringBootTest
class NotificationSchedulerExecutionTest {

    @Autowired private DemandeServiceRepository demandeRepository;
    @Autowired private LigneServiceRepository ligneRepository;
    @Autowired private ProposPaiementRepository proposRepository;
    @Autowired private EcheancePaiementRepository echeanceRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationScheduler scheduler;
    @Autowired private PlatformTransactionManager txManager;

    private long creerEcheance(String suffixe, LocalDate dateEcheance, String attendu, String paye) {
        var id = new long[1];
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            DemandeService demande = demandeRepository.save(DemandeService.builder()
                    .referenceDemande("DEM-INT-" + suffixe)
                    .dateDemande(LocalDate.now())
                    .actif(true)
                    .build());
            Service service = serviceRepository.save(Service.builder()
                    .nomService("Service " + suffixe)
                    .codeService("SRV-INT-" + suffixe)
                    .typeService("STANDARD")
                    .actif(true)
                    .build());
            LigneService ligne = ligneRepository.save(LigneService.builder()
                    .demande(demande)
                    .service(service)
                    .tarif(new BigDecimal(attendu))
                    .debutService(LocalDate.now().minusDays(30))
                    .finService(LocalDate.now().plusDays(60))
                    .build());
            ProposPaiement plan = proposRepository.save(ProposPaiement.builder()
                    .demande(demande)
                    .ligneService(ligne)
                    .dateDebut(LocalDate.now().minusDays(30))
                    .dateFin(LocalDate.now().plusDays(60))
                    .montantTotal(new BigDecimal(attendu))
                    .restePayer(new BigDecimal(attendu))
                    .nombreEcheances(1)
                    .statut("ACTIF")
                    .build());
            EcheancePaiement echeance = echeanceRepository.save(EcheancePaiement.builder()
                    .proposPaiement(plan)
                    .numeroEcheance(1)
                    .dateEcheance(dateEcheance)
                    .montantAttendu(new BigDecimal(attendu))
                    .montantPaye(new BigDecimal(paye))
                    .build());
            id[0] = echeance.getId();
        });
        return id[0];
    }

    private boolean notificationExiste(String type, long referenceId) {
        return notificationRepository.findAll().stream().anyMatch(n ->
                type.equals(n.getTypeNotification())
                        && n.getReferenceId() != null
                        && n.getReferenceId() == referenceId);
    }

    @Test
    void leSchedulerReelCreeLesAlertesSansErreurDeLazyLoading() {
        long enRetard = creerEcheance("RETARD", LocalDate.now().minusDays(2), "100000", "0");
        long proche = creerEcheance("PROCHE", LocalDate.now().plusDays(1), "100000", "0");
        long lointaine = creerEcheance("LOIN", LocalDate.now().plusDays(10), "100000", "0");

        assertDoesNotThrow(() -> scheduler.verifierEcheances());

        assertTrue(notificationExiste("PAIEMENT_RETARD", enRetard));
        assertTrue(notificationExiste("ECHEANCE_PROCHE", proche));
        assertFalse(notificationExiste("ECHEANCE_PROCHE", lointaine));
        assertFalse(notificationExiste("PAIEMENT_RETARD", lointaine));
    }
}
