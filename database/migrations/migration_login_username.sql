-- =========================================================
-- Migration : authentification par username + mot de passe
-- Ajoute la colonne utilisateur.username (UNIQUE, NOT NULL),
-- alimentée depuis la partie locale de l'email.
-- =========================================================

-- 1. Ajout de la colonne (nullable pour autoriser le backfill)
ALTER TABLE utilisateur ADD COLUMN username VARCHAR(100) NULL AFTER prenom;

-- 2. Backfill : username = partie locale de l'email (avant '@')
UPDATE utilisateur
SET username = SUBSTRING_INDEX(email, '@', 1)
WHERE username IS NULL OR username = '';

-- 3. Passage en NOT NULL puis contrainte UNIQUE
ALTER TABLE utilisateur MODIFY username VARCHAR(100) NOT NULL;

ALTER TABLE utilisateur ADD UNIQUE KEY uq_utilisateur_username (username);