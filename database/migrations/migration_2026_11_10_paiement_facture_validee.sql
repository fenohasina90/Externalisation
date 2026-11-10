-- =========================================================
-- Migration : paiement toujours lié à une facture validée
-- Date : 2026-11-10
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Modèle :
--  - Paiement : remplacer echeance_id et ligne_service_id par
--    facture_validee_id (toujours lié à une facture validée,
--    que ce soit DEBOURS ou EXTERNALISATION).
--  - Ligne service PRESTATION : dates début/fin NULL autorisées.
--  - Facture validee : peut exister sans proforma (cas DEBOURS
--    ou ABONNEMENT_MENSUEL sans proforma distincte).
--
-- Cycle demande : EN_ATTENTE → EN_COURS (après validation proforma)
--                 → TERMINEE (manuel) ; ANNULEE depuis EN_ATTENTE.
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Paiement : remplacer echeance_id et ligne_service_id
--    par facture_validee_id
-- ---------------------------------------------------------
SET @has_echeance = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'echeance_id'
);
SET @ddl_drop_echeance = IF(@has_echeance = 1,
    'ALTER TABLE paiement DROP FOREIGN KEY fk_paiement_echeance, DROP COLUMN echeance_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_echeance;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_ligne = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'ligne_service_id'
);
SET @ddl_drop_ligne = IF(@has_ligne = 1,
    'ALTER TABLE paiement DROP FOREIGN KEY fk_paiement_ligne, DROP COLUMN ligne_service_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_ligne;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_facture_validee = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'facture_validee_id'
);
SET @ddl_add = IF(@has_facture_validee = 0,
    'ALTER TABLE paiement ADD COLUMN facture_validee_id INT NULL COMMENT "Facture validée liée au paiement (obligatoire pour EXTERNALISATION et DEBOURS)" AFTER demande_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_add;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND CONSTRAINT_NAME = 'fk_paiement_facture_validee'
);
SET @ddl_fk = IF(@has_fk = 0,
    'ALTER TABLE paiement ADD CONSTRAINT fk_paiement_facture_validee FOREIGN KEY (facture_validee_id) REFERENCES facture_validee(id) ON DELETE RESTRICT',
    'SELECT 1');
PREPARE stmt FROM @ddl_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND INDEX_NAME = 'idx_paiement_facture_validee'
);
SET @ddl_idx = IF(@has_idx = 0,
    'CREATE INDEX idx_paiement_facture_validee ON paiement(facture_validee_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Ligne service : dates début/fin NULL autorisées
--    (PRESTATION peut ne pas avoir de période)
-- ---------------------------------------------------------
-- Déjà NULL dans BD_corrige.sql, garder pour les bases existantes.
SET @has_debut = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'debut_service'
    AND IS_NULLABLE = 'NO'
);
SET @ddl_debut = IF(@has_debut = 1,
    'ALTER TABLE ligne_service MODIFY COLUMN debut_service DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl_debut;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fin = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'fin_service'
    AND IS_NULLABLE = 'NO'
);
SET @ddl_fin = IF(@has_fin = 1,
    'ALTER TABLE ligne_service MODIFY COLUMN fin_service DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl_fin;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 3. Facture validee : facture_proforma_id déjà NULL (DEBOURS)
--    Gardé tel quel.
