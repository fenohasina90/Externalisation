package com.cabex.backend.notification.controller;

import com.cabex.backend.notification.dto.NotificationResponse;
import com.cabex.backend.notification.dto.NotificationsNonLuesResponse;
import com.cabex.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Centre de notifications avec compteur de non lues")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lister mes notifications",
            description = "Notifications de l'utilisateur connecté, triées par date décroissante")
    public ResponseEntity<Page<NotificationResponse>> list(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean nonLues) {
        return ResponseEntity.ok(notificationService.list(pageable, type, nonLues));
    }

    @GetMapping("/non-lues")
    @Operation(summary = "Notifications non lues + compteur",
            description = "Liste des notifications non lues et le total de non lues (badge)")
    public ResponseEntity<NotificationsNonLuesResponse> nonLues() {
        return ResponseEntity.ok(notificationService.nonLues());
    }

    @PatchMapping("/{id}/lu")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<NotificationResponse> marquerLu(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerLu(id));
    }

    @PatchMapping("/tout-lire")
    @Operation(summary = "Tout marquer comme lu",
            description = "Marque toutes les notifications non lues comme lues, retourne le nombre mis à jour")
    public ResponseEntity<Map<String, Long>> toutLire() {
        return ResponseEntity.ok(Map.of("misAJour", notificationService.toutLire()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une notification")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
