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
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final LigneServiceRepository ligneRepository;
    private final HistoriqueLigneServiceRepository historiqueLigneRepository;
    private final DemandeServiceRepository demandeRepository;
    private final HistoriqueDemandeRepository historiqueDemandeRepository;
    private final StatutDemandeRepository statutDemandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public FactureService(FactureRepository factureRepository,
                          LigneServiceRepository ligneRepository,
                          HistoriqueLigneServiceRepository historiqueLigneRepository,
                          DemandeServiceRepository demandeRepository,
                          HistoriqueDemandeRepository historiqueDemandeRepository,
                          StatutDemandeRepository statutDemandeRepository,
                          UtilisateurRepository utilisateurRepository,
                          AuditService auditService) {
        this.factureRepository = factureRepository;
        this.ligneRepository = ligneRepository;
        this.historiqueLigneRepository = historiqueLigneRepository;
        this.demandeRepository = demandeRepository;
        this.historiqueDemandeRepository = historiqueDemandeRepository;
        this.statutDemandeRepository = statutDemandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
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
        List<LigneService> lignes = ligneRepository.findByDemandeIdOrderByIdAsc(demandeId);
        if (lignes.isEmpty()) {
            throw new BusinessException("Une demande contient au moins un service");
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
        List<LigneService> lignes = ligneRepository.findByDemandeIdOrderByIdAsc(demandeId);
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

    @Transactional
    public FactureResponse valider(Integer factureId, String commentaire) {
        Facture facture = getFacture(factureId);
        if (!FACTURE_PROFORMA.equals(facture.getStatut())) {
            throw new BusinessException(
                    "Seule une facture proforma peut être validée (statut actuel : "
                            + facture.getStatut() + ")");
        }
        Utilisateur validateur = currentUtilisateur();
        facture.setStatut(FACTURE_VALIDEE);
        facture.setValidePar(validateur);
        facture.setValideLe(LocalDateTime.now());
        facture = factureRepository.save(facture);

        List<LigneService> lignes = ligneRepository.findByDemandeId(facture.getDemande().getId());
        for (LigneService ligne : lignes) {
            if (ligne.getFacture() != null && ligne.getFacture().getId().equals(facture.getId())) {
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
        DemandeService demande = facture.getDemande();
        ajouterHistoriqueDemande(demande, "Facture validée : " + facture.getReferenceFacture());

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
                        l.getStatut()))
                .toList();
        return new com.cabex.backend.facture.dto.FactureResponse(
                facture.getId(),
                facture.getReferenceFacture(),
                facture.getDemande() != null ? facture.getDemande().getId() : null,
                facture.getDemande() != null ? facture.getDemande().getReferenceDemande() : null,
                facture.getModeFacturation(),
                facture.getMontant(),
                facture.getStatut(),
                facture.getValidePar() != null ? facture.getValidePar().getId() : null,
                facture.getValidePar() != null
                        ? facture.getValidePar().getPrenom() + " " + facture.getValidePar().getNom()
                        : null,
                facture.getValideLe(),
                lignes);
    }

    private Facture creerFacture(DemandeService demande, String mode, BigDecimal montant) {
        return Facture.builder()
                .referenceFacture(genererReference(java.time.LocalDate.now().getYear()))
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

    private String genererReference(int annee) {
        String prefix = "PRO-" + annee + "-";
        long seq = factureRepository.countByReferenceFactureStartingWith(prefix) + 1;
        String reference;
        do {
            reference = prefix + String.format("%05d", seq);
            seq++;
        } while (factureRepository.existsByReferenceFacture(reference));
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
