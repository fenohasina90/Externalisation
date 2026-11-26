package com.cabex.backend.mail;

import com.cabex.backend.client.entity.ClientResponsable;
import com.cabex.backend.client.entity.Societe;
import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.demande.repository.DemandeServiceRepository;
import com.cabex.backend.document.service.DocumentGestionService;
import com.cabex.backend.facture.entity.Facture;
import com.cabex.backend.facture.entity.FactureValidee;
import com.cabex.backend.facture.repository.FactureRepository;
import com.cabex.backend.facture.repository.FactureValideeRepository;
import com.cabex.backend.mail.MailService.PieceJointeEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifications e-mail destinées au client : envoi de la fiche récapitulative et
 * des factures proforma à la création d'une demande, puis de la facture validée
 * à chaque validation.
 *
 * <p>L'envoi est volontairement non bloquant : un problème SMTP, une adresse
 * absente ou un PDF indisponible sont journalisés sans annuler l'opération
 * métier (création de demande ou validation de facture).</p>
 */
@Service
public class ClientMailNotifier {

    private static final Logger log = LoggerFactory.getLogger(ClientMailNotifier.class);

    private final MailService mailService;
    private final DocumentGestionService documentService;
    private final DemandeServiceRepository demandeRepository;
    private final FactureRepository factureRepository;
    private final FactureValideeRepository factureValideeRepository;

    public ClientMailNotifier(MailService mailService,
                              DocumentGestionService documentService,
                              DemandeServiceRepository demandeRepository,
                              FactureRepository factureRepository,
                              FactureValideeRepository factureValideeRepository) {
        this.mailService = mailService;
        this.documentService = documentService;
        this.demandeRepository = demandeRepository;
        this.factureRepository = factureRepository;
        this.factureValideeRepository = factureValideeRepository;
    }

    /** Appelé après la création complète d'une demande (factures proforma générées). */
    public void notifierDemandeCreee(Integer demandeId) {
        try {
            DemandeService demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande == null) {
                log.warn("E-mail client ignoré : demande {} introuvable", demandeId);
                return;
            }
            String destinataire = destinataire(demande);
            if (destinataire == null) {
                log.info("E-mail client ignoré pour la demande {} : aucune adresse e-mail renseignée",
                        demande.getReferenceDemande());
                return;
            }

            byte[] recapitulatif = documentService.recapitulatifDemande(demandeId);
            List<PieceJointeEmail> proformas = new ArrayList<>();
            for (Facture facture : factureRepository.findByDemandeId(demandeId)) {
                byte[] pdf = documentService.factureProforma(facture.getId());
                proformas.add(new PieceJointeEmail(
                        "proforma-" + facture.getReferenceFacture().replaceAll("[^A-Za-z0-9._-]", "-")
                                + ".pdf",
                        pdf));
            }

            boolean envoye = mailService.envoyerDemandeClient(
                    destinataire, demande.getReferenceDemande(), recapitulatif, proformas);
            logInfoEnvoi(envoye, destinataire, demande.getReferenceDemande());
        } catch (RuntimeException e) {
            log.error("Échec de la préparation de l'e-mail client pour la demande {}", demandeId, e);
        }
    }

    /** Appelé après la validation d'une proforma, avec l'identifiant de la facture validée. */
    public void notifierFactureValidee(Integer factureValideeId) {
        if (factureValideeId == null) {
            return;
        }
        try {
            FactureValidee facture = factureValideeRepository.findById(factureValideeId).orElse(null);
            if (facture == null || facture.getDemande() == null) {
                log.warn("E-mail client ignoré : facture validée {} introuvable", factureValideeId);
                return;
            }
            DemandeService demande = facture.getDemande();
            String destinataire = destinataire(demande);
            if (destinataire == null) {
                log.info("E-mail client ignoré pour {} : aucune adresse e-mail renseignée",
                        facture.getReferenceFactureValidee());
                return;
            }

            byte[] pdf = documentService.factureValideeDocument(factureValideeId);
            boolean envoye = mailService.envoyerFactureValideeClient(
                    destinataire,
                    demande.getReferenceDemande(),
                    facture.getReferenceFactureValidee(),
                    pdf);
            logInfoEnvoi(envoye, destinataire, facture.getReferenceFactureValidee());
        } catch (RuntimeException e) {
            log.error("Échec de la préparation de l'e-mail client pour la facture validée {}",
                    factureValideeId, e);
        }
    }

    /** Responsable client en priorité, puis société ; {@code null} si aucune adresse. */
    private String destinataire(DemandeService demande) {
        ClientResponsable responsable = demande.getClientResponsable();
        if (responsable != null && estRenseigne(responsable.getEmail())) {
            return responsable.getEmail().trim();
        }
        Societe societe = demande.getSociete();
        if (societe != null && estRenseigne(societe.getEmail())) {
            return societe.getEmail().trim();
        }
        return null;
    }

    private boolean estRenseigne(String email) {
        return email != null && !email.isBlank();
    }

    private void logInfoEnvoi(boolean envoye, String destinataire, String reference) {
        if (!envoye) {
            log.warn("E-mail client non envoyé à {} pour {}", destinataire, reference);
        }
    }
}
