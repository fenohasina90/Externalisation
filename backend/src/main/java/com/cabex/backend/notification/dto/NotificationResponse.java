package com.cabex.backend.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String typeNotification,
        String titre,
        String message,
        String referenceType,
        Long referenceId,
        Boolean lu,
        LocalDateTime dateLecture,
        LocalDateTime dateCreation
) {
}
