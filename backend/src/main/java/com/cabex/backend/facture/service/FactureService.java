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
import com.cabex.backend.facture.dto.FactureValideeLigneResponse;
import com.cabex.backend.facture.dto.FactureValideeResponse;
import com.cabex.backend.facture.dto.InclusionDisponibleResponse;
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
import java.util.Comparator;
import java.util.HashSet;
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
    private final com.cabex.backend.facture.repository.FactureValideeLigneRepository factureValideeLigneRepository;

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
                          com.cabex.backend.paiement.repository.PaiementRepository paiementRepository,
                          com.cabex.backend.facture.repository.FactureValideeLigneRepository factureValideeLigneRepository) {
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
        this.factureValideeLigneRepository = factureValideeLigneRepository;
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
        // EXTERNALISATION : toujours dans la proforma.
        // DEBOURS : seulement si le montant est annoncé à la création (montant figé),
        // il figure alors dans la proforma et dans la facture générale validée.
        List<LigneService> lignes = lignesDansProforma(demandeId);
        if (lignes.isEmpty()) {
            return List.of();
        }

        List<Facture> crees = new ArrayList<>();
        if (MODE_UNIQUE.equals(modeNormalise)) {
            Facture facture = creerFacture(demande, modeNormalise, BigDecimal.ZERO);
            BigDecimal total = BigDecimal.ZERO;
            for (LigneService ligne : lignes) {
                ligne.setFacture(facture);
                ligne.setTarifProforma(montantLigne(ligne));
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
                ligne.setTarifProforma(montantLigne(ligne));
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
        List<LigneService> lignes = lignesDansProforma(demandeId);
        if (factures.size() == 1 && MODE_UNIQUE.equals(factures.get(0).getModeFacturation())) {
            Facture unique = factures.get(0);
            BigDecimal total = BigDecimal.ZERO;
            for (LigneService ligne : lignes) {
                if (ligne.getFacture() == null) {
                    ligne.setFacture(unique);
                    ligne.setTarifProforma(montantLigne(ligne));
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
                ligne.setTarifProforma(montantLigne(ligne));
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
                                                                  String mois,
                                                                  String commentaire,
                                                                  List<Integer> inclusionLigneIds) {
        DemandeService demande = verifierDemande(demandeId);
        String statutDemande = statutActuelDemande(demandeId);
        if ("TERMINEE".equals(statutDemande) || "ANNULEE".equals(statutDemande)) {
            throw new BusinessException(
                    "La demande est " + statutDemande + " : création de facture impossible");
        }
        if (dateEmission == null) {
            throw new BusinessException("La date d'émission de la facture est obligatoire");
        }
        java.time.YearMonth moisAnnee;
        try {
            moisAnnee = java.time.YearMonth.parse(mois);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException("Le mois facturé est obligatoire et doit être au format AAAA-MM (ex. 2026-01)");
        }
        java.time.LocalDate moisDebut = moisAnnee.atDay(1);
        java.time.format.DateTimeFormatter libelleMois =
                java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRANCE);
        String moisLabel = libelleMois.format(moisAnnee);
        moisLabel = Character.toUpperCase(moisLabel.charAt(0)) + moisLabel.substring(1);

        // Un mois ne peut être facturé qu'une fois par abonnement (pas de facture de novembre 2026 en double).
        com.cabex.backend.facture.entity.FactureValidee dejaExistante =
                factureValideeRepository.findByLigneServiceIdAndMois(ligneServiceId, moisDebut);
        if (dejaExistante != null) {
            throw new BusinessException(
                    "Une facture pour le mois " + moisLabel + " existe déjà pour cet abonnement "
                            + "(" + dejaExistante.getReferenceFactureValidee() + ")");
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
        // Le montant appliqué est le montant validé de la LIGNE d'abonnement
        // (le tarif figé par la validation de la proforma), et non celui de la
        // facture générale validée : en mode « unique », celle-ci couvre
        // plusieurs services.
        BigDecimal tarif = montantLigne(ligne);
        if (tarif == null) {
            throw new BusinessException("Le montant de l'abonnement est manquant");
        }

        // --- Lignes de la facture du mois --------------------------------
        // La facture du mois est UNIQUEMENT l'objet payable (l'abonnement) ;
        // les prestations / débours impayés mentionnés dans la facture ne sont là
        // qu'à titre de rappel et ne constituent pas une ligne payable ici.
        List<com.cabex.backend.facture.entity.FactureValideeLigne> lignesFacture =
                new ArrayList<>();
        String nomServiceAbo = ligne.getService() != null ? ligne.getService().getNomService() : null;
        lignesFacture.add(com.cabex.backend.facture.entity.FactureValideeLigne.builder()
                .ligneService(ligne)
                .designation("Abonnement mensuel"
                        + (nomServiceAbo != null ? " — " + nomServiceAbo : ""))
                .montant(tarif)
                .type("PRINCIPALE")
                .build());
        BigDecimal montantTotal = tarif;
        List<String> inclusionsNote = new ArrayList<>();
        List<Integer> idsVus = new ArrayList<>();
        Integer societeId = demande.getSociete() != null ? demande.getSociete().getId() : null;
        if (inclusionLigneIds != null) {
            for (Integer inclusionId : inclusionLigneIds) {
                if (inclusionId == null) {
                    continue;
                }
                if (idsVus.contains(inclusionId)) {
                    throw new BusinessException(
                            "La ligne n° " + inclusionId + " est incluse deux fois dans la facture");
                }
                idsVus.add(inclusionId);
                if (inclusionId.equals(ligne.getId())) {
                    throw new BusinessException(
                            "La ligne d'abonnement est déjà la ligne principale de la facture");
                }
                LigneService inclusion = ligneRepository.findById(inclusionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorMessages.introuvable("Ligne de service", inclusionId)));
                if (inclusion.getDemande() == null
                        || inclusion.getDemande().getSociete() == null
                        || !inclusion.getDemande().getSociete().getId().equals(societeId)) {
                    throw new BusinessException(
                            "L'élément n° " + inclusionId
                                    + " n'appartient pas à la même société (client)");
                }
                InclusionDisponibleResponse candidat = inclusionPourLigne(inclusion,
                        statutActuelDemande(inclusion.getDemande().getId()));
                if (candidat == null) {
                    throw new BusinessException(
                            "L'élément n° " + inclusionId
                                    + " n'est pas reprenable dans la facture "
                                    + "(déjà soldé, ou demande terminée/annulée)");
                }
                lignesFacture.add(com.cabex.backend.facture.entity.FactureValideeLigne.builder()
                        .ligneService(inclusion)
                        .designation(candidat.designation())
                        .montant(candidat.reste())
                        .type("RAPPEL")
                        .build());
                inclusionsNote.add(candidat.designation() + " (" + candidat.reste() + ")");
            }
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
                .montant(montantTotal)
                .mois(moisDebut)
                .validePar(validateur)
                .valideLe(valideLe)
                .build();
        child.setLignes(new HashSet<>());
        for (com.cabex.backend.facture.entity.FactureValideeLigne fvl : lignesFacture) {
            fvl.setFactureValidee(child);
            child.getLignes().add(fvl);
        }
        child = factureValideeRepository.save(child);
        String note = "Facture mensuelle d'abonnement " + child.getReferenceFactureValidee()
                + " — mois " + moisAnnee
                + " — émise le " + dateEmission
                + " — facture générale " + generale.getReferenceFactureValidee()
                + ", proforma " + proforma.getReferenceFacture()
                + ", demande " + demande.getReferenceDemande()
                + (inclusionsNote.isEmpty()
                        ? ""
                        : " + " + inclusionsNote.size() + " élément(s) inclus : "
                                + String.join(" ; ", inclusionsNote))
                + (commentaire != null && !commentaire.isBlank() ? " — " + commentaire.trim() : "");
        ajouterHistoriqueLigne(ligne, ligne.getStatut(), note);
        ajouterHistoriqueDemande(demande, note);
        auditService.log(AuditActions.CREATE, "FACTURE", "FactureValidee",
                child.getId() != null ? Long.valueOf(child.getId()) : null,
                null, note);
        return toFactureValideeResponse(child);
    }

    /**
     * Éléments impayés (prestation / débours) d'une demande pouvant être
     * repris dans la facture mensuelle : demande EN_COURS, montant dû > 0.
     */
    @Transactional(readOnly = true)
    public List<InclusionDisponibleResponse> inclusionsDisponibles(Integer demandeId) {
        DemandeService demande = verifierDemande(demandeId);
        if (demande.getSociete() == null) {
            return List.of();
        }
        List<DemandeService> demandes = demandeRepository
                .findBySocieteIdAndActifTrueOrderByIdAsc(demande.getSociete().getId());
        List<InclusionDisponibleResponse> resultat = new ArrayList<>();
        for (DemandeService d : demandes) {
            String statut = statutActuelDemande(d.getId());
            if ("TERMINEE".equals(statut) || "ANNULEE".equals(statut)) {
                continue;
            }
            List<LigneService> lignes = ligneRepository.findByDemandeIdOrderByIdAsc(d.getId());
            for (LigneService l : lignes) {
                InclusionDisponibleResponse candidat = inclusionPourLigne(l, statut);
                if (candidat != null) {
                    resultat.add(candidat);
                }
            }
        }
        return resultat;
    }

    /** Calcule la part payable (le reste) d'une ligne, ou null si elle n'est pas reprenable. */
    private InclusionDisponibleResponse inclusionPourLigne(LigneService inclusion,
                                                            String statutDemande) {
        if ("TERMINEE".equals(statutDemande) || "ANNULEE".equals(statutDemande)) {
            return null;
        }
        if (!"EN_COURS".equals(inclusion.getStatut())
                && !"PARTIEL".equals(inclusion.getStatut())
                && !"NON_PAYEE".equals(inclusion.getStatut())) {
            return null;
        }
        BigDecimal paye = paiementsPourLigne(inclusion);
        String designation;
        if (LigneServiceService.estDebours(inclusion)) {
            designation = "Débours — "
                    + (inclusion.getDesignationDebours() != null
                            ? inclusion.getDesignationDebours()
                            : "n° " + inclusion.getId());
        } else if (LigneServiceService.CATEGORIE_EXTERNALISATION.equalsIgnoreCase(inclusion.getCategorie())
                && LigneServiceService.PRESTATION.equalsIgnoreCase(inclusion.getTypePrestation())) {
            String nomService = inclusion.getService() != null
                    ? inclusion.getService().getNomService() : "Prestation";
            designation = "Prestation — " + nomService;
        } else {
            return null;
        }
        BigDecimal attendu = attenduPourLigne(inclusion);
        if (attendu == null) {
            attendu = BigDecimal.ZERO;
        }
        BigDecimal reste = attendu.subtract(paye).max(BigDecimal.ZERO);
        if (reste.signum() <= 0) {
            return null;
        }
        return new InclusionDisponibleResponse(inclusion.getId(),
                inclusion.getDemande().getId(),
                inclusion.getDemande().getReferenceDemande(),
                designation,
                LigneServiceService.estDebours(inclusion) ? "DEBOURS" : "PRESTATION",
                reste);
    }

    /**
     * Montant attendu d'une ligne : le montant validé de LA LIGNE —
     * montant du débours (fixé) ou tarif validé (mis à jour à la validation
     * de la proforma). N'utilise jamais la facture générale (partagée avec
     * les autres services en mode UNIQUE).
     */
    private BigDecimal attenduPourLigne(LigneService l) {
        if (LigneServiceService.estDebours(l)) {
            return l.getMontantDebours() != null ? l.getMontantDebours() : BigDecimal.ZERO;
        }
        return l.getTarif() != null ? l.getTarif() : BigDecimal.ZERO;
    }

    /** Somme payée pour une ligne : facture générale (par service uniquement), enfants, débours et factures combinées. */
    private BigDecimal paiementsPourLigne(LigneService ligne) {
        BigDecimal paye = BigDecimal.ZERO;
        // En mode « facture générale unique », la facture est partagée entre les
        // services : son montant n'est attribuable à aucune ligne ; chaque
        // service paie sur sa propre facture enfant.
        if (ligne.getFacture() != null
                && !MODE_UNIQUE.equals(ligne.getFacture().getModeFacturation())) {
            com.cabex.backend.facture.entity.FactureValidee fv = factureValideeRepository
                    .findByFactureProformaId(ligne.getFacture().getId()).orElse(null);
            if (fv != null) {
                BigDecimal s = paiementRepository.sommePayeeParFactureValidee(fv.getId());
                paye = paye.add(s != null ? s : BigDecimal.ZERO);
            }
        }
        List<Integer> lieesIds = factureValideeRepository.findByLigneServiceId(ligne.getId()).stream()
                .filter(f -> f.getLignes() == null || f.getLignes().isEmpty())
                .map(com.cabex.backend.facture.entity.FactureValidee::getId)
                .toList();
        if (!lieesIds.isEmpty()) {
            BigDecimal s = paiementRepository.sommePayeeParFacturesValidees(lieesIds);
            paye = paye.add(s != null ? s : BigDecimal.ZERO);
        }
        BigDecimal sCombi = factureValideeLigneRepository.sommePayeeViaFacturesCombinees(ligne.getId());
        paye = paye.add(sCombi != null ? sCombi : BigDecimal.ZERO);
        return paye;
    }

    /**
     * Facture validée créée à la validation d'un montant DEBOURS
     * (facture sans proforma, liée à la ligne de débours). En mode
     * « facture générale unique », la facture générale validée y est
     * rattachée : la référence de la facture générale figure toujours
     * dans la facture du service.
     */
    @Transactional
    public FactureValidee creerFactureValideePourDebours(LigneService ligne, BigDecimal montant) {
        Utilisateur validateur = currentUtilisateur();
        LocalDateTime valideLe = LocalDateTime.now();
        com.cabex.backend.facture.entity.FactureValidee generale = null;
        if (ligne.getDemande() != null) {
            boolean modeUnique = factureRepository
                    .findByDemandeId(ligne.getDemande().getId()).stream()
                    .anyMatch(f -> MODE_UNIQUE.equals(f.getModeFacturation()));
            if (modeUnique) {
                generale = factureValideeRepository
                        .findByDemandeIdOrderByIdAsc(ligne.getDemande().getId())
                        .stream()
                        .filter(f -> f.getFactureProforma() != null)
                        .findFirst().orElse(null);
            }
        }
        FactureValidee creee = FactureValidee.builder()
                .referenceFactureValidee(genererReferenceValidee(valideLe.getYear()))
                .factureProforma(null)
                .factureGenerale(generale)
                .ligneService(ligne)
                .demande(ligne.getDemande())
                .montant(montant)
                .validePar(validateur)
                .valideLe(valideLe)
                .build();
        return factureValideeRepository.save(creee);
    }

    /**
     * Lignes de la facture validée à afficher : lignes du type ABONNEMENT_MENSUEL
     * créées à la facture, lignes de la proforma pour une facture générale validée,
     * ou une ligne unique pour un débours direct.
     */
    private List<FactureValideeLigneResponse> construireLignesFacture(FactureValidee fv,
                                                                      LigneService ligne) {
        // Facture enfant d'une « facture générale » validée (mode UNIQUE) :
        // facture mono-ligne pour le paiement (PRESTATION / débours / création...).
        if (fv.getFactureGenerale() != null && fv.getMois() == null && ligne != null) {
            String designation;
            if (LigneServiceService.estDebours(ligne)) {
                designation = "Débours — " + (ligne.getDesignationDebours() != null
                        ? ligne.getDesignationDebours() : "n° " + ligne.getId());
            } else {
                designation = DemandeMapper.libelleLigne(ligne);
                if (designation == null || designation.isBlank()) {
                    designation = ligne.getService() != null
                            ? ligne.getService().getNomService() : "Service " + ligne.getId();
                }
            }
            String nomService = ligne.getService() != null ? ligne.getService().getNomService() : null;
            return List.of(new FactureValideeLigneResponse(
                    null,
                    ligne.getId(),
                    ligne.getDemande() != null ? ligne.getDemande().getReferenceDemande() : null,
                    designation,
                    fv.getMontant() != null ? fv.getMontant() : BigDecimal.ZERO,
                    "PRINCIPALE",
                    nomService));
        }
        // Lignes enfants (facture combinée du mois).
        if (fv.getLignes() != null && !fv.getLignes().isEmpty()) {
            return fv.getLignes().stream()
                    .sorted(Comparator.comparing(
                            com.cabex.backend.facture.entity.FactureValideeLigne::getId,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(l -> new FactureValideeLigneResponse(
                            l.getId(),
                            l.getLigneService() != null ? l.getLigneService().getId() : null,
                            l.getLigneService() != null && l.getLigneService().getDemande() != null
                                    ? l.getLigneService().getDemande().getReferenceDemande() : null,
                            l.getDesignation(),
                            l.getMontant(),
                            l.getType() != null ? l.getType() : "PRINCIPALE",
                            l.getLigneService() != null && l.getLigneService().getService() != null
                                    ? l.getLigneService().getService().getNomService() : null))
                    .toList();
        }
        // Facture générale (proforma validée) : une ligne par ligne de la proforma.
        if (fv.getFactureProforma() != null && fv.getFactureProforma().getDemande() != null) {
            List<LigneService> lignesProforma = ligneRepository
                    .findByDemandeIdOrderByIdAsc(fv.getFactureProforma().getDemande().getId()).stream()
                    .filter(l -> l.getFacture() != null
                            && l.getFacture().getId().equals(fv.getFactureProforma().getId()))
                    .toList();
            return lignesProforma.stream()
                    .map(l -> {
                        String nomService = l.getService() != null ? l.getService().getNomService()
                                : DemandeMapper.libelleLigne(l);
                        return new FactureValideeLigneResponse(
                                null,
                                l.getId(),
                                fv.getFactureProforma().getDemande().getReferenceDemande(),
                                nomService,
                                l.getTarifProforma() != null ? l.getTarifProforma() : l.getTarif(),
                                "PRINCIPALE",
                                l.getService() != null ? l.getService().getNomService() : null);
                    })
                    .toList();
        }
        // Débours : une seule ligne reprenant la désignation du débours.
        if (ligne != null && LigneServiceService.estDebours(ligne)) {
            return List.of(new FactureValideeLigneResponse(
                    null,
                    ligne.getId(),
                    ligne.getDemande() != null ? ligne.getDemande().getReferenceDemande() : null,
                    "Débours — " + (ligne.getDesignationDebours() != null
                            ? ligne.getDesignationDebours() : "Débours"),
                    fv.getMontant() != null ? fv.getMontant() : BigDecimal.ZERO,
                    "PRINCIPALE",
                    null));
        }
        return List.of();
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
            if (fv.getMois() != null) {
                type = "ABONNEMENT_MENSUEL";
            } else if (ligne != null && LigneServiceService.estDebours(ligne)) {
                type = "DEBOURS";
            } else if (ligne != null && ligne.getTypePrestation() != null) {
                type = ligne.getTypePrestation();
            } else if (ligne != null && ligne.getService() != null
                    && ligne.getService().getTypeService() != null
                    && !"STANDARD".equalsIgnoreCase(ligne.getService().getTypeService())) {
                type = ligne.getService().getTypeService();
            } else {
                type = "PRESTATION";
            }
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
        List<FactureValideeLigneResponse> lignesResp = construireLignesFacture(fv, ligne);
        String nomService = ligne != null && ligne.getService() != null
                ? ligne.getService().getNomService() : null;
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
                nomService,
                montant,
                paye,
                reste,
                type,
                fv.getValidePar() != null
                        ? fv.getValidePar().getPrenom() + " " + fv.getValidePar().getNom() : null,
                fv.getValideLe(),
                lignesResp,
                fv.getMois() != null
                        ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM").format(fv.getMois())
                        : null,
                fv.getFactureProforma() != null
                        ? fv.getFactureProforma().getModeFacturation() : null);
    }

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire) {
        return valider(factureId, commentaire, null, null);
    }

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire, BigDecimal montantValide) {
        return valider(factureId, commentaire, montantValide, null);
    }

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire, BigDecimal montantValide,
                                    List<com.cabex.backend.facture.dto.ValiderFactureLigneRequest> lignesValidees) {
        Facture facture = getFacture(factureId);
        if (!FACTURE_PROFORMA.equals(facture.getStatut())) {
            throw new BusinessException(
                    "Seule une facture proforma peut être validée (statut actuel : "
                            + facture.getStatut() + ")");
        }
        if (factureValideeRepository.existsByFactureProformaId(facture.getId())) {
            throw new BusinessException("Cette facture proforma a déjà été validée");
        }
        List<LigneService> lignesLignees = ligneRepository.findByDemandeId(facture.getDemande().getId()).stream()
                .filter(l -> l.getFacture() != null && l.getFacture().getId().equals(facture.getId()))
                .toList();

        // --- Validation par ligne : chaque tarif de ligne devient le tarif validé ---
        BigDecimal montantFinal;
        if (lignesValidees != null && !lignesValidees.isEmpty()) {
            if (lignesValidees.size() != lignesLignees.size()) {
                throw new BusinessException(
                        "La liste des lignes validées (" + lignesValidees.size()
                                + ") ne correspond pas à la facture (" + lignesLignees.size() + " ligne(s))");
            }
            for (com.cabex.backend.facture.dto.ValiderFactureLigneRequest req : lignesValidees) {
                if (req == null || req.ligneServiceId() == null) {
                    throw new BusinessException("Une ligne à valider ne peut pas être vide");
                }
                if (req.montant() == null || req.montant().signum() < 0) {
                    throw new BusinessException(
                            "Le montant validé d'une ligne doit être supérieur ou nul");
                }
                LigneService ligne = null;
                for (LigneService l : lignesLignees) {
                    if (l.getId() != null && l.getId().equals(req.ligneServiceId())) {
                        ligne = l;
                        break;
                    }
                }
                if (ligne == null) {
                    throw new BusinessException(
                            "La ligne " + req.ligneServiceId()
                                    + " n'appartient pas à la facture " + facture.getReferenceFacture());
                }
                if (LigneServiceService.estDebours(ligne)) {
                    // Montant du débours figé à la création : il ne peut pas
                    // être ajusté à la validation, seulement constaté.
                    BigDecimal attendu = ligne.getMontantDebours() != null
                            ? ligne.getMontantDebours() : BigDecimal.ZERO;
                    if (req.montant().compareTo(attendu) != 0) {
                        throw new BusinessException(
                                "Le montant du débours « "
                                        + (ligne.getDesignationDebours() != null
                                                ? ligne.getDesignationDebours() : "n° " + ligne.getId())
                                        + " » est figé à " + attendu
                                        + " : il ne peut pas être modifié à la validation");
                    }
                    ajouterHistoriqueLigne(ligne,
                            ligne.getStatut() != null ? ligne.getStatut() : LigneServiceService.LIGNE_NON_PAYEE,
                            "Débours figuré dans la facture proforma (montant figé) : " + attendu);
                    continue;
                }
                BigDecimal ancien = ligne.getTarif();
                ligne.setTarif(req.montant());
                ligneRepository.save(ligne);
                ajouterHistoriqueLigne(ligne,
                        ligne.getStatut() != null ? ligne.getStatut() : "TARIF_AJUSTE",
                        "Tarif ajusté à la validation : " + ancien + " → " + req.montant());
            }
            // Le montant total de la facture validée = somme des lignes après mise à jour.
            montantFinal = lignesLignees.stream()
                    .map(this::montantLigne)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (montantValide != null) {
            // Montant validé global (ancienne API) : sur une facture à une seule ligne,
            // le tarif de la ligne suit aussi la valeur validée. Pour un débours,
            // le montant reste figé à celui annoncé à la création.
            if (lignesLignees.size() == 1) {
                LigneService ligne = lignesLignees.get(0);
                if (LigneServiceService.estDebours(ligne)) {
                    BigDecimal attendu = ligne.getMontantDebours() != null
                            ? ligne.getMontantDebours() : BigDecimal.ZERO;
                    if (montantValide.compareTo(attendu) != 0) {
                        throw new BusinessException(
                                "Le montant du débours est figé à " + attendu
                                        + " : il ne peut pas être modifié à la validation");
                    }
                } else {
                    BigDecimal ancien = ligne.getTarif();
                    ligne.setTarif(montantValide);
                    ligneRepository.save(ligne);
                    ajouterHistoriqueLigne(ligne,
                            ligne.getStatut() != null ? ligne.getStatut() : "TARIF_AJUSTE",
                            "Tarif ajusté à la validation : " + ancien + " → " + montantValide);
                }
            }
            montantFinal = montantValide;
        } else {
            montantFinal = facture.getMontant();
        }
        if (montantFinal == null || montantFinal.signum() < 0) {
            throw new BusinessException("Le montant validé doit être supérieur ou nul");
        }
        Utilisateur validateur = currentUtilisateur();
        facture.setStatut(FACTURE_VALIDEE);
        factureRepository.save(facture);

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

        // En mode UNIQUE (« facture générale unique »), chaque service possède sa
        // propre facture validée pour payer : création automatique des factures
        // enfant (PRESTATION / débours annoncé / créations / transactions), liées
        // à la facture générale validée.
        // L'abonnement mensuel n'a pas de facture enfant : il se règle via la
        // facture du mois (créée manuellement).
        if (MODE_UNIQUE.equals(facture.getModeFacturation())) {
            Utilisateur validateurEnfant = validateur;
            for (LigneService ligne : lignesLignees) {
                boolean estAbonnement = LigneServiceService.CATEGORIE_EXTERNALISATION
                        .equalsIgnoreCase(ligne.getCategorie())
                        && LigneServiceService.ABONNEMENT_MENSUEL
                                .equalsIgnoreCase(ligne.getTypePrestation());
                if (estAbonnement) {
                    continue;
                }
                com.cabex.backend.facture.entity.FactureValidee enfant =
                        com.cabex.backend.facture.entity.FactureValidee.builder()
                                .referenceFactureValidee(genererReferenceValidee(valideLe.getYear()))
                                .factureProforma(null)
                                .factureGenerale(validee)
                                .ligneService(ligne)
                                .demande(demande)
                                .montant(montantLigne(ligne))
                                .validePar(validateurEnfant)
                                .valideLe(valideLe)
                                .build();
                factureValideeRepository.save(enfant);
                ajouterHistoriqueLigne(ligne,
                        ligne.getStatut() != null ? ligne.getStatut() : LigneServiceService.LIGNE_VALIDE,
                        "Facture créée pour le paiement : " + enfant.getReferenceFactureValidee()
                                + " — facture générale " + validee.getReferenceFactureValidee());
                auditService.log(AuditActions.CREATE, "FACTURE", "FactureValidee",
                        enfant.getId() != null ? Long.valueOf(enfant.getId()) : null,
                        null, "Facture de service " + enfant.getReferenceFactureValidee()
                                + " liée à la facture générale " + validee.getReferenceFactureValidee()
                                + " (ligne " + ligne.getId() + ")");
            }
        }

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
                        l.getTarifProforma(),
                        l.getDebutService(),
                        l.getFinService(),
                        l.getStatut(),
                        com.cabex.backend.demande.mapper.DemandeMapper.libelleLigne(l),
                        l.getCategorie() != null ? l.getCategorie() : "EXTERNALISATION"))
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

    /**
     * Lignes reprises dans la proforma : EXTERNALISATION + DEBOURS dont le
     * montant est annoncé à la création (montant figé). Les débours sans
     * montant connu attendent « Valider le montant » avant de figurer dans une
     * facture.
     */
    private List<LigneService> lignesDansProforma(Integer demandeId) {
        return ligneRepository.findByDemandeIdOrderByIdAsc(demandeId).stream()
                .filter(l -> !LigneServiceService.estDebours(l)
                        || l.getMontantDebours() != null)
                .toList();
    }

    /**
     * Montant ligne pour les calculs de factures : le montant du débours
     * pour une ligne DEBOURS, sinon le tarif. Un débours sans montant connu
     * (montant encore à valider) vaut 0 : il n'est figuré dans aucune proforma.
     */
    private BigDecimal montantLigne(LigneService ligne) {
        if (LigneServiceService.estDebours(ligne)) {
            return ligne.getMontantDebours() != null ? ligne.getMontantDebours() : BigDecimal.ZERO;
        }
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
