package com.cabex.backend.backup.dto;

import java.time.LocalDateTime;

public record RestoreResponse(
        String message,
        String sauvegardeSecurite,
        LocalDateTime dateRestauration
) {
}
