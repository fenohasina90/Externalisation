package com.cabex.backend.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expire périodiquement les demandes de réinitialisation de mot de passe
 * restées en attente trop longtemps ainsi que les mots de passe temporaires
 * non changés avant leur échéance.
 */
@Component
public class PasswordResetScheduler {

    private final PasswordResetService passwordResetService;

    public PasswordResetScheduler(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Scheduled(cron = "0 30 6 * * *")
    public void expirerDemandes() {
        passwordResetService.expirerDemandes();
    }
}