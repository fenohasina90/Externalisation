package com.cabex.backend.auth.service;

import com.cabex.backend.audit.action.AuditActions;
import com.cabex.backend.audit.service.AuditService;
import com.cabex.backend.auth.dto.ForgotPasswordRequest;
import com.cabex.backend.auth.dto.MotDePasseTemporaireResponse;
import com.cabex.backend.auth.dto.PasswordResetRequestResponse;
import com.cabex.backend.configuration.repository.ConfigurationRepository;
import com.cabex.backend.exception.BusinessException;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.mail.MailService;
import com.cabex.backend.notification.service.NotificationService;
import com.cabex.backend.security.SecurityUtils;
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
 * Workflow de réinitialisation de mot de passe validé par un administrateur,
 * fondé sur la remise d'un mot de passe temporaire à l'utilisateur :
 * 1. l'utilisateur demande la réinitialisation (demande EN_ATTENTE) ;
 * 2. les administrateurs sont notifiés et approuvent la demande (APPROUVEE) ;
 * 3. l'administrateur génère un mot de passe temporaire, communiqué une seule
 *    fois puis envoyé par e-mail si l'envoi est activé ;
 * 4. à sa prochaine connexion, l'utilisateur doit changer ce mot de passe, ce
 *    qui clôture la demande (TERMINEE).
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
    private final NotificationService notificationService;
    private final MailService mailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(DemandeReinitialisationRepository demandeRepository,
                                UtilisateurRepository utilisateurRepository,
                                ConfigurationRepository configurationRepository,
                                NotificationService notificationService,
                                MailService mailService,
                                AuditService auditService,
                                PasswordEncoder passwordEncoder) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.configurationRepository = configurationRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void demanderReinitialisation(ForgotPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email().trim()).orElse(null);
        if (utilisateur == null || !Boolean.TRUE.equals(utilisateur.getActif())) {
            log.info("Demande de réinitialisation ignorée pour l'email : {}", request.email());
            return;
        }

        if (demandeRepository.existsByUtilisateurIdAndStatutIn(utilisateur.getId(),
                List.of(DemandeReinitialisationStatut.EN_ATTENTE,
                        DemandeReinitialisationStatut.APPROUVEE))) {
            log.info("Une demande de réinitialisation est déjà active pour {}", utilisateur.getUsername());
            return;
        }

        DemandeReinitialisation demande = DemandeReinitialisation.builder()
                .utilisateur(utilisateur)
                .statut(DemandeReinitialisationStatut.EN_ATTENTE)
                .build();
        demande = demandeRepository.save(demande);

        notificationService.notifierDemandeReinitialisation(utilisateur, demande.getId());
        auditService.log(AuditActions.RESET_REQUEST, "AUTH", "DemandeReinitialisation",
                demande.getId(), null, DemandeReinitialisationStatut.EN_ATTENTE);
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

    @Transactional
    public PasswordResetRequestResponse approuver(Long id) {
        DemandeReinitialisation demande = getDemande(id);
        exigerEnAttente(demande);
        verifierPasSoiMeme(demande);

        demande.setStatut(DemandeReinitialisationStatut.APPROUVEE);
        demande.setTraitePar(currentUtilisateur());
        demande.setDateTraitement(LocalDateTime.now());
        demande = demandeRepository.save(demande);

        auditService.log(AuditActions.RESET_APPROVE, "AUTH", "DemandeReinitialisation",
                demande.getId(), DemandeReinitialisationStatut.EN_ATTENTE,
                DemandeReinitialisationStatut.APPROUVEE);
        return toResponse(demande);
    }

    @Transactional
    public PasswordResetRequestResponse refuser(Long id, String motif) {
        DemandeReinitialisation demande = getDemande(id);
        exigerEnAttente(demande);
        verifierPasSoiMeme(demande);

        demande.setStatut(DemandeReinitialisationStatut.REFUSEE);
        demande.setMotifRefus(motif);
        demande.setTraitePar(currentUtilisateur());
        demande.setDateTraitement(LocalDateTime.now());
        demande = demandeRepository.save(demande);

        auditService.log(AuditActions.RESET_REJECT, "AUTH", "DemandeReinitialisation",
                demande.getId(), DemandeReinitialisationStatut.EN_ATTENTE,
                DemandeReinitialisationStatut.REFUSEE);
        return toResponse(demande);
    }

    @Transactional
    public MotDePasseTemporaireResponse reinitialiser(Long id) {
        DemandeReinitialisation demande = getDemande(id);
        if (!DemandeReinitialisationStatut.APPROUVEE.equals(demande.getStatut())) {
            throw new BusinessException(
                    "La demande doit d'abord être approuvée avant de générer un mot de passe temporaire");
        }
        verifierPasSoiMeme(demande);

        int validiteMinutes = dureeMdpTemporaire();
        String motDePasse = genererMotDePasseTemporaire();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(validiteMinutes);

        Utilisateur utilisateur = demande.getUtilisateur();
        utilisateur.setMdpHash(passwordEncoder.encode(motDePasse));
        utilisateur.setDoitChangerMdp(true);
        utilisateur.setDateExpirationMdpTemporaire(expiration);
        utilisateurRepository.save(utilisateur);

        demande.setGenerePar(currentUtilisateur());
        demande.setDateGenerationMdp(LocalDateTime.now());
        demande.setDateExpirationMdp(expiration);
        demande = demandeRepository.save(demande);

        mailService.envoyerMotDePasseTemporaire(
                utilisateur.getEmail(),
                utilisateur.getPrenom() + " " + utilisateur.getNom(),
                motDePasse,
                validiteMinutes);

        auditService.log(AuditActions.RESET_SEND, "AUTH", "DemandeReinitialisation",
                demande.getId(), null, "Mot de passe temporaire généré");

        return new MotDePasseTemporaireResponse(
                motDePasse,
                expiration,
                "Mot de passe temporaire généré. Communiquez-le à l'utilisateur : il devra le changer "
                        + "à sa prochaine connexion.",
                toResponse(demande));
    }

    /**
     * Clôture les demandes approuvées de l'utilisateur après le changement effectif
     * de son mot de passe. Appelée par {@link AuthService}.
     */
    @Transactional
    public void terminerDemandesApresChangement(Utilisateur utilisateur) {
        List<DemandeReinitialisation> demandes = demandeRepository.findByUtilisateurIdAndStatutIn(
                utilisateur.getId(), List.of(DemandeReinitialisationStatut.APPROUVEE));
        for (DemandeReinitialisation demande : demandes) {
            demande.setStatut(DemandeReinitialisationStatut.TERMINEE);
            demandeRepository.save(demande);
            auditService.log(AuditActions.RESET_DONE, "AUTH", "DemandeReinitialisation",
                    demande.getId(), DemandeReinitialisationStatut.APPROUVEE,
                    DemandeReinitialisationStatut.TERMINEE);
        }
    }

    /**
     * Tâche planifiée : expire les demandes restées en attente trop longtemps et
     * les mots de passe temporaires non changés avant leur échéance.
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

        for (DemandeReinitialisation demande : demandeRepository
                .findByStatutAndDateExpirationMdpBefore(DemandeReinitialisationStatut.APPROUVEE, maintenant)) {
            demande.setStatut(DemandeReinitialisationStatut.EXPIREE);
            Utilisateur utilisateur = demande.getUtilisateur();
            if (utilisateur != null && Boolean.TRUE.equals(utilisateur.getDoitChangerMdp())
                    && utilisateur.getDateExpirationMdpTemporaire() != null
                    && !utilisateur.getDateExpirationMdpTemporaire().isAfter(maintenant)) {
                utilisateur.setDoitChangerMdp(false);
                utilisateur.setDateExpirationMdpTemporaire(null);
                utilisateurRepository.save(utilisateur);
            }
            demandeRepository.save(demande);
        }
    }

    private DemandeReinitialisation getDemande(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.introuvable("Demande de réinitialisation", id)));
    }

    private void exigerEnAttente(DemandeReinitialisation demande) {
        if (!DemandeReinitialisationStatut.EN_ATTENTE.equals(demande.getStatut())) {
            throw new BusinessException("Cette demande de réinitialisation a déjà été traitée");
        }
    }

    private void verifierPasSoiMeme(DemandeReinitialisation demande) {
        Utilisateur courant = currentUtilisateur();
        if (courant != null && demande.getUtilisateur() != null
                && courant.getId().equals(demande.getUtilisateur().getId())) {
            throw new BusinessException(
                    "Vous ne pouvez pas traiter votre propre demande de réinitialisation");
        }
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

    private Utilisateur currentUtilisateur() {
        try {
            return utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        } catch (Exception e) {
            return null;
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