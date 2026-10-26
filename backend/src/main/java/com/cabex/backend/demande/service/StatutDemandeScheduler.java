package com.cabex.backend.demande.service;

import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Clôture opérationnelle automatique (quotidienne) :
 * - une ligne passe TERMINE dès que sa date de fin est atteinte
 *   (solde éventuel conservé) ;
 * - une demande passe TERMINEE quand toutes ses lignes sont TERMINEES.
 * Transitions idempotentes : réexécution sans effet.
 */
@Component
public class StatutDemandeScheduler {

    private final LigneServiceRepository ligneRepository;
    private final LigneServiceService ligneService;
    private final DemandeGestionService demandeService;

    public StatutDemandeScheduler(LigneServiceRepository ligneRepository,
                                  LigneServiceService ligneService,
                                  DemandeGestionService demandeService) {
        this.ligneRepository = ligneRepository;
        this.ligneService = ligneService;
        this.demandeService = demandeService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cloturerServicesEtDemandes() {
        LocalDate aujourdHui = LocalDate.now();
        List<LigneService> aCloturer = ligneRepository.findByStatutInAndFinServiceBefore(
                List.of(LigneServiceService.LIGNE_VALIDE,
                        LigneServiceService.LIGNE_EN_ATTENTE,
                        LigneServiceService.LIGNE_EN_COURS),
                aujourdHui);
        Set<Integer> demandesIds = new HashSet<>();
        for (LigneService ligne : aCloturer) {
            boolean appliquee = ligneService.avancerStatut(ligne,
                    LigneServiceService.LIGNE_TERMINE,
                    "Date de fin de réalisation atteinte (" + ligne.getFinService()
                            + ") — clôture automatique",
                    "AUTO");
            if (appliquee && ligne.getDemande() != null) {
                demandesIds.add(ligne.getDemande().getId());
            }
        }
        for (Integer demandeId : demandesIds) {
            demandeService.cloturerSiTerminee(demandeId);
        }
    }
}
