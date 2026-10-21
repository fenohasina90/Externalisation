-- =========================================================
-- Migration : role ADMIN + reinitialisation de mot de passe
--            validee par un administrateur
-- Date : 2026
-- Prerequis : base gestion_externalisation initialisee (BD_corrige.sql)
-- Idempotente autant que possible (INSERT IGNORE / modifications conditionnelles).
-- =========================================================

USE gestion_externalisation;

-- =========================================================
-- 1. SUIVI DES DEMANDES DE REINITIALISATION
--    token_hash et expiration deviennent nullables : le token n'est
--    genere qu'apres validation par un administrateur.
-- =========================================================

ALTER TABLE password_reset_token
    MODIFY COLUMN token_hash VARCHAR(255) NULL,
    MODIFY COLUMN expiration DATETIME NULL,
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE' AFTER expiration,
    ADD COLUMN traite_par INT NULL AFTER statut,
    ADD COLUMN date_traitement DATETIME NULL AFTER traite_par;

ALTER TABLE password_reset_token
    ADD CONSTRAINT fk_reset_traite_par
        FOREIGN KEY (traite_par) REFERENCES utilisateur(id) ON DELETE SET NULL;

CREATE INDEX idx_reset_statut ON password_reset_token(statut);

-- Les tokens deja generes avant la migration ne sont pas des demandes en attente
UPDATE password_reset_token
SET statut = CASE WHEN utilise = TRUE THEN 'UTILISE' ELSE 'ENVOYE' END
WHERE token_hash IS NOT NULL;

-- =========================================================
-- 2. ROLE ADMIN
-- =========================================================

INSERT IGNORE INTO role_utilisateur (nom_role, description)
VALUES ('ADMIN', 'Administrateur : acces complet, validation des demandes de reinitialisation');

-- =========================================================
-- 3. PERMISSION DE GESTION DES REINITIALISATIONS
-- =========================================================

INSERT IGNORE INTO permission (code_permission, nom_permission, module, description)
VALUES ('PASSWORD_RESET_MANAGE', 'Gerer les reinitialisations de mot de passe',
        'UTILISATEUR', 'Valider ou rejeter les demandes de reinitialisation de mot de passe');

-- =========================================================
-- 4. ATTRIBUTION DES PERMISSIONS : SUPER_ADMIN + ADMIN
-- =========================================================

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN');

-- =========================================================
-- 5. CONFIGURATION DU WORKFLOW DE REINITIALISATION
-- =========================================================

INSERT IGNORE INTO configuration (cle, valeur, type_valeur, description)
VALUES
    ('DUREE_VALIDITE_LIEN_RESET', '30', 'INTEGER',
     'Duree de validite (minutes) du lien de reinitialisation genere apres validation'),

    ('URL_APPLICATION', 'http://localhost:5173', 'STRING',
     'URL de base du frontend utilisee pour construire les liens envoyes par email');