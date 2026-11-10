package com.cabex.backend.facture.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
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
import com.cabex.backend.demande.service.LigneServiceService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.dto.FactureLigneResponse;
import com.cabex.backend.facture.dto.FactureResponse;
import com.cabex.backend.facture.dto.FactureValideeResponse;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Facturation des demandes : génération des proformas (UNIQUE ou PAR_SERVICE),
 * actualisation tant que la demande est EN_ATTENTE, validation traçée.
 */
@Service
public class FactureService {

    public static final String MODE_UNIQUE = "UNIQUE";
    public static final String MODE_PAR_SERVICE = "PAR_SERVICE";

    public static final String FACTURE_PROFORMA = "PROFORMA";
    public static final String FACTURE_VALIDEE = "VALIDEE";
    public static final String FACTURE_ANNULEE = "ANNULEE";

    private static final String DEMANDE_EN_ATTENTE = "EN_ATTENTE";

    private final FactureRepository factureRepository;
    private final com.cabex.backend.facture.repository.FactureValideeRepository factureValideeRepository;
    private final LigneServiceRepository ligneRepository;
    private final HistoriqueLigneServiceRepository historiqueLigneRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueDemandeRepository;
    private final StatutDemandeRepository statutDemandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final com.cabex.backend.paiement.repository.PaiementRepository paiementRepository;

    public FactureService(FactureRepository factureRepository,
                          com.cabex.backend.facture.repository.FactureValideeRepository factureValideeRepository,
                          LigneServiceRepository ligneRepository,
                          HistoriqueLigneServiceRepository historiqueLigneRepository,
                          DemandeServiceRepository demandeRepository,
                          HistoriqueDemandeRepository historiqueDemandeRepository,
                          StatutDemandeRepository statutDemandeRepository,
                          UtilisateurRepository utilisateurRepository,
                          AuditService auditService,
                          NotificationService notificationService,
                          com.cabex.backend.paiement.repository.PaiementRepository paiementRepository) {
        this.factureRepository = factureRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.ligneRepository = ligneRepository;
        this.historiqueLigneRepository = historiqueLigneRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueDemandeRepository = historiqueDemandeRepository;
        this.statutDemandeRepository = statutDemandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.paiementRepository = paiementRepository;
    }

    @Transactional(readOnly = true)
    public List<FactureResponse> listerParDemande(Integer demandeId) {
        verifierDemande(demandeId);
        return factureRepository.findByDemandeId(demandeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FactureResponse getById(Integer id) {
        return toResponse(getFacture(id));
    }

    /** Toutes les factures de la demande existent et sont validées. */
    @Transactional(readOnly = true)
    public boolean toutesValidees(Integer demandeId) {
        List<Facture> factures = factureRepository.findByDemandeId(demandeId);
        if (factures.isEmpty()) {
            return false;
        }
        return factures.stream().allMatch(f -> FACTURE_VALIDEE.equals(f.getStatut()));
    }

    @Transactional
    public List<FactureResponse> genererPourDemande(Integer demandeId, String mode) {
        DemandeService demande = verifierDemande(demandeId);
        exigerEnAttente(demande);
        String modeNormalise = normaliserMode(mode);
        if (!factureRepository.findByDemandeId(demandeId).isEmpty()) {
            throw new BusinessException(
                    "Des factures existent déjà : modifiez les lignes de service pour les actualiser");
        }
        // Seules les lignes EXTERNALISATION génèrent une proforma.
        List<LigneService> lignes = ligneRepository.findByDemandeIdOrderByIdAsc(demandeId)
                .stream().filter(l -> !LigneServiceService.estDebours(l)).toList();
        if (lignes.isEmpty()) {
            return List.of();
        }

        List<Facture> crees = new ArrayList<>();
        if (MODE_UNIQUE.equals(modeNormalise)) {
            Facture facture = creerFacture(demande, modeNormalise, BigDecimal.ZERO);
            BigDecimal total = BigDecimal.ZERO;
            for (LigneService ligne : lignes) {
                ligne.setFacture(facture);
                ligneRepository.save(ligne);
                total = total.add(montantLigne(ligne));
                ajouterHistoriqueLigne(ligne, LigneServiceService.LIGNE_CREE,
                        "Facture proforma unique générée : " + facture.getReferenceFacture());
            }
            facture.setMontant(total);
            crees.add(factureRepository.save(facture));
        } else {
            for (LigneService ligne : lignes) {
                Facture facture = creerFacture(demande, modeNormalise, montantLigne(ligne));
                facture = factureRepository.save(facture);
                ligne.setFacture(facture);
                ligneRepository.save(ligne);
                ajouterHistoriqueLigne(ligne, LigneServiceService.LIGNE_CREE,
                        "Facture proforma générée : " + facture.getReferenceFacture());
                crees.add(facture);
            }
        }

        ajouterHistoriqueDemande(demande,
                "Factures proforma générées (mode " + modeNormalise + ") : " + crees.size());
        auditService.log(AuditActions.CREATE, "FACTURE", "Facture",
                null, null, demande.getReferenceDemande() + " : " + modeNormalise);

        return crees.stream().map(this::toResponse).toList();
    }

    /**
     * Actualise les montants après ajout/modification/retrait d'une ligne.
     * Bloqué dès que la demande a quitté EN_ATTENTE ou qu'une facture est validée.
     */
    @Transactional
    public void actualiserPourDemande(Integer demandeId) {
        DemandeService demande = verifierDemande(demandeId);
        exigerEnAttente(demande);
        List<Facture> factures = factureRepository.findByDemandeId(demandeId);
        if (factures.isEmpty()) {
            return;
        }
        for (Facture facture : factures) {
            if (!FACTURE_PROFORMA.equals(facture.getStatut())) {
                throw new BusinessException(
                        "La facture " + facture.getReferenceFacture() + " est déjà validée : modification impossible");
            }
        }
        List<LigneService> lignes = ligneRepository.findByDemandeIdOrderByIdAsc(demandeId)
                .stream().filter(l -> !LigneServiceService.estDebours(l)).toList();
        if (factures.size() == 1 && MODE_UNIQUE.equals(factures.get(0).getModeFacturation())) {
            Facture unique = factures.get(0);
            BigDecimal total = BigDecimal.ZERO;
            for (LigneService ligne : lignes) {
                if (ligne.getFacture() == null) {
                    ligne.setFacture(unique);
                    ligneRepository.save(ligne);
                }
                total = total.add(montantLigne(ligne));
            }
            unique.setMontant(total);
            factureRepository.save(unique);
            return;
        }
        for (LigneService ligne : lignes) {
            if (ligne.getFacture() == null) {
                Facture facture = creerFacture(demande, MODE_PAR_SERVICE, montantLigne(ligne));
                facture = factureRepository.save(facture);
                ligne.setFacture(facture);
                ligneRepository.save(ligne);
            } else {
                Facture facture = ligne.getFacture();
                facture.setMontant(montantLigne(ligne));
                factureRepository.save(facture);
            }
        }
        // Supprime les proformas orphelines (ligne retirée).
        for (Facture facture : factureRepository.findByDemandeId(demandeId)) {
            boolean rattachee = lignes.stream().anyMatch(
                    l -> l.getFacture() != null && l.getFacture().getId().equals(facture.getId()));
            if (!rattachee && MODE_PAR_SERVICE.equals(facture.getModeFacturation())) {
                factureRepository.delete(facture);
            }
        }
    }

    /** Supprime la proforma individuelle d'une ligne avant son retrait. */
    @Transactional
    public void detacherAvantRetraitLigne(LigneService ligne) {
        Facture facture = ligne.getFacture();
        if (facture == null) {
            return;
        }
        if (!FACTURE_PROFORMA.equals(facture.getStatut())) {
            throw new BusinessException(
                    "La facture " + facture.getReferenceFacture() + " est déjà validée : retrait impossible");
        }
        if (MODE_PAR_SERVICE.equals(facture.getModeFacturation())) {
            ligne.setFacture(null);
            factureRepository.delete(facture);
        }
    }

    // ------------------------------------------------------------------
    // Factures validées (proforma validée, facture du mois d'abonnement,
    // facture débours)
    // ------------------------------------------------------------------

    /**
     * Toutes les factures validées de la demande, avec le suivi des paiements
     * (payé / reste à payer). Utilisée notamment par l'écran d'encaissement.
     */
    @Transactional(readOnly = true)
    public List<FactureValideeResponse> listerFacturesValidees(Integer demandeId) {
        verifierDemande(demandeId);
        return factureValideeRepository.findByDemandeIdOrderByIdAsc(demandeId).stream()
                .map(this::toFactureValideeResponse)
                .toList();
    }

    /**
     * Facture du mois d'un abonnement : facture validée enfant de la facture
     * générale, montant = tarif mensuel de la ligne. Dans la facture, on voit :
     * la référence de la facture générale validée, la référence de la facture
     * proforma et la référence de la demande.
     */
    @Transactional
    public FactureValideeResponse creerFactureMensuelleAbonnement(Integer demandeId,
                                                                  Integer ligneServiceId,
                                                                  LocalDate dateEmission,
                                                                  String commentaire) {
        DemandeService demande = verifierDemande(demandeId);
        String statutDemande = statutActuelDemande(demandeId);
        if ("TERMINEE".equals(statutDemande) || "ANNULEE".equals(statutDemande)) {
            throw new BusinessException(
                    "La demande est " + statutDemande + " : création de facture impossible");
        }
        if (dateEmission == null) {
            throw new BusinessException("La date d'émission de la facture est obligatoire");
        }
        LigneService ligne = ligneRepository.findById(ligneServiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Ligne de service", ligneServiceId)));
        if (ligne.getDemande() == null || !ligne.getDemande().getId().equals(demandeId)) {
            throw new BusinessException("La ligne de service n'appartient pas à cette demande");
        }
        if (!LigneServiceService.CATEGORIE_EXTERNALISATION.equalsIgnoreCase(ligne.getCategorie())
                || !LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(ligne.getTypePrestation())) {
            throw new BusinessException(
                    "Cette action est réservée aux lignes « Abonnement mensuel »");
        }
        if (!LigneServiceService.LIGNE_EN_COURS.equals(ligne.getStatut())) {
            throw new BusinessException(
                    "La facture du mois n'est disponible que pour un abonnement EN_COURS");
        }
        Facture proforma = ligne.getFacture();
        if (proforma == null || !FACTURE_VALIDEE.equals(proforma.getStatut())) {
            throw new BusinessException(
                    "La facture proforma doit être validée avant de créer la facture du mois");
        }
        FactureValidee generale = factureValideeRepository.findByFactureProformaId(proforma.getId())
                .orElseThrow(() -> new BusinessException(
                        "La facture validée associée à la proforma est introuvable"));
        BigDecimal tarif = ligne.getTarif();
        if (tarif == null) {
            throw new BusinessException("Le tarif mensuel de la ligne est manquant");
        }
        Utilisateur validateur = currentUtilisateur();
        // La date d'émission de la facture est celle saisie par l'utilisateur ;
        // created_at (timestamp automatique) garde l'horodatage réel de saisie.
        LocalDateTime valideLe = dateEmission.atStartOfDay();
        FactureValidee child = FactureValidee.builder()
                .referenceFactureValidee(genererReferenceValidee(valideLe.getYear()))
                .factureProforma(null)
                .factureGenerale(generale)
                .ligneService(ligne)
                .demande(demande)
                .montant(tarif)
                .validePar(validateur)
                .valideLe(valideLe)
                .build();
        child = factureValideeRepository.save(child);
        String note = "Facture mensuelle d'abonnement " + child.getReferenceFactureValidee()
                + " — émise le " + dateEmission
                + " — facture générale " + generale.getReferenceFactureValidee()
                + ", proforma " + proforma.getReferenceFacture()
                + ", demande " + demande.getReferenceDemande()
                + (commentaire != null && !commentaire.isBlank() ? " — " + commentaire.trim() : "");
        ajouterHistoriqueLigne(ligne, ligne.getStatut(), note);
        ajouterHistoriqueDemande(demande, note);
        auditService.log(AuditActions.CREATE, "FACTURE", "FactureValidee",
                child.getId() != null ? Long.valueOf(child.getId()) : null,
                null, note);
        return toFactureValideeResponse(child);
    }

    /**
     * Facture validée créée à la validation d'une ligne DEBOURS
     * (facture sans proforma, liée à la ligne de débours).
     */
    @Transactional
    public FactureValidee creerFactureValideePourDebours(LigneService ligne, BigDecimal montant) {
        Utilisateur validateur = currentUtilisateur();
        LocalDateTime valideLe = LocalDateTime.now();
        FactureValidee creee = FactureValidee.builder()
                .referenceFactureValidee(genererReferenceValidee(valideLe.getYear()))
                .factureProforma(null)
                .ligneService(ligne)
                .demande(ligne.getDemande())
                .montant(montant)
                .validePar(validateur)
                .valideLe(valideLe)
                .build();
        return factureValideeRepository.save(creee);
    }

    private FactureValideeResponse toFactureValideeResponse(FactureValidee fv) {
        BigDecimal paye = fv.getId() != null ? paiementRepository.sommePayeeParFactureValidee(fv.getId()) : BigDecimal.ZERO;
        if (paye == null) {
            paye = BigDecimal.ZERO;
        }
        BigDecimal montant = fv.getMontant() != null ? fv.getMontant() : BigDecimal.ZERO;
        BigDecimal reste = montant.subtract(paye).max(BigDecimal.ZERO);

        LigneService ligne = fv.getLigneService();
        // Pour une facture générale (proforma validée), la ligne vient de la proforma.
        if (ligne == null && fv.getFactureProforma() != null
                && fv.getFactureProforma().getDemande() != null) {
            List<LigneService> lignesProforma = ligneRepository
                    .findByDemandeIdOrderByIdAsc(fv.getFactureProforma().getDemande().getId()).stream()
                    .filter(l -> l.getFacture() != null
                            && l.getFacture().getId().equals(fv.getFactureProforma().getId()))
                    .toList();
            ligne = lignesProforma.isEmpty() ? null : lignesProforma.get(0);
        }
        String libelleLigne;
        if (ligne != null && LigneServiceService.estDebours(ligne)) {
            libelleLigne = ligne.getDesignationDebours() != null ? ligne.getDesignationDebours() : "Débours";
        } else if (ligne != null) {
            libelleLigne = DemandeMapper.libelleLigne(ligne);
        } else {
            libelleLigne = null;
        }
        String type;
        if (fv.getFactureGenerale() != null) {
            type = "ABONNEMENT_MENSUEL";
        } else if (fv.getFactureProforma() == null) {
            type = "DEBOURS";
        } else {
            type = "GENERALE";
        }
        String referenceProforma;
        if (fv.getFactureProforma() != null) {
            referenceProforma = fv.getFactureProforma().getReferenceFacture();
        } else if (fv.getFactureGenerale() != null && fv.getFactureGenerale().getFactureProforma() != null) {
            referenceProforma = fv.getFactureGenerale().getFactureProforma().getReferenceFacture();
        } else {
            referenceProforma = null;
        }
        return new FactureValideeResponse(
                fv.getId(),
                fv.getReferenceFactureValidee(),
                referenceProforma,
                fv.getFactureGenerale() != null ? fv.getFactureGenerale().getReferenceFactureValidee() : null,
                fv.getFactureGenerale() != null ? fv.getFactureGenerale().getId() : null,
                fv.getDemande() != null ? fv.getDemande().getId() : null,
                fv.getDemande() != null ? fv.getDemande().getReferenceDemande() : null,
                ligne != null ? ligne.getId() : null,
                libelleLigne,
                montant,
                paye,
                reste,
                type,
                fv.getValidePar() != null
                        ? fv.getValidePar().getPrenom() + " " + fv.getValidePar().getNom() : null,
                fv.getValideLe());
    }

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire) {
        return valider(factureId, commentaire, null);
    }

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire, BigDecimal montantValide) {
        Facture facture = getFacture(factureId);
        if (!FACTURE_PROFORMA.equals(facture.getStatut())) {
            throw new BusinessException(
                    "Seule une facture proforma peut être validée (statut actuel : "
                            + facture.getStatut() + ")");
        }
        if (factureValideeRepository.existsByFactureProformaId(facture.getId())) {
            throw new BusinessException("Cette facture proforma a déjà été validée");
        }
        BigDecimal montantFinal = montantValide != null ? montantValide : facture.getMontant();
        if (montantFinal == null || montantFinal.signum() < 0) {
            throw new BusinessException("Le montant validé doit être supérieur ou nul");
        }
        Utilisateur validateur = currentUtilisateur();
        facture.setStatut(FACTURE_VALIDEE);
        facture = factureRepository.save(facture);

        LocalDateTime valideLe = LocalDateTime.now();
        com.cabex.backend.facture.entity.FactureValidee validee =
                com.cabex.backend.facture.entity.FactureValidee.builder()
                        .referenceFactureValidee(genererReferenceValidee(valideLe.getYear()))
                        .factureProforma(facture)
                        .demande(facture.getDemande())
                        .montant(montantFinal)
                        .validePar(validateur)
                        .valideLe(valideLe)
                        .build();
        factureValideeRepository.save(validee);

        List<LigneService> lignes = ligneRepository.findByDemandeId(facture.getDemande().getId());
        DemandeService demande = facture.getDemande();

        // Pour chaque ligne EXTERNALISATION de la demande liée à cette facture,
        // passer le statut à EN_COURS (VALIDATION automatique après validation proforma).
        boolean demandePasseEnCours = false;
        String statutActuel = statutActuelDemande(demande.getId());
        for (LigneService ligne : lignes) {
            if (ligne.getFacture() != null && ligne.getFacture().getId().equals(facture.getId())) {
                if (LigneServiceService.CATEGORIE_EXTERNALISATION.equalsIgnoreCase(
                        ligne.getCategorie())
                        && (LigneServiceService.PRESTATION.equalsIgnoreCase(ligne.getTypePrestation())
                        || LigneServiceService.ABONNEMENT_MENSUEL.equalsIgnoreCase(ligne.getTypePrestation()))) {
                    // Pour PRESTATION et ABONNEMENT_MENSUEL : après validation de la facture
                    // proforma, la ligne passe EN_COURS et la demande passe EN_COURS.
                    ligne.setStatut(LigneServiceService.LIGNE_EN_COURS);
                    ligneRepository.save(ligne);
                    ajouterHistoriqueLigne(ligne, LigneServiceService.LIGNE_EN_COURS,
                            "Facture validée : " + facture.getReferenceFacture()
                                    + " → service mis EN_COURS"
                                    + (commentaire != null && !commentaire.isBlank()
                                            ? " — " + commentaire : ""));
                    if (!LigneServiceService.LIGNE_EN_COURS.equals(statutActuel)) {
                        demandePasseEnCours = true;
                    }
                } else {
                    if (ligne.getStatut() == null) {
                        ligne.setStatut(LigneServiceService.LIGNE_VALIDE);
                        ligneRepository.save(ligne);
                    }
                    ajouterHistoriqueLigne(ligne, LigneServiceService.LIGNE_VALIDE,
                            "Facture validée : " + facture.getReferenceFacture()
                                    + (commentaire != null && !commentaire.isBlank()
                                            ? " — " + commentaire : ""));
                }
            }
        }

        if (demandePasseEnCours && !LigneServiceService.LIGNE_EN_COURS.equals(statutActuel)) {
            StatutDemande statutEnCours = statutDemandeRepository.findByStatut(
                    LigneServiceService.LIGNE_EN_COURS)
                    .orElseThrow(() -> new BusinessException(ErrorMessages.STATUT_INVALIDE));
            ajouterHistoriqueDemande(demande,
                    "Demande passée EN_COURS après validation de la facture proforma "
                            + facture.getReferenceFacture());
            notificationService.notifierChangementStatut(demande, LigneServiceService.LIGNE_EN_COURS);
            auditService.log(AuditActions.STATUS_CHANGE, "DEMANDE", "DemandeService",
                    Long.valueOf(demande.getId()), statutActuel, LigneServiceService.LIGNE_EN_COURS);
        }

        ajouterHistoriqueDemande(demande,
                "Facture validée : proforma " + facture.getReferenceFacture()
                        + " → validée " + validee.getReferenceFactureValidee()
                        + " (" + montantFinal + ")");

        auditService.log(AuditActions.STATUS_CHANGE, "FACTURE", "Facture",
                Long.valueOf(facture.getId()), FACTURE_PROFORMA, FACTURE_VALIDEE);

        return toResponse(facture);
    }

    public FactureResponse toResponse(Facture facture) {
        List<FactureLigneResponse> lignes = ligneRepository.findByDemandeId(
                        facture.getDemande() != null ? facture.getDemande().getId() : -1).stream()
                .filter(l -> l.getFacture() != null && l.getFacture().getId().equals(facture.getId()))
                .map(l -> new com.cabex.backend.facture.dto.FactureLigneResponse(
                        l.getId(),
                        l.getService() != null ? l.getService().getId() : null,
                        l.getService() != null ? l.getService().getNomService() : null,
                        l.getTarif(),
                        l.getDebutService(),
                        l.getFinService(),
                        l.getStatut(),
                        com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(l)))
                .toList();
        com.cabex.backend.facture.entity.FactureValidee validee =
                facture.getId() != null
                        ? factureValideeRepository.findByFactureProformaId(facture.getId()).orElse(null)
                        : null;
        return new com.cabex.backend.facture.dto.FactureResponse(
                facture.getId(),
                facture.getReferenceFacture(),
                validee != null ? validee.getReferenceFactureValidee() : null,
                validee != null ? validee.getId() : null,
                facture.getDemande() != null ? facture.getDemande().getId() : null,
                facture.getDemande() != null ? facture.getDemande().getReferenceDemande() : null,
                facture.getModeFacturation(),
                facture.getMontant(),
                validee != null ? validee.getMontant() : null,
                facture.getStatut(),
                validee != null && validee.getValidePar() != null ? validee.getValidePar().getId() : null,
                validee != null && validee.getValidePar() != null
                        ? validee.getValidePar().getPrenom() + " " + validee.getValidePar().getNom()
                        : null,
                validee != null ? validee.getValideLe() : null,
                lignes);
    }

    private Facture creerFacture(DemandeService demande, String mode, BigDecimal montant) {
        return Facture.builder()
                .referenceFacture(genererReferenceProforma(demande))
                .demande(demande)
                .modeFacturation(mode)
                .montant(montant)
                .statut(FACTURE_PROFORMA)
                .build();
    }

    private BigDecimal montantLigne(LigneService ligne) {
        return ligne.getTarif() != null ? ligne.getTarif() : BigDecimal.ZERO;
    }

    private String normaliserMode(String mode) {
        if (mode == null) {
            return MODE_PAR_SERVICE;
        }
        String normalise = mode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!MODE_UNIQUE.equals(normalise) && !MODE_PAR_SERVICE.equals(normalise)) {
            throw new BusinessException("Mode de facturation invalide (UNIQUE ou PAR_SERVICE)");
        }
        return normalise;
    }

    private void exigerEnAttente(DemandeService demande) {
        String statut = historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(
                        demande.getId())
                .map(h -> h.getStatut() != null ? h.getStatut().getStatut() : null)
                .orElse(null);
        if (!DEMANDE_EN_ATTENTE.equals(statut)) {
            throw new BusinessException(
                    "La facturation ne peut être modifiée que quand la demande est EN_ATTENTE");
        }
    }

    /**
     * Référence proforma : PRO-DDMMYYYY-XXXXX.
     * La date est celle de la demande, XXXXX se réinitialise chaque année.
     */
    private String genererReferenceProforma(DemandeService demande) {
        java.time.LocalDate date = demande.getDateDemande() != null
                ? demande.getDateDemande() : java.time.LocalDate.now();
        String dd = String.format("%02d", date.getDayOfMonth());
        String mm = String.format("%02d", date.getMonthValue());
        String aaaa = String.valueOf(date.getYear());
        String prefix = "PRO-" + dd + mm + aaaa + "-";
        long seq = factureRepository.countByAnneeDemande(date.getYear()) + 1;
        String reference;
        do {
            reference = prefix + String.format("%05d", seq);
            seq++;
        } while (factureRepository.existsByReferenceFacture(reference));
        return reference;
    }

    /** Référence validée : XXXXXEXT/TNR/YYYY, XXXXX réinitialisé chaque année. */
    private String genererReferenceValidee(int annee) {
        String suffixe = "EXT/TNR/" + annee;
        long seq = factureValideeRepository.countByReferenceFactureValideeEndingWith(suffixe) + 1;
        String reference;
        do {
            if (seq > 99999) {
                throw new BusinessException("Limite de numéros de facture atteinte pour " + annee);
            }
            reference = String.format("%05d", seq) + suffixe;
            seq++;
        } while (factureValideeRepository.existsByReferenceFactureValidee(reference));
        return reference;
    }

    private void ajouterHistoriqueLigne(LigneService ligne, String statut, String commentaire) {
        historiqueLigneRepository.save(HistoriqueLigneService.builder()
                .ligneService(ligne)
                .statut(statut)
                .commentaire(commentaire)
                .utilisateur(currentUtilisateur())
                .origine("MANUEL")
                .build());
    }

    private void ajouterHistoriqueDemande(DemandeService demande, String commentaire) {
        String statut = historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(
                        demande.getId())
                .map(h -> h.getStatut() != null ? h.getStatut().getStatut() : null)
                .orElse(DEMANDE_EN_ATTENTE);
        StatutDemande statutEntite = statutDemandeRepository.findByStatut(statut)
                .orElseThrow(() -> new BusinessException(ErrorMessages.STATUT_INVALIDE));
        historiqueDemandeRepository.save(HistoriqueDemande.builder()
                .demande(demande)
                .statut(statutEntite)
                .commentaire(commentaire)
                .utilisateur(currentUtilisateur())
                .build());
    }

    private Facture getFacture(Integer id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Facture", id)));
    }

    private String statutActuelDemande(Integer demandeId) {
        return historiqueDemandeRepository.findFirstByDemandeIdOrderByIdDesc(demandeId)
                .map(h -> h.getStatut() != null ? h.getStatut().getStatut() : null)
                .orElse(null);
    }

    private DemandeService verifierDemande(Integer demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
