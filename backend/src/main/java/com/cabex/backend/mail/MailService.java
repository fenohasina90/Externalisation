package com.cabex.backend.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
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
        String sujet = "Votre mot de passe temporaire";
        String corps = "<p>Bonjour " + escape(nomComplet) + ",</p>"
                + "<p>Votre demande de réinitialisation de mot de passe a été approuvée.</p>"
                + "<p>Mot de passe temporaire : <strong>" + escape(motDePasse) + "</strong></p>"
                + "<p>Ce mot de passe est valable " + dureeMinutes + " minutes. "
                + "Vous devrez le changer dès votre prochaine connexion.</p>"
                + "<p>Si vous n'êtes pas à l'origine de cette demande, contactez votre administrateur.</p>";

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!enabled || sender == null) {
            log.info("[MAIL désactivé] Mot de passe temporaire pour {} : {}", destinataire, motDePasse);
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corps, true);
            sender.send(message);
            log.info("Email de réinitialisation envoyé à {}", destinataire);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de l'envoi de l'email à " + destinataire, e);
        }
    }

    private String escape(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}