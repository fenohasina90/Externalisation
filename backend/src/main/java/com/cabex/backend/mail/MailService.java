package com.cabex.backend.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envoi des e-mails de réinitialisation via {@link JavaMailSender}.
 * Le mot de passe temporaire n'est jamais écrit dans les logs : l'e-mail est
 * le seul canal de remise à l'utilisateur.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@cabex.mg}")
    private String from;

    @Value("${app.mail.from-name:CABEX}")
    private String fromName;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void envoyerMotDePasseTemporaire(String destinataire, String nomComplet,
                                            String motDePasse, int dureeMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!enabled || sender == null) {
            log.error("Envoi d'e-mail indisponible (app.mail.enabled={}, JavaMailSender={}) pour {}",
                    enabled, sender != null, destinataire);
            throw new IllegalStateException("L'envoi d'e-mails est indisponible. "
                    + "Vérifiez la configuration SMTP (app.mail.enabled et les identifiants Gmail).");
        }

        String sujet = "Votre mot de passe temporaire CABEX";
        String corps = "<p>Bonjour " + escape(nomComplet) + ",</p>"
                + "<p>Vous (ou un administrateur) avez demandé la réinitialisation du mot de passe "
                + "de votre compte CABEX.</p>"
                + "<p>Votre mot de passe temporaire est : <strong>" + escape(motDePasse) + "</strong></p>"
                + "<p>Ce mot de passe est valable " + dureeMinutes + " minutes. "
                + "Vous devrez le changer dès votre prochaine connexion.</p>"
                + "<p>Si vous n'êtes pas à l'origine de cette demande, contactez votre administrateur.</p>";

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corps, true);
            sender.send(message);
            log.info("E-mail de réinitialisation envoyé à {}", destinataire);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'e-mail de réinitialisation à {}", destinataire, e);
            throw new IllegalStateException("Échec de l'envoi de l'e-mail à " + destinataire, e);
        }
    }

    /** Pièce jointe PDF d'un e-mail destiné au client. */
    public record PieceJointeEmail(String nomFichier, byte[] contenu) {
    }

    /**
     * Envoie un e-mail au responsable client avec les documents PDF joints.
     * Utilisé par les actions manuelles « Envoyer email » des fiches de factures.
     */
    public boolean envoyerDocumentsClient(String destinataire, String copie, String objet,
                                          String corpsHtml, List<PieceJointeEmail> pieces) {
        return envoyerAvecPiecesJointes(destinataire, copie, objet, corpsHtml, pieces);
    }

    private boolean envoyerAvecPiecesJointes(String destinataire, String copie, String sujet,
                                             String corpsHtml, List<PieceJointeEmail> pieces) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!enabled || sender == null) {
            log.warn("E-mail client non envoyé à {} : envoi désactivé ou JavaMailSender indisponible",
                    destinataire);
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(destinataire);
            if (copie != null && !copie.isBlank()) {
                helper.addCc(copie.trim());
            }
            helper.setSubject(sujet);
            helper.setText(corpsHtml, true);
            int jointes = 0;
            for (PieceJointeEmail piece : pieces) {
                if (piece == null || piece.contenu() == null || piece.contenu().length == 0) {
                    continue;
                }
                helper.addAttachment(piece.nomFichier(), new ByteArrayResource(piece.contenu()));
                jointes++;
            }
            sender.send(message);
            log.info("E-mail client envoyé à {} ({} pièce(s) jointe(s))", destinataire, jointes);
            return true;
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'e-mail client à {}", destinataire, e);
            return false;
        }
    }

    public static String nomFichier(String reference) {
        if (reference == null) {
            return "document";
        }
        return reference.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private String escape(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
