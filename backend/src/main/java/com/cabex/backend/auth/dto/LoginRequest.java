package com.cabex.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "L'username est obligatoire")
        @Size(max = 100, message = "L'username ne doit pas dépasser 100 caractères")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
