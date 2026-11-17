-- =========================================================
-- Migration : type de prestation suivi du dossier transaction
-- Date : 2026-11-17
-- À exécuter APRÈS migration_transaction_immobiliere.sql.
--
-- Modèle :
--  - dossier_transaction_immobiliere : prestation_abonnement_mensuel (BOOLEAN)
--    devient type_prestation_dossier (VARCHAR(30)).
--    Deux choix exclusifs : PRESTATION (défaut) ou ABONNEMENT_MENSUEL.
--    false → PRESTATION, true → ABONNEMENT_MENSUEL.
--
-- Script réexécutable proprement (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- Détection de la présence de l'ancienne colonne booléenne
SET @has_old = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
      AND TABLE_NAME = 'dossier_transaction_immobiliere'
      AND COLUMN_NAME = 'prestation_abonnement_mensuel'
);

-- ---------------------------------------------------------
-- 1. nouvelle colonne type_prestation_dossier
-- ---------------------------------------------------------
SET @has_type = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
      AND TABLE_NAME = 'dossier_transaction_immobiliere'
      AND COLUMN_NAME = 'type_prestation_dossier'
);
SET @ddl_type = IF(@has_type = 0,
    'ALTER TABLE dossier_transaction_immobiliere ADD COLUMN type_prestation_dossier VARCHAR(30) NOT NULL DEFAULT ''PRESTATION'' COMMENT ''PRESTATION (payable après facture validée) ou ABONNEMENT_MENSUEL (facture du mois)'' AFTER commission_cn',
    'SELECT 1');
PREPARE stmt FROM @ddl_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. migration des données (uniquement si l'ancienne colonne
--    est encore là) : false → PRESTATION, true → ABONNEMENT_MENSUEL
-- ---------------------------------------------------------
SET @upd = IF(@has_old = 1,
    'UPDATE dossier_transaction_immobiliere SET type_prestation_dossier = CASE WHEN prestation_abonnement_mensuel = TRUE THEN ''ABONNEMENT_MENSUEL'' ELSE ''PRESTATION'' END',
    'SELECT 1');
PREPARE stmt FROM @upd;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 3. contrainte CHECK (réexécutable)
-- ---------------------------------------------------------
SET @has_check = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
      AND TABLE_NAME = 'dossier_transaction_immobiliere'
      AND CONSTRAINT_NAME = 'chk_dti_type_prestation_dossier'
);
SET @ddl_check = IF(@has_check = 0,
    'ALTER TABLE dossier_transaction_immobiliere ADD CONSTRAINT chk_dti_type_prestation_dossier CHECK (type_prestation_dossier IN (''PRESTATION'', ''ABONNEMENT_MENSUEL''))',
    'SELECT 1');
PREPARE stmt FROM @ddl_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 4. suppression de l'ancienne colonne booléenne (si présente)
-- ---------------------------------------------------------
SET @ddl_old = IF(@has_old = 1,
    'ALTER TABLE dossier_transaction_immobiliere DROP COLUMN prestation_abonnement_mensuel',
    'SELECT 1');
PREPARE stmt FROM @ddl_old;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
