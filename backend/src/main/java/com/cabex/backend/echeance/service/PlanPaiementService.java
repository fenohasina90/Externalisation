package com.cabex.backend.echeance.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.HistoriqueLigneService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.configuration.service.ConfigurationKeys;
import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.echeance.dto.CreatePlanPaiementRequest;
import com.cabex.backend.echeance.dto.PlanPaiementResponse;
import com.cabex.backend.echeance.dto.UpdatePlanPaiementRequest;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.mapper.EcheanceMapper;
import com.cabex.backend.echeance.repository.EcheancePaiementRepository;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@org.springframework.stereotype.Service
public class PlanPaiementService {

    private final ProposPaiementRepository proposRepository;
    private final EcheancePaiementRepository echeanceRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueRepository;
    private final LigneServiceRepository ligneRepository;
    private final HistoriqueLigneServiceRepository historiqueLigneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final ConfigurationService configurationService;

    public PlanPaiementService(ProposPaiementRepository proposRepository,
                               EcheancePaiementRepository echeanceRepository,
                               DemandeServiceRepository demandeRepository,
                               HistoriqueDemandeRepository historiqueRepository,
                               LigneServiceRepository ligneRepository,
                               HistoriqueLigneServiceRepository historiqueLigneRepository,
                               UtilisateurRepository utilisateurRepository,
                               AuditService auditService,
                               ConfigurationService configurationService) {
        this.proposRepository = proposRepository;
        this.echeanceRepository = echeanceRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueRepository = historiqueRepository;
        this.ligneRepository = ligneRepository;
        this.historiqueLigneRepository = historiqueLigneRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
        this.configurationService = configurationService;
    }

    private int joursAlerte() {
        return Math.max(configurationService.lireEntier(
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE,
                ConfigurationKeys.JOURS_AVANCE_ALERTE_ECHEANCE_DEFAUT), 1);
    }

    @Transactional(readOnly = true)
    public PlanPaiementResponse getByDemande(Integer demandeId) {
        DemandeService demande = getDemande(demandeId);
        ProposPaiement propos = proposRepository
                .findFirstByDemandeIdAndStatutOrderByCreatedAtDesc(demandeId, "ACTIF")
                .orElseGet(() -> proposRepository.findByDemandeIdOrderByCreatedAtDesc(demandeId)
                        .stream().findFirst().orElse(null));
        if (propos == null) {
            throw new ResourceNotFoundException(
                    "Aucun plan de paiement pour la demande " + demande.getReferenceDemande());
        }
        return toResponse(propos);
    }

    @Transactional
    public PlanPaiementResponse create(Integer demandeId, CreatePlanPaiementRequest request) {
        List<LigneService> lignes = ligneRepository.findByDemandeId(demandeId);
        if (lignes.size() != 1) {
            throw new BusinessException(
                    "Demande multi-services : créez le plan depuis la ligne de service concernée");
        }
        return createForLigne(lignes.get(0).getId(), request);
    }

    @Transactional(readOnly = true)
    public PlanPaiementResponse getByLigne(Integer ligneId) {
        LigneService ligne = getLigne(ligneId);
        ProposPaiement propos = proposRepository.findByLigneServiceId(ligne.getId()).stream()
                .filter(p -> "ACTIF".equals(p.getStatut()))
                .findFirst()
                .orElseGet(() -> proposRepository.findByLigneServiceId(ligne.getId()).stream()
                        .findFirst().orElse(null));
        if (propos == null) {
            throw new ResourceNotFoundException(
                    "Aucun plan de paiement pour la ligne de service " + ligne.getId());
        }
        return toResponse(propos);
    }

    /**
     * Établit le plan de paiement d'une ligne : la ligne passe EN_ATTENTE
     * (en attente du premier paiement).
     */
    @Transactional
    public PlanPaiementResponse createForLigne(Integer ligneId, CreatePlanPaiementRequest request) {
        LigneService ligne = getLigne(ligneId);
        DemandeService demande = ligne.getDemande();

        if (ligne.getFacture() == null
                || !"VALIDEE".equals(ligne.getFacture().getStatut())) {
            throw new BusinessException(
                    "La facture du service doit être validée avant d'établir son plan de paiement");
        }

        if (proposRepository.existsByLigneServiceId(ligneId)) {
            throw new BusinessException("Un plan de paiement existe déjà pour ce service");
        }

        String statutActuel = statutActuel(demande.getId());
        if ("TERMINEE".equals(statutActuel) || "ANNULEE".equals(statutActuel)) {
            throw new BusinessException(
                    "Le plan de paiement ne peut pas être créé pour une demande terminée ou annulée");
        }

        if (request.dateFin() != null && request.dateDebut() != null
                && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }

        if (request.dateDebut() != null && ligne.getDebutService() != null
                && request.dateDebut().isBefore(ligne.getDebutService())) {
            throw new BusinessException(
                    "Le plan de paiement doit commencer après le début du service ("
                            + ligne.getDebutService() + ")");
        }
        if (request.dateFin() != null && ligne.getFinService() != null
                && request.dateFin().isAfter(ligne.getFinService())) {
            throw new BusinessException(
                    "Le plan de paiement doit se terminer avant la fin du service ("
                            + ligne.getFinService() + ")");
        }

        ProposPaiement propos = ProposPaiement.builder()
                .demande(demande)
                .ligneService(ligne)
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .montantTotal(request.montantTotal())
                .restePayer(request.montantTotal())
                .nombreEcheances(request.nombreEcheances())
                .commentaire(request.commentaire())
                .statut("ACTIF")
                .createdBy(currentUtilisateur())
                .build();
        propos = proposRepository.save(propos);

        propos.setEcheances(construireEcheances(propos, ligne, request));
        propos = proposRepository.save(propos);

        marquerPlanEtabli(ligne);

        auditService.log(AuditActions.CREATE, "PAIEMENT", "PlanPaiement",
                Long.valueOf(propos.getId()), null,
                demande.getReferenceDemande() + " : " + ligne.getId());
        return toResponse(propos);
    }

    @Transactional
    public PlanPaiementResponse update(Integer proposId, UpdatePlanPaiementRequest request) {
        ProposPaiement propos = proposRepository.findById(proposId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Plan de paiement", proposId)));

        if (request.dateFin() != null && request.dateDebut() != null
                && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }

        DemandeService demandeCourante = propos.getDemande();
        LigneService ligneCourante = propos.getLigneService();
        LocalDate debutService = ligneCourante != null ? ligneCourante.getDebutService() : null;
        LocalDate finService = ligneCourante != null ? ligneCourante.getFinService() : null;
        if (request.dateDebut() != null && debutService != null
                && request.dateDebut().isBefore(debutService)) {
            throw new BusinessException(
                    "Le plan de paiement doit commencer après le début du service ("
                            + debutService + ")");
        }
        if (request.dateFin() != null && finService != null
                && request.dateFin().isAfter(finService)) {
            throw new BusinessException(
                    "Le plan de paiement doit se terminer avant la fin du service ("
                            + finService + ")");
        }

        propos.setDateDebut(request.dateDebut());
        propos.setDateFin(request.dateFin());
        propos.setMontantTotal(request.montantTotal());
        propos.setCommentaire(request.commentaire());

        BigDecimal totalPaye = echeanceRepository.findByProposPaiementIdOrderByNumeroEcheanceAsc(proposId).stream()
                .map(EcheancePaiement::getMontantPaye)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        propos.setRestePayer(request.montantTotal().subtract(totalPaye).max(BigDecimal.ZERO));

        ProposPaiement saved = proposRepository.save(propos);
        auditService.log(AuditActions.UPDATE, "PAIEMENT", "PlanPaiement",
                Long.valueOf(saved.getId()), saved.getDemande().getReferenceDemande(),
                saved.getDemande().getReferenceDemande());
        return toResponse(saved);
    }

    /**
     * Échéances manuelles (date + montant saisies) ou répartition automatique.
     * En manuel : la taille doit égaler nombreEcheances et la somme montantTotal.
     */
    private Set<EcheancePaiement> construireEcheances(ProposPaiement propos, LigneService ligne,
                                                      CreatePlanPaiementRequest request) {
        if (request.echeances() == null || request.echeances().isEmpty()) {
            return genererEcheances(propos);
        }
        if (request.echeances().size() != propos.getNombreEcheances()) {
            throw new BusinessException(
                    "Le nombre d'échéances saisies doit égaler le nombre d'échéances du plan");
        }
        BigDecimal somme = request.echeances().stream()
                .map(com.cabex.backend.echeance.dto.EcheancePlanRequest::montantAttendu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (somme.compareTo(propos.getMontantTotal()) != 0) {
            throw new BusinessException(
                    "La somme des échéances (" + somme + ") doit égaler le montant total ("
                            + propos.getMontantTotal() + ")");
        }
        Set<EcheancePaiement> echeances = new HashSet<>();
        int numero = 1;
        for (com.cabex.backend.echeance.dto.EcheancePlanRequest saisie : request.echeances()) {
            validerDateEcheance(ligne, saisie.dateEcheance());
            echeances.add(EcheancePaiement.builder()
                    .proposPaiement(propos)
                    .numeroEcheance(numero++)
                    .dateEcheance(saisie.dateEcheance())
                    .montantAttendu(saisie.montantAttendu())
                    .montantPaye(BigDecimal.ZERO)
                    .build());
        }
        return echeances;
    }

    private void validerDateEcheance(LigneService ligne, LocalDate date) {
        if (date != null && ligne.getDebutService() != null && ligne.getFinService() != null
                && (date.isBefore(ligne.getDebutService()) || date.isAfter(ligne.getFinService()))) {
            throw new BusinessException(
                    "L'échéance doit être comprise entre le début et la fin du service ("
                            + ligne.getDebutService() + " → " + ligne.getFinService() + ")");
        }
    }

    private Set<EcheancePaiement> genererEcheances(ProposPaiement propos) {
        int n = propos.getNombreEcheances();
        LocalDate debut = propos.getDateDebut();
        LocalDate fin = propos.getDateFin();
        BigDecimal total = propos.getMontantTotal();

        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        Set<EcheancePaiement> echeances = new HashSet<>();
        long totalJours = Math.max(0, ChronoUnit.DAYS.between(debut, fin));

        for (int i = 0; i < n; i++) {
            BigDecimal montant = (i == n - 1)
                    ? total.subtract(base.multiply(BigDecimal.valueOf(n - 1)))
                    : base;

            LocalDate date = fin;
            if (n > 1) {
                date = debut.plusDays(totalJours * i / (n - 1));
            }

            echeances.add(EcheancePaiement.builder()
                    .proposPaiement(propos)
                    .numeroEcheance(i + 1)
                    .dateEcheance(date)
                    .montantAttendu(montant)
                    .montantPaye(BigDecimal.ZERO)
                    .build());
        }
        return echeances;
    }

    private String statutActuel(Integer demandeId) {
        return historiqueRepository.findFirstByDemandeIdOrderByIdDesc(demandeId)
                .map(HistoriqueDemande::getStatut)
                .map(s -> s.getStatut())
                .orElse(null);
    }

    private PlanPaiementResponse toResponse(ProposPaiement propos) {
        return EcheanceMapper.toPlanResponse(propos, LocalDate.now(), joursAlerte());
    }

    private DemandeService getDemande(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", id)));
    }

    private LigneService getLigne(Integer id) {
        return ligneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Ligne de service", id)));
    }

    /** La ligne passe EN_ATTENTE à l'établissement de son plan (jamais de régression). */
    private void marquerPlanEtabli(LigneService ligne) {
        String statut = ligne.getStatut();
        if (LigneServiceService.LIGNE_EN_COURS.equals(statut)
                || LigneServiceService.LIGNE_TERMINE.equals(statut)
                || LigneServiceService.LIGNE_EN_ATTENTE.equals(statut)) {
            return;
        }
        ligne.setStatut(LigneServiceService.LIGNE_EN_ATTENTE);
        ligneRepository.save(ligne);
        historiqueLigneRepository.save(HistoriqueLigneService.builder()
                .ligneService(ligne)
                .statut(LigneServiceService.LIGNE_EN_ATTENTE)
                .commentaire("Plan de paiement établi (en attente du premier paiement)")
                .utilisateur(currentUtilisateur())
                .origine("MANUEL")
                .build());
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
