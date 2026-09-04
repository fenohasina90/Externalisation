package com.cabex.backend.notification.dto;

import java.util.List;

public record NotificationsNonLuesResponse(
        List<NotificationResponse> notifications,
        long totalNonLues
) {
}
