package com.cabex.backend.demande.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.demande.dto.CreateLigneRequest;
import com.cabex.backend.demande.dto.HistoriqueLigneResponse;
import com.cabex.backend.demande.dto.LigneServiceResponse;
import com.cabex.backend.demande.dto.UpdateLigneRequest;
import com.cabex.backend.demande.dto.ValiderMontantDeboursRequest;
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
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureValideeRepository;
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
    public static final String LIGNE_ANNULEE = "ANNULEE";

    /** Statuts DEBOURS (par ligne, VARCHAR libre). */
    public static final String LIGNE_NON_PAYEE = "NON_PAYEE";
    public static final String LIGNE_PARTIEL = "PARTIEL";
    public static final String LIGNE_PAYEE = "PAYEE";

    public static final String CATEGORIE_EXTERNALISATION = "EXTERNALISATION";
    public static final String CATEGORIE_DEBOURS = "DEBOURS";

    /** Sous-types pour EXTERNALISATION + service STANDARD. */
    public static final String PRESTATION = "PRESTATION";
    public static final String ABONNEMENT_MENSUEL = "ABONNEMENT_MENSUEL";

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
    private final FactureValideeRepository factureValideeRepository;
    private final com.cabex.backend.facture.repository.FactureValideeLigneRepository factureValideeLigneRepository;
    private final com.cabex.backend.paiement.repository.PaiementRepository paiementRepository;

    public LigneServiceService(LigneServiceRepository ligneRepository,
                               HistoriqueLigneServiceRepository historiqueLigneRepository,
                               DemandeServiceRepository demandeRepository,
                               HistoriqueDemandeRepository historiqueDemandeRepository,
                               StatutDemandeRepository statutDemandeRepository,
                               ServiceRepository serviceRepository,
                               ProposPaiementRepository proposRepository,
                               UtilisateurRepository utilisateurRepository,
                               AuditService auditService,
                               FactureService factureService,
                               com.cabex.backend.paiement.repository.PaiementRepository paiementRepository,
                               FactureValideeRepository factureValideeRepository,
                               com.cabex.backend.facture.repository.FactureValideeLigneRepository factureValideeLigneRepository) {
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
        this.paiementRepository = paiementRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.factureValideeLigneRepository = factureValideeLigneRepository;
    }

    @Transactional(readOnly = true)
    public List<LigneServiceResponse> listerParDemande(Integer demandeId) {
        verifierDemande(demandeId);
        List<LigneService> lignes = ligneRepository.findByDemandeIdInWithService(List.of(demandeId));
        return lignes.stream()
                .map(l -> toResponse(l, montantPayeParLigne(l.getId())))
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
        String categorie = normaliserCategorie(request.categorie());

        if (CATEGORIE_DEBOURS.equals(categorie)) {
            return ajouterDebours(demande, request);
        }
        if (request.designationDebours() != null && !request.designationDebours().isBlank()) {
            throw new BusinessException(
                    "La désignation du débours ne s'applique qu'aux lignes DEBOURS");
        }

        if (request.serviceId() == null || request.tarif() == null) {
            throw new BusinessException(ErrorMessages.DONNEES_OBLIGATOIRES);
        }
        Service service = verifierService(request.serviceId());
        verifierCoherenceTypeSolo(demande, service);
        verifierDoublonService(demandeId, request.serviceId(), null);
        verifierDates(request.debutService(), request.finService());
        if (request.tarif().signum() < 0) {
            throw new BusinessException(ErrorMessages.TARIF_POSITIF);
        }
        // Validation des dates selon le type de prestation :
        // - PRESTATION : début et fin optionnels (tous deux peuvent être null)
        // - ABONNEMENT_MENSUEL : début obligatoire, fin optionnel
        // - Autre : début et fin requis
        if (LigneServiceService.PRESTATION.equalsIgnoreCase(request.typePrestation())) {
            // Pas de contrainte de dates pour PRESTATION.
        } else if (LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(
                request.typePrestation())) {
            if (request.debutService() == null) {
                throw new BusinessException(ErrorMessages.DEBUT_SERVICE_OBLIGATOIRE_ABONNEMENT);
            }
            // La date fin est optionnelle pour un abonnement mensuel.
        } else {
            if (request.debutService() == null || request.finService() == null) {
                throw new BusinessException(ErrorMessages.DONNEES_OBLIGATOIRES);
            }
        }
        String typePrestation = normaliserTypePrestation(
                CATEGORIE_EXTERNALISATION, service, request.typePrestation());

        LigneService ligne = LigneService.builder()
                .demande(demande)
                .service(service)
                .tarif(request.tarif())
                .debutService(request.debutService())
                .finService(request.finService())
                .categorie(CATEGORIE_EXTERNALISATION)
                .typePrestation(typePrestation)
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
                demande.getReferenceDemande() + ", " + service.getNomService());

        return toResponse(ligne);
    }

    private LigneServiceResponse ajouterDebours(DemandeService demande,
                                                CreateLigneRequest request) {
        if (request.designationDebours() == null || request.designationDebours().isBlank()) {
            throw new BusinessException(ErrorMessages.DESIGNATION_DEBOURS_OBLIGATOIRE);
        }
        if (request.serviceId() != null || request.tarif() != null
                || request.debutService() != null || request.finService() != null) {
            throw new BusinessException(ErrorMessages.DEBOURS_SERVICE_INTERDIT);
        }
        // DEBOURS : type de prestation interdit (null obligatoire).
        normaliserTypePrestation(CATEGORIE_DEBOURS, null, request.typePrestation());
        if (request.montantDebours() != null && request.montantDebours().signum() < 0) {
            throw new BusinessException("Le montant du débours doit être positif ou nul");
        }

        BigDecimal montantInitial = request.montantDebours();
        LigneService ligne = LigneService.builder()
                .demande(demande)
                .categorie(CATEGORIE_DEBOURS)
                .designationDebours(request.designationDebours().trim())
                .statut(montantInitial != null ? LIGNE_NON_PAYEE : LIGNE_EN_ATTENTE)
                .montantDebours(montantInitial)
                .build();
        ligne = ligneRepository.save(ligne);

        if (montantInitial != null) {
            ajouterHistoriqueLigne(ligne, LIGNE_NON_PAYEE,
                    "Débours ajouté (montant annoncé : " + montantInitial + ") : "
                            + ligne.getDesignationDebours(), "MANUEL");
        } else {
            ajouterHistoriqueLigne(ligne, LIGNE_EN_ATTENTE,
                    "Débours ajouté (EN_ATTENTE) : " + ligne.getDesignationDebours(), "MANUEL");
        }
        ajouterHistoriqueDemande(demande,
                "Débours ajouté : " + ligne.getDesignationDebours());

        // Un débours avec montant figure dans la proforma : la régénérer/actualiser.
        factureService.actualiserPourDemande(demande.getId());

        auditService.log(AuditActions.CREATE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), null,
                demande.getReferenceDemande() + ", débours : " + ligne.getDesignationDebours());

        return toResponse(ligne);
    }

    /**
     * Bouton « Valider le montant » d'une ligne DEBOURS.
     * EN_ATTENTE → NON_PAYEE, montant figé sur la ligne.
     */
    @Transactional
    public LigneServiceResponse validerMontantDebours(
            Integer ligneId, ValiderMontantDeboursRequest request) {
        LigneService ligne = getLigne(ligneId);
        if (!estDebours(ligne)) {
            throw new BusinessException(ErrorMessages.DEBOURS_UNIQUEMENT);
        }
        if (!LIGNE_EN_ATTENTE.equals(ligne.getStatut())) {
            throw new BusinessException(
                    "Le montant ne peut être validé que quand la ligne est EN_ATTENTE");
        }
        if (ligne.getMontantDebours() != null) {
            throw new BusinessException(ErrorMessages.DEBOURS_MONTANT_DEJA_VALIDE);
        }
        BigDecimal montant = request != null ? request.montantDebours() : null;
        if (montant == null || montant.signum() <= 0) {
            throw new BusinessException(ErrorMessages.MONTANT_DEBOURS_POSITIF);
        }

        ligne.setMontantDebours(montant);
        ligne.setStatut(LIGNE_NON_PAYEE);
        ligneRepository.save(ligne);
        // Un paiement est toujours lié à une facture validée : la validation
        // d'un débours crée sa facture validée (sans proforma).
        factureService.creerFactureValideePourDebours(ligne, montant);

        ajouterHistoriqueLigne(ligne, LIGNE_NON_PAYEE,
                "Montant du débours validé : " + montant.toPlainString(), "MANUEL");
        ajouterHistoriqueDemande(ligne.getDemande(),
                "Montant du débours validé (ligne " + ligne.getId() + ") : "
                        + montant.toPlainString());

        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), LIGNE_EN_ATTENTE, LIGNE_NON_PAYEE);

        return toResponse(ligne);
    }

    /**
     * Annulation d'une ligne DEBOURS : EN_ATTENTE → ANNULEE uniquement.
     */
    @Transactional
    public LigneServiceResponse annulerDebours(Integer ligneId) {
        LigneService ligne = getLigne(ligneId);
        if (!estDebours(ligne)) {
            throw new BusinessException(ErrorMessages.DEBOURS_UNIQUEMENT);
        }
        if (!LIGNE_EN_ATTENTE.equals(ligne.getStatut())) {
            throw new BusinessException(ErrorMessages.TRANSITION_STATUT_INTERDITE);
        }

        ligne.setStatut(LIGNE_ANNULEE);
        ligneRepository.save(ligne);

        ajouterHistoriqueLigne(ligne, LIGNE_ANNULEE,
                "Débours annulé (EN_ATTENTE → ANNULEE)", "MANUEL");
        ajouterHistoriqueDemande(ligne.getDemande(),
                "Débours annulé (ligne " + ligne.getId() + ")");

        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), LIGNE_EN_ATTENTE, LIGNE_ANNULEE);

        return toResponse(ligne);
    }

    /**
     * Recalcule le statut automatique d'une ligne DEBOURS après un paiement :
     * NON_PAYEE → PARTIEL → PAYEE (selon le total payé vs montant validé).
     */
    @Transactional
    public void actualiserStatutDeboursApresPaiement(LigneService ligne) {
        if (ligne == null || !estDebours(ligne)) {
            return;
        }
        if (ligne.getMontantDebours() == null) {
            return;
        }
        BigDecimal paye = montantPayeParLigne(ligne.getId());
        if (paye == null) {
            paye = BigDecimal.ZERO;
        }
        String cible;
        if (paye.compareTo(BigDecimal.ZERO) == 0) {
            cible = LIGNE_NON_PAYEE;
        } else if (paye.compareTo(ligne.getMontantDebours()) >= 0) {
            cible = LIGNE_PAYEE;
        } else {
            cible = LIGNE_PARTIEL;
        }
        String actuel = ligne.getStatut();
        if (cible.equals(actuel)) {
            return;
        }
        ligne.setStatut(cible);
        ligneRepository.save(ligne);

        ajouterHistoriqueLigne(ligne, cible,
                "Statut du débours mis à jour (payé : " + paye.toPlainString()
                        + " / " + ligne.getMontantDebours().toPlainString() + ")", "AUTO");
        ajouterHistoriqueDemande(ligne.getDemande(),
                "Débours ligne " + ligne.getId() + " : " + cible + " (payé : "
                        + paye.toPlainString() + ")");

        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), actuel, cible);
    }

    /**
     * Recalcule le statut d'une ligne après un paiement :
     * NON_PAYEE → PARTIEL → PAYEE (selon le total payé vs montant attendu).
     * Pour EXTERNALISATION, le montant attendu est le tarif de la ligne.
     * Pour DEBOURS, c'est le montant débours validé.
     */
    @Transactional
    public void actualiserStatutApresPaiement(LigneService ligne) {
        if (ligne == null) {
            return;
        }
        BigDecimal paye = montantPayeParLigne(ligne.getId());
        BigDecimal attendu = montantAttendu(ligne);
        String cible;
        boolean estDebours = estDebours(ligne);
        if (ABONNEMENT_MENSUEL.equalsIgnoreCase(ligne.getTypePrestation())) {
            // Abonnement mensuel : les règlements du mois sont suivis sur la facture
            // et sur la facture du mois ; la ligne reste EN_COURS. La demande est
            // clôturée par changement de statut (TERMINEE).
            return;
        }
        if (paye.compareTo(BigDecimal.ZERO) == 0) {
            cible = estDebours ? LIGNE_NON_PAYEE : LIGNE_NON_PAYEE;
        } else if (paye.compareTo(attendu) >= 0) {
            // Pour EXTERNALISATION PRESTATION : passage à TERMINE quand totalement
            // payé. Pour DEBOURS : passage à PAYEE.
            cible = estDebours ? LIGNE_PAYEE : LIGNE_TERMINE;
        } else {
            cible = LIGNE_PARTIEL;
        }
        String actuel = ligne.getStatut();
        if (cible.equals(actuel)) {
            return;
        }
        ligne.setStatut(cible);
        ligneRepository.save(ligne);

        ajouterHistoriqueLigne(ligne, cible,
                "Statut de ligne mis à jour (payé : " + paye.toPlainString()
                        + " / " + attendu.toPlainString() + ")", "AUTO");
        ajouterHistoriqueDemande(ligne.getDemande(),
                "Ligne " + ligne.getId() + " : " + cible + " (payé : "
                        + paye.toPlainString() + ")");

        auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "LigneService",
                Long.valueOf(ligne.getId()), actuel, cible);
    }

    @Transactional
    public LigneServiceResponse modifier(Integer ligneId, UpdateLigneRequest request) {
        LigneService ligne = getLigne(ligneId);
        // DEBOURS : seule la désignation est modifiable, en EN_ATTENTE.
        if (estDebours(ligne)) {
            if (!LIGNE_EN_ATTENTE.equals(ligne.getStatut())) {
                throw new BusinessException(
                        "Le débours ne peut être modifié que quand il est EN_ATTENTE");
            }
            if (request.designationDebours() != null && !request.designationDebours().isBlank()) {
                ligne.setDesignationDebours(request.designationDebours().trim());
            }
            ligne = ligneRepository.save(ligne);
            ajouterHistoriqueLigne(ligne, ligne.getStatut(),
                    "Débours modifié : " + ligne.getDesignationDebours(), "MANUEL");
            ajouterHistoriqueDemande(ligne.getDemande(),
                    "Débours modifié (ligne " + ligne.getId() + ")");
            auditService.log(AuditActions.UPDATE, "DEMANDE", "LigneService",
                    Long.valueOf(ligne.getId()), null, ligne.getDesignationDebours());
            return toResponse(ligne);
        }
        exigerEnAttente(ligne.getDemande());
        exigerFactureNonValidee(ligne);

        StringBuilder details = new StringBuilder();
        if (request.typePrestation() != null && !request.typePrestation().isBlank()) {
            // Service déjà chargé via la ligne (proxy LAZY dans la transaction).
            Service svc = ligne.getService();
            String type = normaliserTypePrestation(
                    CATEGORIE_EXTERNALISATION, svc, request.typePrestation());
            details.append("type=").append(ligne.getTypePrestation()).append("→").append(type);
            ligne.setTypePrestation(type);
            // Pour ABONNEMENT_MENSUEL, la date début est obligatoire.
            if (ABONNEMENT_MENSUEL.equalsIgnoreCase(type)
                    && (ligne.getDebutService() == null)) {
                throw new BusinessException(ErrorMessages.DEBUT_SERVICE_OBLIGATOIRE_ABONNEMENT);
            }
        }
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
        // DEBOURS : retrait (annulation par suppression) uniquement en EN_ATTENTE.
        // Préférer annulerDebours (statut ANNULEE) dans le nouveau workflow.
        if (estDebours(ligne)) {
            if (!LIGNE_EN_ATTENTE.equals(ligne.getStatut())) {
                throw new BusinessException(
                        "Un débours ne peut être retiré que quand il est EN_ATTENTE (utilisez annuler sinon)");
            }
            if (ligneRepository.countByDemandeId(demande.getId()) <= 1) {
                throw new BusinessException("Une demande contient au moins une ligne : retrait impossible");
            }
            String designation = ligne.getDesignationDebours();
            ligneRepository.delete(ligne);
            ajouterHistoriqueDemande(demande, "Débours retiré : " + designation);
            auditService.log(AuditActions.DELETE, "DEMANDE", "LigneService",
                    Long.valueOf(ligneId), demande.getReferenceDemande(),
                    "débours retiré : " + designation);
            return;
        }
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
                Long.valueOf(ligneId), demande.getReferenceDemande(),
                "service retiré : " + nomService);
    }

    /**
     * Historique de création d'une ligne (utilisé à la création de la demande).
     */
    @Transactional
    public void ajouterHistoriqueCreation(LigneService ligne, String commentaire) {
        String statut = (ligne.getStatut() != null) ? ligne.getStatut() : LIGNE_CREE;
        ajouterHistoriqueLigne(ligne, statut, commentaire, "MANUEL");
    }

    /**
     * Transition centralisée : n'avance que vers un état strictement supérieur
     * (aucune régression). Retourne true si la transition a été appliquée.
     */
    @Transactional
    public boolean avancerStatut(LigneService ligne, String nouveauStatut, String commentaire,
                                 String origine) {
        if (estDebours(ligne)) {
            return false;
        }
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
        LigneService ligne = ligneRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Ligne de service", ligneId)));
        if (ligne.getFacture() != null
                && !"UNIQUE".equals(ligne.getFacture().getModeFacturation())) {
            com.cabex.backend.facture.entity.FactureValidee fv = factureValideeRepository
                    .findByFactureProformaId(ligne.getFacture().getId()).orElse(null);
            BigDecimal somme = BigDecimal.ZERO;
            if (fv != null) {
                somme = paiementRepository.sommePayeeParFactureValidee(fv.getId());
                if (somme == null) {
                    somme = BigDecimal.ZERO;
                }
            }
            // Factures du mois de l'abonnement liées à cette ligne.
            List<Integer> enfantsIds = factureValideeRepository.findByLigneServiceId(ligne.getId())
                    .stream()
                    .filter(f -> f.getLignes() == null || f.getLignes().isEmpty())
                    .map(com.cabex.backend.facture.entity.FactureValidee::getId)
                    .filter(id -> fv == null || !id.equals(fv.getId()))
                    .toList();
            if (!enfantsIds.isEmpty()) {
                BigDecimal sommeEnfants = paiementRepository.sommePayeeParFacturesValidees(enfantsIds);
                somme = somme.add(sommeEnfants != null ? sommeEnfants : BigDecimal.ZERO);
            }
            // Factures combinées du mois dont les lignes incluent cette ligne.
            BigDecimal sommeCombi = factureValideeLigneRepository
                    .sommePayeeViaFacturesCombinees(ligne.getId());
            somme = somme.add(sommeCombi != null ? sommeCombi : BigDecimal.ZERO);
            if (fv != null || !enfantsIds.isEmpty()) {
                return somme;
            }
        }
        // Ligne incluse dans des factures combinées (ligne sans facture directe).
        BigDecimal sommeCombi = factureValideeLigneRepository
                .sommePayeeViaFacturesCombinees(ligne.getId());
        if (sommeCombi != null && sommeCombi.signum() > 0) {
            return sommeCombi;
        }
        // Ligne liée directement à une facture validée (débours via la validation du montant).
        List<Integer> lieesIds = factureValideeRepository.findByLigneServiceId(ligne.getId())
                .stream()
                .filter(f -> f.getLignes() == null || f.getLignes().isEmpty())
                .map(com.cabex.backend.facture.entity.FactureValidee::getId)
                .toList();
        if (!lieesIds.isEmpty()) {
            BigDecimal sommeLiee = paiementRepository.sommePayeeParFacturesValidees(lieesIds);
            return sommeLiee != null ? sommeLiee : BigDecimal.ZERO;
        }
        // Ligne DEBOURS sans proforma : chercher la facture validée liée à la demande
        // sans proforma ni ligne (repli pour l'ancien modèle).
        if (ligne.getDemande() != null) {
            com.cabex.backend.facture.entity.FactureValidee fv = factureValideeRepository
                    .findFirstByDemandeIdAndFactureProformaIdIsNullAndLigneServiceIdIsNull(
                            ligne.getDemande().getId()).orElse(null);
            if (fv != null) {
                return paiementRepository.sommePayeeParFactureValidee(fv.getId());
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Montant attendu effectif d'une ligne : le montant validé de LA LIGNE —
     * tarif validé pour EXTERNALISATION (mis à jour à la validation de la
     * proforma), sinon le montant DEBOURS validé. La facture générale partagée
     * n'est pas attribuable à la ligne (mode « unique »).
     */
    public BigDecimal montantAttendu(LigneService ligne) {
        if (ligne == null) {
            return BigDecimal.ZERO;
        }
        if (estDebours(ligne)) {
            return ligne.getMontantDebours() != null ? ligne.getMontantDebours() : BigDecimal.ZERO;
        }
        return ligne.getTarif() != null ? ligne.getTarif() : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal montantTotalDemande(Integer demandeId) {
        return ligneRepository.findByDemandeId(demandeId).stream()
                .map(this::montantAttendu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LigneServiceResponse toResponse(LigneService ligne) {
        return toResponse(ligne, montantPayeParLigne(ligne.getId()));
    }

    private LigneServiceResponse toResponse(LigneService ligne, BigDecimal paye) {
        BigDecimal attendu = montantAttendu(ligne);
        return DemandeMapper.toLigneResponse(ligne, paye, attendu.subtract(paye).max(BigDecimal.ZERO));
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

    public static boolean estDebours(LigneService ligne) {
        return ligne != null
                && CATEGORIE_DEBOURS.equalsIgnoreCase(
                        ligne.getCategorie() != null ? ligne.getCategorie().trim() : null);
    }

    public static String normaliserCategorie(String categorie) {
        if (categorie == null || categorie.isBlank()) {
            return CATEGORIE_EXTERNALISATION;
        }
        String normalisee = categorie.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CATEGORIE_EXTERNALISATION.equals(normalisee)
                && !CATEGORIE_DEBOURS.equals(normalisee)) {
            throw new BusinessException(ErrorMessages.CATEGORIE_DEMANDE_INVALIDE);
        }
        return normalisee;
    }

    public static boolean estStandard(Service service) {
        return service != null && service.getTypeService() != null
                && "STANDARD".equalsIgnoreCase(service.getTypeService().trim());
    }

    /**
     * Valide/normalise le sous-type d'une ligne EXTERNALISATION.
     * STANDARD → PRESTATION (défaut) ou ABONNEMENT_MENSUEL.
     * CREATION / TRANSACTION → null obligatoire.
     * doit être appelé après chargement du service.
     */
    public static String normaliserTypePrestation(String categorie, Service service,
                                                  String typePrestation) {
        if (CATEGORIE_DEBOURS.equalsIgnoreCase(categorie)) {
            if (typePrestation != null && !typePrestation.isBlank()) {
                throw new BusinessException(ErrorMessages.TYPE_PRESTATION_INTERDIT);
            }
            return null;
        }
        if (!estStandard(service)) {
            if (typePrestation != null && !typePrestation.isBlank()) {
                throw new BusinessException(ErrorMessages.TYPE_PRESTATION_INTERDIT);
            }
            return null;
        }
        if (typePrestation == null || typePrestation.isBlank()) {
            return PRESTATION;
        }
        String normalise = typePrestation.trim().toUpperCase(java.util.Locale.ROOT);
        if (!PRESTATION.equals(normalise) && !ABONNEMENT_MENSUEL.equals(normalise)) {
            throw new BusinessException(ErrorMessages.TYPE_PRESTATION_INVALIDE);
        }
        return normalise;
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
