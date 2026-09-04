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
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
public class NotificationService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Pageable pageable) {
        return notificationRepository
                .findByUtilisateurIdOrderByDateCreationDesc(SecurityUtils.currentUserId(), pageable)
                .map(NotificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationsNonLuesResponse nonLues() {
        Integer userId = SecurityUtils.currentUserId();
        List<NotificationResponse> notifications = notificationRepository
                .findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
        return new NotificationsNonLuesResponse(
                notifications,
                notificationRepository.countByUtilisateurIdAndLuFalse(userId));
    }

    @Transactional
    public NotificationResponse marquerLu(Long id) {
        Notification notification = getNotification(id);
        if (!notification.getLu()) {
            notification.setLu(true);
            notification.setDateLecture(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationMapper.toResponse(notification);
    }

    @Transactional
    public long toutLire() {
        return notificationRepository.marquerToutLu(
                SecurityUtils.currentUserId(), LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        Notification notification = getNotification(id);
        notificationRepository.delete(notification);
    }

    public void notifierNouvelleDemande(DemandeService demande) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourSuperAdmin(
                "NOUVELLE_DEMANDE",
                "Nouvelle demande",
                "📋 Nouvelle demande " + nullSafe(demande.getReferenceDemande())
                        + " enregistrée pour " + societe
                        + (demande.getCommercial() != null
                        ? " par le commercial " + demande.getCommercial().getNomCommercial() : ""),
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null);
    }

    public void notifierChangementStatut(DemandeService demande, String nouveauStatut) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourSuperAdmin(
                "CHANGEMENT_STATUT",
                "Changement de statut",
                "🔄 La demande " + nullSafe(demande.getReferenceDemande()) + " de " + societe
                        + " est passée au statut " + nouveauStatut + ".",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null);
    }

    public void notifierPaiementEnregistre(Paiement paiement) {
        String societe = paiement.getDemande() != null && paiement.getDemande().getSociete() != null
                ? paiement.getDemande().getSociete().getNomSociete() : "client";
        creerPourSuperAdmin(
                "PAIEMENT_ENREGISTRE",
                "Paiement enregistré",
                "💳 Paiement " + nullSafe(paiement.getReferencePaiement()) + " de "
                        + montant(paiement.getMontantPaye()) + " enregistré pour " + societe + ".",
                "PAIEMENT", paiement.getId() != null ? paiement.getId().longValue() : null);
    }

    public void notifierEcheanceProche(DemandeService demande, int jours) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourSuperAdmin(
                "ECHEANCE_PROCHE",
                "Échéance proche",
                "⚠️ L'échéance de la société " + societe
                        + " arrive à échéance dans " + jours + " jour" + (jours > 1 ? "s" : "") + ".",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null);
    }

    public void notifierEcheanceEnRetard(DemandeService demande) {
        String societe = demande.getSociete() != null ? demande.getSociete().getNomSociete() : "client";
        creerPourSuperAdmin(
                "ECHEANCE_RETARD",
                "Paiement en retard",
                "⛔ L'échéance de la société " + societe + " est en retard.",
                "DEMANDE", demande.getId() != null ? demande.getId().longValue() : null);
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

    private void creerPourSuperAdmin(String typeNotification, String titre, String message,
                                     String referenceType, Long referenceId) {
        List<Utilisateur> admins = utilisateurRepository
                .findByRoleNomRoleAndActifTrue(ROLE_SUPER_ADMIN);
        for (Utilisateur admin : admins) {
            creer(admin, typeNotification, titre, message, referenceType, referenceId);
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

    private String nullSafe(String s) {
        return s == null ? "-" : s;
    }

    private String montant(java.math.BigDecimal montant) {
        return montant == null ? "-" : montant.toString() + " (MGA)";
    }
}
