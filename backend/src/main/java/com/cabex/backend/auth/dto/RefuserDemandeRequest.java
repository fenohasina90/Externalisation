package com.cabex.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefuserDemandeRequest(
        @NotBlank(message = "Le motif de refus est obligatoire")
        @Size(max = 500, message = "Le motif de refus ne peut pas dépasser 500 caractères")
        String motif
) {
}