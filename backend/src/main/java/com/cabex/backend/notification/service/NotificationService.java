package com.cabex.backend.notification.service;

import com.cabex.backend.demande.entity.DemandeService;
import com.cabex.backend.exception.ErrorMessages;
import com.cabex.backend.exception.ResourceNotFoundException;
import com.cabex.backend.notification.dto.NotificationResponse;
import com.cabex.backend.notification.dto.NotificationsNonLuesResponse;
import com.cabex.backend.notification.entity.Notification;
import com.cabex.backend.notification.mapper.NotificationMapper;
import com.cabex.backend.notification.repository.NotificationRepository;
import com.cabex.backend.paiement.entity.Paiement;
import com.cabex.backend.security.SecurityUtils;
import com.cabex.backend.utilisateur.entity.Utilisateur;
import com.cabex.backend.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
public class NotificationService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_ACCUEIL = "ACCUEIL";
    private static final String ROLE_ECONOMAT = "ECONOMAT";

    private static final List<String> TYPES_DEMANDE = List.of(
            "NOUVELLE_DEMANDE", "CHANGEMENT_STATUT");

    private static final List<String> TYPES_PAIEMENT = List.of(
            "PAIEMENT_ENREGISTRE", "ECHEANCE_PROCHE", "ECHEANCE_RETARD");

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Pageable pageable) {
        List<String> types = typesAutorises();
        return (types == null
                ? notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(
                        SecurityUtils.currentUserId(), pageable)
                : notificationRepository.findByUtilisateurIdAndTypeNotificationInOrderByDateCreationDesc(
                        SecurityUtils.currentUserId(), types, pageable))
                .map(NotificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationsNonLuesResponse nonLues() {
        Integer userId = SecurityUtils.currentUserId();
        List<String> types = typesAutorises();
        List<NotificationResponse> notifications = (types == null
                ? notificationRepository.findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(
                        userId, PageRequest.of(0, 50))
                : notificationRepository.findByUtilisateurIdAndLuFalseAndTypeNotificationInOrderByDateCreationDesc(
                        userId, types, PageRequest.of(0, 50)))
                .getContent()
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
        long total = types == null
                ? notificationRepository.countByUtilisateurIdAndLuFalse(userId)
                : notificationRepository.countByUtilisateurIdAndLuFalseAndTypeNotificationIn(userId, types);
        return new NotificationsNonLuesResponse(notifications, total);
    }

    @Transactional
    public NotificationResponse marquerLu(Long id) {
        Notification notification = getNotificationAutorisee(id);
        if (!notification.getLu()) {
            notification.setLu(true);
            notification.setDateLecture(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationMapper.toResponse(notification);
    }

    @Transactional
    public long toutLire() {
        Integer userId = SecurityUtils.currentUserId();
        List<String> types = typesAutorises();
        if (types == null) {
            return notificationRepository.marquerToutLu(userId, LocalDateTime.now());
        }
        return notificationRepository.marquerToutLuDansTypes(userId, types, LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        Notification notification = getNotificationAutorisee(id);
        notificationRepository.delete(notification);
    }

    private List<String> typesAutorises() {
        String role = SecurityUtils.currentRoleName();
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(role) || ROLE_ADMIN.equalsIgnoreCase(role)) {
            return null;
        }
        if (ROLE_ACCUEIL.equalsIgnoreCase(role)) {
            return TYPES_DEMANDE;
        }
        if (ROLE_ECONOMAT.equalsIgnoreCase(role)) {
            return TYPES_PAIEMENT;
        }
        return List.of();
    }

    public void notifierNouvelleDemande(DemandeService demande) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourRoles(
                "NOUVELLE_DEMANDE",
                "Nouvelle demande",
                "📋 Nouvelle demande " + nullSafe(demande.getReferenceDemande())
                        + " enregistrée pour " + societe
                        + (demande.getCommercial() != null
                        ? " par le commercial " + demande.getCommercial().getNomCommercial() : ""),
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null,
                ROLE_SUPER_ADMIN, ROLE_ACCUEIL);
    }

    public void notifierChangementStatut(DemandeService demande, String nouveauStatut) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourRoles(
                "CHANGEMENT_STATUT",
                "Changement de statut",
                "🔄 La demande " + nullSafe(demande.getReferenceDemande()) + " de " + societe
                        + " est passée au statut " + nouveauStatut + ".",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null,
                ROLE_SUPER_ADMIN, ROLE_ACCUEIL);
    }

    public void notifierPaiementEnregistre(Paiement paiement) {
        String societe = paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                ? paiement.getDemande().getSociete().getNomSociete() : "client";
        creerPourRoles(
                "PAIEMENT_ENREGISTRE",
                "Paiement enregistré",
                "💳 Paiement " + nullSafe(paiement.getReferencePaiement()) + " de "
                        + montant(paiement.getMontantPaye()) + " enregistré pour " + societe + ".",
                "PAIEMENT", paiement.getId() != null ? paiement.getId().longValue() : null,
                ROLE_SUPER_ADMIN, ROLE_ECONOMAT);
    }

    public void notifierEcheanceProche(DemandeService demande, int jours) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourRoles(
                "ECHEANCE_PROCHE",
                "Échéance proche",
                "⚠️ L'échéance de la société " + societe
                        + " arrive à échéance dans " + jours + " jour" + (jours > 1 ? "s" : "") + ".",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null,
                ROLE_SUPER_ADMIN, ROLE_ECONOMAT);
    }

    public void notifierEcheanceEnRetard(DemandeService demande) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourRoles(
                "ECHEANCE_RETARD",
                "Paiement en retard",
                "⛔ L'échéance de la société " + societe + " est en retard.",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null,
                ROLE_SUPER_ADMIN, ROLE_ECONOMAT);
    }

    public void notifierDemandeReinitialisation(Utilisateur utilisateur, Long demandeId) {
        String nomComplet = utilisateur.getPrenom() + " " + utilisateur.getNom();
        creerPourRoles(
                "DEMANDE_RESET_MDP",
                "Demande de réinitialisation",
                "🔐 " + nomComplet + " (" + utilisateur.getUsername() + ") demande la réinitialisation"
                        + " de son mot de passe. Approuvez la demande puis générez un mot de passe temporaire.",
                "PASSWORD_RESET", demandeId,
                ROLE_SUPER_ADMIN, ROLE_ADMIN);
    }

    @Transactional
    public Notification creer(Utilisateur utilisateur, String typeNotification, String titre,
                              String message, String referenceType, Long referenceId) {
        Notification notification = Notification.builder()
                .utilisateur(utilisateur)
                .typeNotification(typeNotification)
                .titre(titre)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .lu(false)
                .build();
        return notificationRepository.save(notification);
    }

    private void creerPourRoles(String typeNotification, String titre, String message,
                                String referenceType, Long referenceId, String... roles) {
        for (String role : roles) {
            List<Utilisateur> cibles = utilisateurRepository
                    .findByRoleNomRoleAndActifTrue(role);
            for (Utilisateur utilisateur : cibles) {
                creer(utilisateur, typeNotification, titre, message, referenceType, referenceId);
            }
        }
    }

    private Notification getNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.introuvable("Notification", id)));
        if (!notification.getUtilisateur().getId().equals(SecurityUtils.currentUserId())) {
            throw new ResourceNotFoundException(ErrorMessages.introuvable("Notification", id));
        }
        return notification;
    }

    private Notification getNotificationAutorisee(Long id) {
        Notification notification = getNotification(id);
        List<String> types = typesAutorises();
        if (types != null && !types.contains(notification.getTypeNotification())) {
            throw new ResourceNotFoundException(ErrorMessages.introuvable("Notification", id));
        }
        return notification;
    }

    private String nullSafe(String s) {
        return s == null ? "-" : s;
    }

    private String montant(java.math.BigDecimal montant) {
        return montant == null ? "-" : montant.toString() + " (MGA)";
    }
}
