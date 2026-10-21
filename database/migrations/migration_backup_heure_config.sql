-- =========================================================
-- Heure configurable de la sauvegarde automatique
-- =========================================================

INSERT IGNORE INTO configuration
(cle, valeur, type_valeur, description, modifiable)
VALUES
(
    'HEURE_SAUVEGARDE_AUTOMATIQUE',
    '02:00',
    'TIME',
    'Heure de la sauvegarde automatique quotidienne (heure du serveur, HH:mm)',
    1
);
