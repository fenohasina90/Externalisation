package com.cabex.backend.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    private String escape(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
