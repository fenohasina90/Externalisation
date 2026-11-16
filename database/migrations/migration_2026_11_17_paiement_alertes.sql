-- 2026-11-17 — Alertes de retard de paiement et rappel de création mensuelle.
-- Ajoute les 4 paramètres de configuration utilisés par les alertes :
--   JOURS_RETARD_PRESTATION_DEBOURS : nombre de jours après validation de la
--         facture avant de déclencher la notification de retard (prestation et débours)
--   JOURS_RETARD_ABONNEMENT : nombre de jours après émission de la facture
--         mensuelle avant de déclencher la notification de retard (abonnement)
--   JOUR_CREATION_FACTURE_MENSUELLE : jour du mois de création de la facture
--         mensuelle — rappel uniquement (la facture peut être créée à n'importe quelle date)
--   JOURS_RAPPEL_AVANT_CREATION_MENSUELLE : nombre de jours avant la création
--         mensuelle prévue pour envoyer un rappel (le jour J, un rappel est aussi émis)
-- Idempotent : les clés déjà présentes ne sont pas modifiées.

INSERT IGNORE INTO configuration
    (cle, valeur, type_valeur, description, modifiable)
VALUES
    ('JOURS_RETARD_PRESTATION_DEBOURS', '5', 'INTEGER',
     'Alerte de retard de paiement (prestation et débours) : nombre de jours après la validation de la facture avant déclenchement de la notification',
     1),
    ('JOURS_RETARD_ABONNEMENT', '7', 'INTEGER',
     'Alerte de retard de paiement (abonnement mensuel) : nombre de jours après la date d''émission de la facture mensuelle avant déclenchement de la notification',
     1),
    ('JOUR_CREATION_FACTURE_MENSUELLE', '15', 'INTEGER',
     'Jour du mois de création de la facture mensuelle d''abonnement (rappel uniquement — la facture peut être créée à n''importe quelle date)',
     1),
    ('JOURS_RAPPEL_AVANT_CREATION_MENSUELLE', '5', 'INTEGER',
     'Nombre de jours avant la création de la facture mensuelle pour envoyer un rappel (le jour J, un rappel est également émis)',
     1);
