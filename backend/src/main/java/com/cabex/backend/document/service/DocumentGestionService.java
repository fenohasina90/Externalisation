package com.cabex.backend.document.service;

import com.cabex.backend.configuration.service.ConfigurationService;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.document.dto.DocumentResponse;
import com.cabex.backend.document.dto.FichierDocument;
import com.cabex.backend.document.entity.PieceJointe;
import com.cabex.backend.document.mapper.DocumentMapper;
import com.cabex.backend.document.repository.PieceJointeRepository;
import com.cabex.backend.echeance.entity.ProposPaiement;
import com.cabex.backend.echeance.repository.ProposPaiementRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentGestionService {

    private static final Set<String> TYPES_AUTORISES = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif",
            "doc", "docx", "xls", "xlsx", "txt");

    private final PieceJointeRepository pieceRepository;
    private final DemandeServiceRepository demandeRepository;
    private final PaiementRepository paiementRepository;
    private final ProposPaiementRepository proposRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PdfGenerator pdfGenerator;
    private final AuditService auditService;
    private final ConfigurationService configurationService;
    private final com.cabex.backend.facture.repository.FactureRepository factureRepository;

    @Value("${app.document.storage-path}")
    private String storagePath;

    @Value("${app.document.max-size}")
    private long maxSize;

    public DocumentGestionService(PieceJointeRepository pieceRepository,
                                  DemandeServiceRepository demandeRepository,
                                  PaiementRepository paiementRepository,
                                  ProposPaiementRepository proposRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  PdfGenerator pdfGenerator,
                                  AuditService auditService,
                                  ConfigurationService configurationService,
                                  com.cabex.backend.facture.repository.FactureRepository factureRepository) {
        this.pieceRepository = pieceRepository;
        this.demandeRepository = demandeRepository;
        this.paiementRepository = paiementRepository;
        this.proposRepository = proposRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.pdfGenerator = pdfGenerator;
        this.auditService = auditService;
        this.configurationService = configurationService;
        this.factureRepository = factureRepository;
    }

    /**
     * Taille maximale effective : valeur d'administration TAILLE_MAX_FICHIER,
     * avec repli sur la propriété {@code app.document.max-size}. Toute
     * modification depuis l'administration s'applique au prochain téléversement.
     */
    private long tailleMaxEffective() {
        return configurationService.lireEntierLong("TAILLE_MAX_FICHIER", maxSize);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listByDemande(Integer demandeId) {
        verifierDemande(demandeId);
        return pieceRepository.findByDemandeIdOrderByCreatedAtDesc(demandeId)
                .stream().map(DocumentMapper::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(Integer demandeId, MultipartFile fichier, String description) {
        DemandeService demande = verifierDemande(demandeId);
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est obligatoire");
        }
        String nomOriginal = fichier.getOriginalFilename() == null
                ? "document" : fichier.getOriginalFilename();
        String extension = extensionDe(nomOriginal);
        if (!TYPES_AUTORISES.contains(extension)) {
            throw new BusinessException(
                    "Type de fichier non autorisé (." + extension + "). Types acceptés : "
                            + String.join(", ", TYPES_AUTORISES));
        }
        if (fichier.getSize() > tailleMaxEffective()) {
            throw new BusinessException("Le fichier dépasse la taille maximale autorisée ("
                    + (tailleMaxEffective() / (1024 * 1024)) + " Mo)");
        }

        String nomStockage = UUID.randomUUID() + "." + extension;
        Path dossier = Paths.get(storagePath);
        try {
            Files.createDirectories(dossier);
            Path cible = dossier.resolve(nomStockage);
            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);

            PieceJointe piece = PieceJointe.builder()
                    .demande(demande)
                    .nomFichier(nomStockage)
                    .nomOriginal(nomOriginal)
                    .cheminFichier(cible.toString())
                    .typeFichier(extension)
                    .tailleFichier(fichier.getSize())
                    .description(description)
                    .uploadedBy(currentUtilisateur())
                    .build();
            PieceJointe saved = pieceRepository.save(piece);
            auditService.log(AuditActions.CREATE, "DOCUMENT", "PieceJointe",
                    Long.valueOf(saved.getId()), null, "fichier=" + nomOriginal);
            return DocumentMapper.toResponse(saved);
        } catch (IOException e) {
            throw new BusinessException("Impossible d'enregistrer le fichier : " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public FichierDocument telecharger(Integer id) {
        PieceJointe piece = getPiece(id);
        try {
            byte[] contenu = Files.readAllBytes(Paths.get(piece.getCheminFichier()));
            return new FichierDocument(piece.getId(), piece.getNomOriginal(),
                    piece.getTypeFichier(), piece.getCheminFichier(), contenu);
        } catch (IOException e) {
            throw new ResourceNotFoundException(
                    "Fichier introuvable sur le disque pour le document " + id);
        }
    }

    @Transactional
    public void delete(Integer id) {
        PieceJointe piece = getPiece(id);
        String nom = piece.getNomOriginal();
        try {
            Files.deleteIfExists(Paths.get(piece.getCheminFichier()));
        } catch (IOException ignored) {
            // le fichier peut ne plus exister; on supprime quand même l'enregistrement
        }
        pieceRepository.delete(piece);
        auditService.log(AuditActions.DELETE, "DOCUMENT", "PieceJointe",
                Long.valueOf(id), "fichier=" + nom, null);
    }

    public byte[] recapitulatifDemande(Integer demandeId) {
        DemandeService demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
        return pdfGenerator.recapitulatifDemande(demande);
    }

    public byte[] recuPaiement(Integer paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paiement", paiementId)));
        return pdfGenerator.recuPaiement(paiement);
    }

    public byte[] planPaiement(Integer demandeId) {
        ProposPaiement propos = proposRepository
                .findFirstByDemandeIdAndStatutOrderByCreatedAtDesc(demandeId, "ACTIF")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun plan de paiement actif pour la demande " + demandeId));
        return pdfGenerator.planPaiement(propos);
    }

    public byte[] factureProforma(Integer factureId) {
        com.cabex.backend.facture.entity.Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Facture", factureId)));
        return pdfGenerator.factureProforma(facture);
    }

    @Transactional
    public DocumentResponse sauvegarderRecapitulatif(Integer demandeId) {
        verifierDemande(demandeId);
        DemandeService demande = demandeRepository.findById(demandeId).orElseThrow();
        byte[] contenu = pdfGenerator.recapitulatifDemande(demande);
        return enregistrerFichier(demande, "recapitulatif-demande-" + demande.getReferenceDemande() + ".pdf",
                "Récapitulatif de demande généré automatiquement", contenu);
    }

    @Transactional
    public DocumentResponse sauvegarderRecu(Integer paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Paiement", paiementId)));
        byte[] contenu = pdfGenerator.recuPaiement(paiement);
        DemandeService demande = paiement.getDemande();
        return enregistrerFichier(demande,
                "recu-paiement-" + paiement.getReferencePaiement() + ".pdf",
                "Reçu de paiement généré automatiquement", contenu);
    }

    @Transactional
    public DocumentResponse sauvegarderPlan(Integer demandeId) {
        DemandeService demande = verifierDemande(demandeId);
        ProposPaiement propos = proposRepository
                .findFirstByDemandeIdAndStatutOrderByCreatedAtDesc(demandeId, "ACTIF")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun plan de paiement actif pour la demande " + demandeId));
        byte[] contenu = pdfGenerator.planPaiement(propos);
        return enregistrerFichier(demande,
                "plan-paiement-" + demande.getReferenceDemande() + ".pdf",
                "Plan de paiement généré automatiquement", contenu);
    }

    private DocumentResponse enregistrerFichier(DemandeService demande, String nomFichier,
                                                String description, byte[] contenu) {
        String extension = extensionDe(nomFichier);
        String nomStockage = UUID.randomUUID() + "." + extension;
        Path dossier = Paths.get(storagePath);
        try {
            Files.createDirectories(dossier);
            Path cible = dossier.resolve(nomStockage);
            Files.write(cible, contenu);
            PieceJointe piece = PieceJointe.builder()
                    .demande(demande)
                    .nomFichier(nomStockage)
                    .nomOriginal(nomFichier)
                    .cheminFichier(cible.toString())
                    .typeFichier(extension)
                    .tailleFichier((long) contenu.length)
                    .description(description)
                    .uploadedBy(currentUtilisateur())
                    .build();
            PieceJointe saved = pieceRepository.save(piece);
            auditService.log(AuditActions.CREATE, "DOCUMENT", "PieceJointe",
                    Long.valueOf(saved.getId()), null, "fichier=" + nomFichier);
            return DocumentMapper.toResponse(saved);
        } catch (IOException e) {
            throw new BusinessException("Impossible d'enregistrer le document généré : " + e.getMessage());
        }
    }

    private DemandeService verifierDemande(Integer demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande", demandeId)));
    }

    private PieceJointe getPiece(Integer id) {
        return pieceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Document", id)));
    }

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String extensionDe(String nomFichier) {
        int i = nomFichier.lastIndexOf('.');
        return i < 0 ? "bin" : nomFichier.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
