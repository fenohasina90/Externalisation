package com.cabex.backend.notification.dto;

import java.util.List;
import java.util.Map;

public record NotificationsNonLuesResponse(
        List<NotificationResponse> notifications,
        long totalNonLues,
        Map<String, Long> parType
) {
}
