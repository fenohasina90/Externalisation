package com.cabex.backend.paiement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateModePaiementRequest(
        @NotBlank(message = "Le code est obligatoire")
        @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
        String code,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères")
        String libelle
) {
}
