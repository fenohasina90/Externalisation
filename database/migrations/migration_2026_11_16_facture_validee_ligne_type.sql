-- =========================================================
-- Migration : nature des lignes d'une facture validée
-- Date : 2026-11-12
-- À exécuter APRÈS migration_2026_11_15_tarif_proforma.sql.
--
-- Modèle :
--  - facture_validee_ligne : + colonne type (PRINCIPALE | RAPPEL).
--    La facture mensuelle combine :
--      - une ligne PRINCIPALE (l'abonnement, seul élément payable
--        sur cette facture),
--      - des lignes RAPPEL (prestations / débours impayés mentionnés
--        à titre d'information, réglés sur leur propre facture validée).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. facture_validee_ligne : colonne type
-- ---------------------------------------------------------
SET @has_type = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee_ligne'
    AND COLUMN_NAME = 'type'
);
SET @ddl_type = IF(@has_type = 0,
    'ALTER TABLE facture_validee_ligne ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT ''PRINCIPALE'' AFTER designation',
    'SELECT 1');
PREPARE stmt FROM @ddl_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Rattrapage : les lignes existantes qui ne sont pas
--    abonnement gardent PRINCIPALE (aucune rétrogradation de
--    données ; le type s'applique aux factures nouvellement créées).
-- ---------------------------------------------------------
UPDATE facture_validee_ligne SET type = 'RAPPEL'
WHERE designation LIKE 'Prestation —%' OR designation LIKE 'Débours —%';
