package com.cabex.backend.facture.dto;

import jakarta.validation.constraints.NotBlank;

public record GenererFacturesRequest(
        @NotBlank(message = "Le mode de facturation est obligatoire (UNIQUE ou PAR_SERVICE)")
        String modeFacturation
) {
}
