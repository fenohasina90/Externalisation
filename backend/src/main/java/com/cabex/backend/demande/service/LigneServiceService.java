package com.cabex.backend.demande.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.HistoriqueLigneResponse;
import com.cabex.backend.demande.dto.LigneServiceResponse;
import com.cabex.backend.demande.dto.UpdateLigneRequest;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.entity.HistoriqueDemande;
import com.cabex.backend.demande.entity.HistoriqueLigneService;
import com.cabex.backend.demande.entity.LigneService;
import com.cabex.backend.demande.entity.StatutDemande;
import com.cabex.backend.demande.mapper.DemandeMapper;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.demande.repository.HistoriqueDemandeRepository;
import com.cabex.backend.demande.repository.HistoriqueLigneServiceRepository;
import com.cabex.backend.demande.repository.LigneServiceRepository;
import com.cabex.backend.demande.repository.StatutDemandeRepository;
import com.cabex.backend.echeance.entity.EcheancePaiement;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.service.FactureService;
import com.cabex.backend.service.entity.Service;
import com.cabex.backend.service.repository.ServiceRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion des lignes de service d'une demande (multi-services).
 * Ajout, modification et retrait uniquement quand la demande est EN_ATTENTE.
 * Centralise aussi les transitions de statut des lignes (jamais de régression).
 */
@org.springframework.stereotype.Service
public class LigneServiceService {

    public static final String LIGNE_CREE = "CREE";
    public static final String LIGNE_VALIDE = "VALIDE";
    public static final String LIGNE_EN_ATTENTE = "EN_ATTENTE";
    public static final String LIGNE_EN_COURS = "EN_COURS";
    public static final String LIGNE_TERMINE = "TERMINE";

    private static final String DEMANDE_EN_ATTENTE = "EN_ATTENTE";

    private static final Map<String, Integer> ORDRE_LIGNE = Map.of(
            LIGNE_CREE, 0,
            LIGNE_VALIDE, 1,
            LIGNE_EN_ATTENTE, 2,
            LIGNE_EN_COURS, 3,
            LIGNE_TERMINE, 4);

    private final LigneServiceRepository ligneRepository;
    private final HistoriqueLigneServiceRepository historiqueLigneRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueDemandeRepository;
    private final StatutDemandeRepository statutDemandeRepository;
    private final ServiceRepository serviceRepository;
    private final ProposPaiementRepository proposRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final FactureService factureService;

    public LigneServiceService(LigneServiceRepository ligneRepository,
                               HistoriqueLigneServiceRepository historiqueLigneRepository,
                               DemandeServiceRepository demandeRepository,
                               HistoriqueDemandeRepository historiqueDemandeRepository,
                               StatutDemandeRepository statutDemandeRepository,
                               ServiceRepository serviceRepository,
                               ProposPaiementRepository proposRepository,
                               UtilisateurRepository utilisateurRepository,
                               AuditService auditService,
                               FactureService factureService) {
        this.ligneRepository = ligneRepository;
        this.historiqueLigneRepository = historiqueLigneRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueDemandeRepository = historiqueDemandeRepository;
        this.statutDemandeRepository = statutDemandeRepository;
        this.serviceRepository = serviceRepository;
        this.proposRepository = proposRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
        this.factureService = factureService;
    }

    @Transactional(readOnly = true)
    public List<LigneServiceResponse> listerParDemande(Integer demandeId) {
        verifierDemande(demandeId);
        List<LigneService> lignes = ligneRepository.findByDemandeIdInWithService(List.of(demandeId));
        Map<Integer, BigDecimal> paiements = lignes.isEmpty()
                ? Map.of()
                : ligneRepository.sommesPayeesParLignes(
                        lignes.stream().map(LigneService::getId).toList()).stream()
                        .collect(Collectors.toMap(
                                o -> ((Number) o[0]).intValue(),
                                o -> o[1] == null ? BigDecimal.ZERO : new BigDecimal(o[1].toString())));
        return lignes.stream()
                .map(l -> toResponse(l, paiements.getOrDefault(l.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoriqueLigneResponse> historiqueParLigne(Integer ligneId) {
        getLigne(ligneId);
        List<HistoriqueLigneService> historique =
                historiqueLigneRepository.findByLigneServiceIdOrderByIdAsc(ligneId);
        List<HistoriqueLigneResponse> resultat = new ArrayList<>();
        String ancienStatut = null;
        for (HistoriqueLigneService h : historique) {
            resultat.add(DemandeMapper.toHistoriqueLigneResponse(h, ancienStatut));
            ancienStatut = h.getStatut();
        }
        return resultat;
    }

    @Transactional
    public LigneServiceResponse ajouter(Integer demandeId, CreateLigneRequest request) {
        DemandeService demande = verifierDemande(demandeId);
        exigerEnAttente(demande);
        Service service = verifierService(request.serviceId());
        verifierCoherenceTypeSolo(demande, service);
        verifierDoublonService(demandeId, request.serviceId(), null);
        verifierDates(request.debutService(), request.finService());
        if (request.tarif().signum() < 0) {
            throw new BusinessException(ErrorMessages.TARIF_POSITIF);
        }

        LigneService ligne = LigneService.builder()
                .demande(demande)
                .service(service)
                .tarif(request.tarif())
                .debutService(request.debutService())
                .finService(request.finService())
                .build();
        ligne = ligneRepository.save(ligne);

        ajouterHistoriqueLigne(ligne, LIGNE_CREE,
                "Service ajouté : " + service.getNomService() + " (tarif=" + request.tarif() + ")",
                "MANUEL");
        ajouterHistoriqueDemande(demande,
                "Service ajouté : " + service.getNomService() + " (tarif=" + request.tarif() + ")");

        factureService.actualiserPourDemande(demande.getId());

        auditService.log(AuditActions.CREATE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), null,
                "demande=" + demande.getReferenceDemande() + ", service=" + service.getNomService());

        return toResponse(ligne);
    }

    @Transactional
    public LigneServiceResponse modifier(Integer ligneId, UpdateLigneRequest request) {
        LigneService ligne = getLigne(ligneId);
        exigerEnAttente(ligne.getDemande());
        exigerFactureNonValidee(ligne);

        StringBuilder details = new StringBuilder();
        if (request.tarif() != null) {
            if (request.tarif().signum() < 0) {
                throw new BusinessException(ErrorMessages.TARIF_POSITIF);
            }
            details.append("tarif=").append(ligne.getTarif()).append("→").append(request.tarif());
            ligne.setTarif(request.tarif());
        }
        if (request.debutService() != null) {
            ligne.setDebutService(request.debutService());
        }
        if (request.finService() != null) {
            ligne.setFinService(request.finService());
        }
        verifierDates(ligne.getDebutService(), ligne.getFinService());
        if (details.isEmpty()) {
            details.append("période=").append(ligne.getDebutService())
                    .append("→").append(ligne.getFinService());
        }
        ligne = ligneRepository.save(ligne);

        ajouterHistoriqueLigne(ligne, statutOuCree(ligne),
                "Service modifié : " + details, "MANUEL");
        ajouterHistoriqueDemande(ligne.getDemande(), "Service modifié : " + details);

        factureService.actualiserPourDemande(ligne.getDemande().getId());

        auditService.log(AuditActions.UPDATE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), null, details.toString());

        return toResponse(ligne);
    }

    @Transactional
    public void retirer(Integer ligneId) {
        LigneService ligne = getLigne(ligneId);
        DemandeService demande = ligne.getDemande();
        exigerEnAttente(demande);
        exigerFactureNonValidee(ligne);
        if (ligneRepository.countByDemandeId(demande.getId()) <= 1) {
            throw new BusinessException("Une demande contient au moins un service : retrait impossible");
        }
        String nomService = ligne.getService() != null ? ligne.getService().getNomService() : "?";

        factureService.detacherAvantRetraitLigne(ligne);
        ligneRepository.delete(ligne);

        ajouterHistoriqueDemande(demande, "Service retiré : " + nomService);
        factureService.actualiserPourDemande(demande.getId());

        auditService.log(AuditActions.DELETE, "DEMANDE", "LigneService",
                Long.valueOf(ligneId), "demande=" + demande.getReferenceDemande(),
                "service retiré : " + nomService);
    }

    /**
     * Historique de création d'une ligne (utilisé à la création de la demande).
     */
    @Transactional
    public void ajouterHistoriqueCreation(LigneService ligne, String commentaire) {
        ajouterHistoriqueLigne(ligne, LIGNE_CREE, commentaire, "MANUEL");
    }

    /**
     * Transition centralisée : n'avance que vers un état strictement supérieur
     * (aucune régression). Retourne true si la transition a été appliquée.
     */
    @Transactional
    public boolean avancerStatut(LigneService ligne, String nouveauStatut, String commentaire,
                                 String origine) {
        int actuel = ORDRE_LIGNE.getOrDefault(
                ligne.getStatut() != null ? ligne.getStatut() : LIGNE_CREE, 0);
        int cible = ORDRE_LIGNE.getOrDefault(nouveauStatut, -1);
        if (cible < 0 || cible <= actuel) {
            return false;
        }
        ligne.setStatut(nouveauStatut);
        ligneRepository.save(ligne);
        ajouterHistoriqueLigne(ligne, nouveauStatut, commentaire, origine);
        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()),
                ligne.getStatut() != null ? ligne.getStatut() : null, nouveauStatut);
        return true;
    }

    @Transactional(readOnly = true)
    public BigDecimal montantPayeParLigne(Integer ligneId) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProposPaiement plan : proposRepository.findByLigneServiceId(ligneId)) {
            if (plan.getEcheances() == null) {
                continue;
            }
            total = total.add(plan.getEcheances().stream()
                    .map(e -> e.getMontantPaye() != null ? e.getMontantPaye() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return total;
    }

    @Transactional(readOnly = true)
    public BigDecimal montantTotalDemande(Integer demandeId) {
        return ligneRepository.findByDemandeId(demandeId).stream()
                .map(l -> l.getTarif() != null ? l.getTarif() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LigneServiceResponse toResponse(LigneService ligne) {
        return toResponse(ligne, montantPayeParLigne(ligne.getId()));
    }

    private LigneServiceResponse toResponse(LigneService ligne, BigDecimal paye) {
        BigDecimal tarif = ligne.getTarif() != null ? ligne.getTarif() : BigDecimal.ZERO;
        return DemandeMapper.toLigneResponse(ligne, paye, tarif.subtract(paye).max(BigDecimal.ZERO));
    }

    private void verifierDoublonService(Integer demandeId, Integer serviceId, Integer ligneExclue) {
        boolean doublon = ligneRepository.findByDemandeId(demandeId).stream()
                .anyMatch(l -> l.getService() != null
                        && l.getService().getId().equals(serviceId)
                        && (ligneExclue == null || !ligneExclue.equals(l.getId())));
        if (doublon) {
            throw new BusinessException("Ce service est déjà présent dans la demande");
        }
    }

    private void verifierCoherenceTypeSolo(DemandeService demande, Service service) {
        boolean nouveauCreation =
                com.cabex.backend.dossier.DossierRegles.estTypeCreation(service.getTypeService());
        boolean nouveauSolo = nouveauCreation
                || com.cabex.backend.dossier.DossierRegles
                .estTypeTransactionImmobiliere(service.getTypeService());
        List<LigneService> existantes = ligneRepository.findByDemandeId(demande.getId());
        boolean existantSolo = existantes.stream()
                .anyMatch(l -> l.getService() != null
                        && (com.cabex.backend.dossier.DossierRegles
                        .estTypeCreation(l.getService().getTypeService())
                        || com.cabex.backend.dossier.DossierRegles
                        .estTypeTransactionImmobiliere(l.getService().getTypeService())));
        if ((nouveauSolo || existantSolo) && !existantes.isEmpty()) {
            throw new BusinessException(ErrorMessages.SERVICE_SOLO_CREATION_TRANSACTION);
        }
        if (demande.getSociete() == null && !nouveauCreation) {
            throw new BusinessException(ErrorMessages.DEMANDE_SOCIETE_REQUISE);
        }
    }

    private void verifierDates(java.time.LocalDate debut, java.time.LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new BusinessException(ErrorMessages.DATE_FIN_AVANT_DEBUT);
        }
    }

    private Service verifierService(Integer serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Service", serviceId)));
        if (!Boolean.TRUE.equals(service.getActif())) {
            throw new BusinessException(ErrorMessages.SERVICE_INDISPONIBLE);
        }
        return service;
    }

    private DemandeService verifierDemande(Integer demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
    }

    private LigneService getLigne(Integer ligneId) {
        return ligneRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Ligne de service", ligneId)));
    }

    private void exigerEnAttente(DemandeService demande) {
        String statut = statutActuelDemande(demande.getId());
        if (!DEMANDE_EN_ATTENTE.equals(statut)) {
            throw new BusinessException(
                    "Les services ne peuvent être modifiés que quand la demande est EN_ATTENTE");
        }
    }

    private void exigerFactureNonValidee(LigneService ligne) {
        if (ligne.getFacture() != null
                && !"PROFORMA".equals(ligne.getFacture().getStatut())) {
            throw new BusinessException(
                    "La facture de ce service est validée : le service est figé");
        }
    }

    private String statutActuelDemande(Integer demandeId) {
        return historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(demandeId)
                .map(h -> h.getStatut() != null ? h.getStatut().getStatut() : null)
                .orElse(null);
    }

    private String statutOuCree(LigneService ligne) {
        return ligne.getStatut() != null ? ligne.getStatut() : LIGNE_CREE;
    }

    private void ajouterHistoriqueLigne(LigneService ligne, String statut, String commentaire,
                                        String origine) {
        historiqueLigneRepository.save(HistoriqueLigneService.builder()
                .ligneService(ligne)
                .statut(statut)
                .commentaire(commentaire)
                .utilisateur(currentUtilisateur())
                .origine(origine)
                .build());
    }

    private void ajouterHistoriqueDemande(DemandeService demande, String commentaire) {
        String statut = statutActuelDemande(demande.getId());
        StatutDemande statutEntite = statutDemandeRepository.findByStatut(
                        statut != null ? statut : DEMANDE_EN_ATTENTE)
                .orElseThrow(() -> new BusinessException(ErrorMessages.STATUT_INVALIDE));
        historiqueDemandeRepository.save(HistoriqueDemande.builder()
                .demande(demande)
                .statut(statutEntite)
                .commentaire(commentaire)
                .utilisateur(currentUtilisateur())
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
