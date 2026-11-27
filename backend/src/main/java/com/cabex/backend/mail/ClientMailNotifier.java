package com.cabex.backend.mail;

import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.client.repository.ClientResponsableRepository;
import com.cabex.backend.client.repository.SocieteRepository;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.document.service.DocumentGestionService;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.mail.MailService.PieceJointeEmail;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.paiement.repository.PaiementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Envois e-mail manuels au responsable client, déclenchés par les boutons
 * « Envoyer email » de la fiche de demande :
 * <ul>
 *   <li>proformas : fiche récapitulatif + factures proforma du mode PAR_SERVICE,
 *       sauf le débours qui est remplacé par sa facture validée ;</li>
 *   <li>facture validée : uniquement la facture concernée.</li>
 * </ul>
 * La société est destinataire principal et le responsable client est en copie.
 * Si la société n'a pas d'e-mail, le responsable devient destinataire principal.
 * Sans aucune adresse ou en cas d'échec SMTP, une erreur visible est remontée.
 */
@Service
public class ClientMailNotifier {

    private static final Logger log = LoggerFactory.getLogger(ClientMailNotifier.class);

    private final MailService mailService;
    private final DocumentGestionService documentService;
    private final FactureRepository factureRepository;
    private final FactureValideeRepository factureValideeRepository;
    private final DemandeServiceRepository demandeRepository;
    private final PaiementRepository paiementRepository;
    private final SocieteRepository societeRepository;
    private final ClientResponsableRepository responsableRepository;

    public ClientMailNotifier(MailService mailService,
                              DocumentGestionService documentService,
                              FactureRepository factureRepository,
                              FactureValideeRepository factureValideeRepository,
                              DemandeServiceRepository demandeRepository,
                              PaiementRepository paiementRepository,
                              SocieteRepository societeRepository,
                              ClientResponsableRepository responsableRepository) {
        this.mailService = mailService;
        this.documentService = documentService;
        this.factureRepository = factureRepository;
        this.factureValideeRepository = factureValideeRepository;
        this.demandeRepository = demandeRepository;
        this.paiementRepository = paiementRepository;
        this.societeRepository = societeRepository;
        this.responsableRepository = responsableRepository;
    }

    /**
     * Écouteur asynchrone : déclenché après la validation de la transaction de
     * paiement, sur le pool {@code mailTaskExecutor}. Le reçu et la facture sont
     * donc générés sur des données déjà commitées.
     */
    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onPaiementConfirme(PaiementMailEvent event) {
        DemandeService demande = demandeRepository.findById(event.demandeId()).orElse(null);
        if (demande == null) {
            log.warn("E-mail de paiement ignoré : demande {} introuvable", event.demandeId());
            return;
        }
        notifierPaiement(event.paiementId(), event.referencePaiement(),
                event.factureValideeId(), demande);
    }

    /**
     * Envoie la fiche récapitulatif et les factures proforma d'une demande.
     * En mode PAR_SERVICE, la ligne de débours n'est pas envoyée en proforma :
     * c'est sa facture validée qui est jointe. Tout est réuni dans un seul e-mail.
     */
    @Transactional(readOnly = true)
    public void envoyerProformas(Integer factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Facture", factureId)));
        DemandeService demande = facture.getDemande();
        if (demande == null) {
            throw new BusinessException("La facture n'est rattachée à aucune demande.");
        }
        Destinataires destinataires = destinataires(demande);

        List<PieceJointeEmail> pieces = new ArrayList<>();
        pieces.add(new PieceJointeEmail(
                "recapitulatif-" + MailService.nomFichier(demande.getReferenceDemande()) + ".pdf",
                documentService.recapitulatifDemande(demande.getId())));

        boolean parService = "PAR_SERVICE".equals(facture.getModeFacturation());
        if (parService) {
            for (Facture f : factureRepository.findByDemandeId(demande.getId())) {
                if (estDebours(f)) {
                    continue;
                }
                pieces.add(new PieceJointeEmail(
                        "proforma-" + MailService.nomFichier(f.getReferenceFacture()) + ".pdf",
                        documentService.factureProforma(f.getId())));
            }
            // Le débours est représenté par sa facture validée, pas par sa proforma.
            for (FactureValidee fv : factureValideeRepository
                    .findByDemandeIdOrderByIdAsc(demande.getId())) {
                if (estDebours(fv)) {
                    pieces.add(new PieceJointeEmail(
                            "facture-" + MailService.nomFichier(fv.getReferenceFactureValidee()) + ".pdf",
                            documentService.factureValideeDocument(fv.getId())));
                }
            }
        } else {
            pieces.add(new PieceJointeEmail(
                    "proforma-" + MailService.nomFichier(facture.getReferenceFacture()) + ".pdf",
                    documentService.factureProforma(factureId)));
        }

        String corps = "<p>Bonjour,</p>"
                + "<p>Vous trouverez en pièces jointes la fiche récapitulative et les factures "
                + "de votre demande <strong>" + demande.getReferenceDemande() + "</strong>.</p>"
                + "<p>Cordialement,<br/>L'équipe CABEX</p>";
        verifierEnvoi(mailService.envoyerDocumentsClient(
                destinataires.principal(),
                destinataires.copie(),
                "Documents de votre demande " + demande.getReferenceDemande() + " — CABEX",
                corps,
                pieces));
    }

    /** Envoie uniquement la facture validée sur laquelle l'utilisateur a cliqué. */
    @Transactional(readOnly = true)
    public void envoyerFactureValidee(Integer factureValideeId) {
        FactureValidee facture = factureValideeRepository.findById(factureValideeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Facture validée", factureValideeId)));
        DemandeService demande = facture.getDemande();
        if (demande == null) {
            throw new BusinessException("La facture validée n'est rattachée à aucune demande.");
        }
        Destinataires destinataires = destinataires(demande);
        String reference = facture.getReferenceFactureValidee();

        String corps = "<p>Bonjour,</p>"
                + "<p>Vous trouverez en pièce jointe la facture <strong>" + reference
                + "</strong> de votre demande " + demande.getReferenceDemande() + ".</p>"
                + "<p>Cordialement,<br/>L'équipe CABEX</p>";
        verifierEnvoi(mailService.envoyerDocumentsClient(
                destinataires.principal(),
                destinataires.copie(),
                "Facture " + reference + " — CABEX",
                corps,
                List.of(new PieceJointeEmail(
                        "facture-" + MailService.nomFichier(reference) + ".pdf",
                        documentService.factureValideeDocument(factureValideeId)))));
    }

    /**
     * Envoi automatique après enregistrement d'un paiement : facture validée liée
     * et reçu de paiement en pièces jointes. Non bloquant : toute erreur est
     * journalisée, le paiement est déjà enregistré.
     */
    public void notifierPaiement(Integer paiementId, String referencePaiement,
                                 Integer factureValideeId, DemandeService demande) {
        try {
            envoyerPaiement(paiementId, referencePaiement, factureValideeId, demande, false);
        } catch (RuntimeException e) {
            log.error("Échec de l'e-mail de paiement {} (paiement {})",
                    referencePaiement, paiementId, e);
        }
    }

    /** Envoi manuel depuis l'historique des paiements : les erreurs sont visibles. */
    @Transactional(readOnly = true)
    public void envoyerPaiement(Integer paiementId, String referencePaiement,
                                Integer factureValideeId, DemandeService demande) {
        envoyerPaiement(paiementId, referencePaiement, factureValideeId, demande, true);
    }

    private void envoyerPaiement(Integer paiementId, String referencePaiement,
                                 Integer factureValideeId, DemandeService demande,
                                 boolean erreurVisible) {
        Destinataires destinataires = destinataires(demande);
        List<PieceJointeEmail> pieces = List.of(
                new PieceJointeEmail(
                        "facture-" + MailService.nomFichier(String.valueOf(factureValideeId)) + ".pdf",
                        documentService.factureValideeDocument(factureValideeId)),
                new PieceJointeEmail(
                        "recu-" + MailService.nomFichier(referencePaiement) + ".pdf",
                        documentService.recuPaiement(paiementId)));
        String montant = montantRecu(paiementId);
        String corps = "<p>Bonjour,</p>"
                + "<p>Votre paiement <strong>" + referencePaiement
                + "</strong> a bien été enregistré.</p>"
                + "<p><strong>Montant reçu : " + montant + "</strong></p>"
                + "<p>Vous trouverez en pièces jointes la facture concernée et le reçu de paiement.</p>"
                + "<p>Cordialement,<br/>L'équipe CABEX</p>";
        boolean envoye = mailService.envoyerDocumentsClient(
                destinataires.principal(),
                destinataires.copie(),
                "Paiement " + referencePaiement + " — CABEX",
                corps,
                pieces);
        if (!envoye) {
            if (erreurVisible) {
                verifierEnvoi(false);
            } else {
                log.warn("E-mail de paiement non envoyé à {} pour {}",
                        destinataires.principal(), referencePaiement);
            }
        }
    }

    private String montantRecu(Integer paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId).orElse(null);
        if (paiement == null || paiement.getMontantPaye() == null) {
            return "non précisé";
        }
        return String.format(Locale.FRANCE, "%,.0f Ar", paiement.getMontantPaye());
    }

    /**
     * Envoie le PDF de facturation d'un client avec les mêmes filtres que
     * l'export PDF de l'onglet « Facturation ». Les filtres appliqués sont
     * rappelés dans le corps du message.
     */
    @Transactional(readOnly = true)
    public void envoyerFacturationClient(Integer societeId, LocalDate dateDebut, LocalDate dateFin,
                                         String ordre, String recherche) {
        Destinataires destinataires = destinatairesSociete(societeId);
        byte[] pdf = documentService.facturationClientPdf(
                societeId, dateDebut, dateFin, ordre, recherche);
        String corps = "<p>Bonjour,</p>"
                + "<p>Vous trouverez en pièce jointe le PDF de facturation de votre compte CABEX.</p>"
                + "<p><strong>Filtres appliqués :</strong><br/>" + filtresFacturation(
                        dateDebut, dateFin, ordre, recherche) + "</p>"
                + "<p>Cordialement,<br/>L'équipe CABEX</p>";
        verifierEnvoi(mailService.envoyerDocumentsClient(
                destinataires.principal(),
                destinataires.copie(),
                "Facturation CABEX — client " + societeId,
                corps,
                List.of(new PieceJointeEmail(
                        "facturation-client-" + societeId + ".pdf", pdf))));
    }

    private String filtresFacturation(LocalDate dateDebut, LocalDate dateFin,
                                      String ordre, String recherche) {
        List<String> lignes = new ArrayList<>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (dateDebut != null || dateFin != null) {
            String debut = dateDebut != null ? dateDebut.format(format) : "début";
            String fin = dateFin != null ? dateFin.format(format) : "aujourd'hui";
            lignes.add("Période : du " + debut + " au " + fin);
        } else {
            lignes.add("Période : toutes les dates");
        }
        if (recherche != null && !recherche.isBlank()) {
            lignes.add("Recherche : « " + recherche.trim() + " »");
        }
        lignes.add("Tri : " + ("asc".equalsIgnoreCase(ordre)
                ? "du plus ancien au plus récent"
                : "du plus récent au plus ancien"));
        if (dateDebut == null && dateFin == null && (recherche == null || recherche.isBlank())) {
            lignes.add("Aucun autre filtre : toutes les factures sont incluses.");
        }
        return String.join("<br/>", lignes);
    }

    /** Société en destinataire principal, responsable actif en copie. */
    private Destinataires destinatairesSociete(Integer societeId) {
        Societe societe = societeRepository.findById(societeId).orElse(null);
        String emailSociete = societe != null && societe.getEmail() != null
                && !societe.getEmail().isBlank() ? societe.getEmail().trim() : null;
        String emailResponsable = responsableRepository.findBySocieteIdAndActifTrue(societeId)
                .stream()
                .map(ClientResponsable::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .findFirst()
                .map(String::trim)
                .orElse(null);
        if (emailSociete == null && emailResponsable == null) {
            throw new BusinessException(
                    "Ni la société ni un responsable actif n'a d'adresse e-mail. "
                            + "Renseignez une adresse avant l'envoi.");
        }
        return emailSociete != null
                ? new Destinataires(emailSociete, emailResponsable)
                : new Destinataires(emailResponsable, null);
    }

    /**
     * Société en destinataire principal, responsable en copie. Si la société n'a
     * pas d'adresse, le responsable devient destinataire principal (sans copie).
     */
    private Destinataires destinataires(DemandeService demande) {
        String emailSociete = null;
        Societe societe = demande.getSociete();
        if (societe != null && societe.getEmail() != null && !societe.getEmail().isBlank()) {
            emailSociete = societe.getEmail().trim();
        }
        String emailResponsable = null;
        ClientResponsable responsable = demande.getClientResponsable();
        if (responsable != null && responsable.getEmail() != null
                && !responsable.getEmail().isBlank()) {
            emailResponsable = responsable.getEmail().trim();
        }

        if (emailSociete == null && emailResponsable == null) {
            throw new BusinessException(
                    "Ni la société ni le responsable client n'a d'adresse e-mail. "
                            + "Renseignez une adresse sur la fiche client avant l'envoi.");
        }
        return emailSociete != null
                ? new Destinataires(emailSociete, emailResponsable)
                : new Destinataires(emailResponsable, null);
    }

    private record Destinataires(String principal, String copie) {
    }

    private boolean estDebours(Facture facture) {
        if (facture.getLignesProforma() != null && facture.getLignesProforma().stream()
                .anyMatch(l -> "DEBOURS".equalsIgnoreCase(l.getCategorie()))) {
            return true;
        }
        return facture.getLignes() != null && facture.getLignes().stream()
                .anyMatch(l -> "DEBOURS".equalsIgnoreCase(l.getCategorie()));
    }

    private boolean estDebours(FactureValidee facture) {
        return facture.getMois() == null && facture.getFactureProforma() == null;
    }

    private void verifierEnvoi(boolean envoye) {
        if (!envoye) {
            throw new BusinessException(
                    "L'e-mail n'a pas pu être envoyé. Vérifiez la configuration SMTP "
                            + "(identifiants Gmail) et réessayez.");
        }
    }
}
