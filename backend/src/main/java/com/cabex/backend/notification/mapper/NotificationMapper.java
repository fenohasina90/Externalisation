package com.cabex.backend.notification.mapper;

import com.cabex.backend.notification.dto.NotificationResponse;
import com.cabex.backend.notification.entity.Notification;

public class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(
                notification.getId(),
                notification.getTypeNotification(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getLu(),
                notification.getDateLecture(),
                notification.getDateCreation()
        );
    }
}
