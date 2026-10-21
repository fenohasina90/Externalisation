-- =========================================================
-- Migration : reinitialisation de mot de passe par mot de passe
--            temporaire validee par un administrateur
-- Date : 2026
-- Prerequis : migration_role_admin_reset_password.sql deja appliquee
--             (role ADMIN, permission PASSWORD_RESET_MANAGE).
--
-- Remplace le flux « lien par e-mail » par un flux « mot de passe
-- temporaire » : l'administrateur approuve la demande puis genere un
-- mot de passe temporaire que l'utilisateur doit changer a sa premiere
-- connexion. Aucune dependance a l'envoi d'e-mail n'est requise.
-- =========================================================

USE gestion_externalisation;

-- =========================================================
-- 1. NOUVELLE TABLE DES DEMANDES DE REINITIALISATION
-- =========================================================

CREATE TABLE IF NOT EXISTS demande_reinitialisation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    utilisateur_id INT NOT NULL,

    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',

    motif_refus VARCHAR(500) NULL,

    traite_par INT NULL,

    date_traitement DATETIME NULL,

    genere_par INT NULL,

    date_generation_mdp DATETIME NULL,

    date_expiration_mdp DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_demande_reset_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_demande_reset_traite_par
        FOREIGN KEY (traite_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_demande_reset_genere_par
        FOREIGN KEY (genere_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_demande_reset_utilisateur
ON demande_reinitialisation(utilisateur_id);

CREATE INDEX idx_demande_reset_statut
ON demande_reinitialisation(statut);

-- =========================================================
-- 2. ETAT DU MOT DE PASSE TEMPORAIRE SUR L'UTILISATEUR
-- =========================================================

ALTER TABLE utilisateur
    ADD COLUMN doit_changer_mdp BOOLEAN NOT NULL DEFAULT FALSE AFTER actif,
    ADD COLUMN date_expiration_mdp_temporaire DATETIME NULL AFTER doit_changer_mdp;

-- =========================================================
-- 3. CONFIGURATION DU WORKFLOW
--    - validite d'une demande en attente d'approbation ;
--    - validite du mot de passe temporaire apres generation.
-- =========================================================

INSERT IGNORE INTO configuration (cle, valeur, type_valeur, description)
VALUES
    ('DUREE_VALIDITE_DEMANDE_RESET', '1440', 'INTEGER',
     'Duree de validite (minutes) d une demande de reinitialisation en attente avant expiration'),

    ('DUREE_VALIDITE_MDP_TEMPORAIRE', '1440', 'INTEGER',
     'Duree de validite (minutes) du mot de passe temporaire avant changement obligatoire');

-- L'ancienne cle liee au lien par e-mail n'est plus utilisee.
DELETE FROM configuration WHERE cle = 'DUREE_VALIDITE_LIEN_RESET';

-- =========================================================
-- 4. SUPPRESSION DE L'ANCIENNE TABLE (flux par lien e-mail)
-- =========================================================

DROP TABLE IF EXISTS password_reset_token;