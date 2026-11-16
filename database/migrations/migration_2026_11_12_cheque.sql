-- =========================================================
-- Migration : mode de paiement CHÈQUE
-- Date : 2026-11-12
-- À exécuter APRÈS les migrations 2026_11_10 et 2026_11_11.
--
-- Modèle :
--  - Nouveau paiement : colonnes banque (VARCHAR 150) et
--    numero_cheque (VARCHAR 100), NULL pour les autres modes.
--    Obligatoires lorsque le mode CHÈQUE est sélectionné.
--  - Mode de paiement CHÈQUE ajouté aux référentiels existants
--    (les nouvelles installations sont seedées par BD_corrige.sql).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Paiement : colonnes banque / numero_cheque
-- ---------------------------------------------------------
SET @has_banque = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'banque'
);
SET @ddl_banque = IF(@has_banque = 0,
    'ALTER TABLE paiement ADD COLUMN banque VARCHAR(150) NULL AFTER reference_virement',
    'SELECT 1');
PREPARE stmt FROM @ddl_banque;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_numero = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'numero_cheque'
);
SET @ddl_numero = IF(@has_numero = 0,
    'ALTER TABLE paiement ADD COLUMN numero_cheque VARCHAR(100) NULL AFTER banque',
    'SELECT 1');
PREPARE stmt FROM @ddl_numero;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Mode de paiement CHÈQUE (inséré seulement s'il n'existe pas)
-- ---------------------------------------------------------
INSERT INTO mode_paiement (code, libelle)
SELECT 'CHEQUE', 'Chèque'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mode_paiement WHERE code = 'CHEQUE');
