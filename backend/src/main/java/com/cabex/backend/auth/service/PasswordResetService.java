package com.cabex.backend.auth.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.auth.dto.ForgotPasswordRequest;
import com.cabex.backend.auth.dto.PasswordResetRequestResponse;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.mail.MailService;
import com.cabex.backend.utilisateur.entity.DemandeReinitialisation;
import com.cabex.backend.utilisateur.entity.DemandeReinitialisationStatut;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.DemandeReinitialisationRepository;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Réinitialisation de mot de passe sans intervention d'un administrateur :
 * 1. l'utilisateur soumet son adresse e-mail ({@code POST /api/auth/forgot-password}) ;
 * 2. un mot de passe temporaire est généré immédiatement, haché (BCrypt) et envoyé
 *    directement à l'utilisateur par e-mail (JavaMailSender) ;
 * 3. la demande est conservée avec le statut {@code ENVOYEE} et reste consultable
 *    par les administrateurs ;
 * 4. à sa prochaine connexion, l'utilisateur doit remplacer ce mot de passe, ce qui
 *    clôture la demande ({@code TERMINEE}).
 *
 * Les statuts {@code APPROUVEE} et {@code REFUSEE} ne sont plus produits : ils ne
 * subsistent que dans les données historiques. Les actions « approuver » et
 * « refuser » ont été supprimées.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int DUREE_DEMANDE_MINUTES_DEFAUT = 1440;
    private static final int DUREE_MDP_TEMPORAIRE_MINUTES_DEFAUT = 1440;

    private static final String CLE_DUREE_DEMANDE = "DUREE_VALIDITE_DEMANDE_RESET";
    private static final String CLE_DUREE_MDP_TEMPORAIRE = "DUREE_VALIDITE_MDP_TEMPORAIRE";

    private static final String CARACTERES_MDP =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LONGUEUR_MDP_TEMPORAIRE = 10;

    private final DemandeReinitialisationRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ConfigurationRepository configurationRepository;
    private final MailService mailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(DemandeReinitialisationRepository demandeRepository,
                                UtilisateurRepository utilisateurRepository,
                                ConfigurationRepository configurationRepository,
                                MailService mailService,
                                AuditService auditService,
                                PasswordEncoder passwordEncoder) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.configurationRepository = configurationRepository;
        this.mailService = mailService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Génère le mot de passe temporaire et l'envoie directement à l'utilisateur.
     * La réponse HTTP reste volontairement générique (que l'e-mail existe ou non).
     * Tout échec d'envoi fait échouer la transaction : ni mot de passe ni demande
     * ne sont conservés en base.
     */
    @Transactional
    public void demanderReinitialisation(ForgotPasswordRequest request) {
        String email = request.email() == null ? "" : request.email().trim();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        if (utilisateur == null || !Boolean.TRUE.equals(utilisateur.getActif())) {
            log.info("Demande de réinitialisation ignorée pour l'email : {}", email);
            return;
        }

        if (demandeRepository.existsByUtilisateurIdAndStatutIn(utilisateur.getId(),
                List.of(DemandeReinitialisationStatut.EN_ATTENTE,
                        DemandeReinitialisationStatut.APPROUVEE,
                        DemandeReinitialisationStatut.ENVOYEE))) {
            log.info("Une demande de réinitialisation est déjà active pour {}", utilisateur.getUsername());
            return;
        }

        int validiteMinutes = dureeMdpTemporaire();
        String motDePasse = genererMotDePasseTemporaire();
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime expiration = maintenant.plusMinutes(validiteMinutes);

        DemandeReinitialisation demande = DemandeReinitialisation.builder()
                .utilisateur(utilisateur)
                .statut(DemandeReinitialisationStatut.ENVOYEE)
                .dateTraitement(maintenant)
                .dateGenerationMdp(maintenant)
                .dateExpirationMdp(expiration)
                .build();
        demande = demandeRepository.save(demande);

        utilisateur.setMdpHash(passwordEncoder.encode(motDePasse));
        utilisateur.setDoitChangerMdp(true);
        utilisateur.setDateExpirationMdpTemporaire(expiration);
        utilisateurRepository.save(utilisateur);

        try {
            mailService.envoyerMotDePasseTemporaire(
                    utilisateur.getEmail(),
                    utilisateur.getPrenom() + " " + utilisateur.getNom(),
                    motDePasse,
                    validiteMinutes);
        } catch (RuntimeException e) {
            log.error("Échec de l'envoi de l'e-mail de réinitialisation à {}", utilisateur.getEmail(), e);
            throw new IllegalStateException(
                    "Impossible d'envoyer l'e-mail de réinitialisation. Veuillez réessayer plus tard.");
        }

        auditService.log(AuditActions.RESET_REQUEST, "AUTH", "DemandeReinitialisation",
                demande.getId(), null, DemandeReinitialisationStatut.ENVOYEE);
        auditService.log(AuditActions.RESET_SEND, "AUTH", "DemandeReinitialisation",
                demande.getId(), null, "Mot de passe temporaire envoyé par e-mail");
    }

    @Transactional(readOnly = true)
    public Page<PasswordResetRequestResponse> lister(String statut, Pageable pageable) {
        Page<DemandeReinitialisation> page = (statut == null || statut.isBlank())
                ? demandeRepository.findAllByOrderByCreatedAtDesc(pageable)
                : demandeRepository.findByStatutOrderByCreatedAtDesc(statut.trim().toUpperCase(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PasswordResetRequestResponse detail(Long id) {
        return toResponse(getDemande(id));
    }

    /**
     * Clôture les demandes envoyées (ou approuvées dans l'ancien flux) après le
     * changement effectif du mot de passe. Appelée par {@link AuthService}.
     */
    @Transactional
    public void terminerDemandesApresChangement(Utilisateur utilisateur) {
        List<DemandeReinitialisation> demandes = demandeRepository.findByUtilisateurIdAndStatutIn(
                utilisateur.getId(),
                List.of(DemandeReinitialisationStatut.ENVOYEE,
                        DemandeReinitialisationStatut.APPROUVEE));
        for (DemandeReinitialisation demande : demandes) {
            demande.setStatut(DemandeReinitialisationStatut.TERMINEE);
            demandeRepository.save(demande);
            auditService.log(AuditActions.RESET_DONE, "AUTH", "DemandeReinitialisation",
                    demande.getId(), DemandeReinitialisationStatut.ENVOYEE,
                    DemandeReinitialisationStatut.TERMINEE);
        }
    }

    /**
     * Tâche planifiée : expire les anciennes demandes et les mots de passe
     * temporaires non changés avant leur échéance. Le mot de passe temporaire
     * expiré reste marqué {@code doit_changer_mdp} : {@code AuthService} refusera
     * la connexion avec le message {@code MDP_TEMPORAIRE_EXPIRE}, obligeant
     * l'utilisateur à en demander un nouveau.
     */
    @Transactional
    public void expirerDemandes() {
        LocalDateTime maintenant = LocalDateTime.now();

        LocalDateTime seuil = maintenant.minusMinutes(dureeDemande());
        for (DemandeReinitialisation demande :
                demandeRepository.findByStatutAndCreatedAtBefore(
                        DemandeReinitialisationStatut.EN_ATTENTE, seuil)) {
            demande.setStatut(DemandeReinitialisationStatut.EXPIREE);
            demande.setDateTraitement(maintenant);
            demandeRepository.save(demande);
        }

        for (String statut : List.of(DemandeReinitialisationStatut.ENVOYEE,
                DemandeReinitialisationStatut.APPROUVEE)) {
            for (DemandeReinitialisation demande : demandeRepository
                    .findByStatutAndDateExpirationMdpBefore(statut, maintenant)) {
                demande.setStatut(DemandeReinitialisationStatut.EXPIREE);
                demande.setDateTraitement(maintenant);
                demandeRepository.save(demande);
            }
        }
    }

    private DemandeReinitialisation getDemande(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande de réinitialisation", id)));
    }

    private PasswordResetRequestResponse toResponse(DemandeReinitialisation demande) {
        Utilisateur utilisateur = demande.getUtilisateur();
        Utilisateur traitePar = demande.getTraitePar();
        Utilisateur generePar = demande.getGenerePar();
        return new PasswordResetRequestResponse(
                demande.getId(),
                utilisateur != null ? utilisateur.getId() : null,
                utilisateur != null ? utilisateur.getPrenom() + " " + utilisateur.getNom() : null,
                utilisateur != null ? utilisateur.getUsername() : null,
                utilisateur != null ? utilisateur.getEmail() : null,
                demande.getStatut(),
                demande.getMotifRefus(),
                demande.getCreatedAt(),
                demande.getDateTraitement(),
                traitePar != null ? traitePar.getUsername() : null,
                demande.getDateGenerationMdp(),
                demande.getDateExpirationMdp(),
                generePar != null ? generePar.getUsername() : null);
    }

    private int dureeDemande() {
        return lireDuree(CLE_DUREE_DEMANDE, DUREE_DEMANDE_MINUTES_DEFAUT);
    }

    private int dureeMdpTemporaire() {
        return lireDuree(CLE_DUREE_MDP_TEMPORAIRE, DUREE_MDP_TEMPORAIRE_MINUTES_DEFAUT);
    }

    private int lireDuree(String cle, int defaut) {
        return configurationRepository.findByCle(cle)
                .map(config -> parseEntier(config.getValeur(), defaut))
                .orElse(defaut);
    }

    private int parseEntier(String valeur, int defaut) {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    private String genererMotDePasseTemporaire() {
        StringBuilder sb = new StringBuilder(LONGUEUR_MDP_TEMPORAIRE);
        for (int i = 0; i < LONGUEUR_MDP_TEMPORAIRE; i++) {
            sb.append(CARACTERES_MDP.charAt(secureRandom.nextInt(CARACTERES_MDP.length())));
        }
        return sb.toString();
    }
}
