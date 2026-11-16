-- =========================================================
-- Migration : sous-type PRESTATION / ABONNEMENT_MENSUEL
-- pour les lignes EXTERNALISATION de service STANDARD
-- Date : 2026-11-09
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Modèle :
--  - Ligne EXTERNALISATION + service STANDARD :
--      type_prestation = 'PRESTATION' ou 'ABONNEMENT_MENSUEL'
--  - Ligne DEBOURS : type_prestation = NULL
--  - Ligne EXTERNALISATION CREATION / TRANSACTION_IMMOBILIERE : NULL
--
-- La spécificité métier de chaque type (suivi, facturation,
-- échéances) sera précisée ensuite ; ce script ne crée que
-- le support stockage + contrainte.
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'type_prestation'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE ligne_service ADD COLUMN type_prestation VARCHAR(30) NULL COMMENT ''Sous-type STANDARD : PRESTATION ou ABONNEMENT_MENSUEL (EXTERNALISATION + STANDARD uniquement)''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Reprise : les lignes EXTERNALISATION STANDARD existantes sont PRESTATION.
UPDATE ligne_service l
JOIN services s ON s.id = l.service_id
SET l.type_prestation = 'PRESTATION'
WHERE l.categorie = 'EXTERNALISATION'
  AND s.type_service = 'STANDARD'
  AND (l.type_prestation IS NULL OR l.type_prestation = '');

SET @chk_existe = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND CONSTRAINT_NAME = 'chk_ligne_type_prestation'
);
SET @ddl_chk = IF(
    @chk_existe = 0,
    'ALTER TABLE ligne_service ADD CONSTRAINT chk_ligne_type_prestation CHECK (type_prestation IS NULL OR type_prestation IN (''PRESTATION'', ''ABONNEMENT_MENSUEL''))',
    'SELECT 1'
);
PREPARE stmt_chk FROM @ddl_chk;
EXECUTE stmt_chk;
DEALLOCATE PREPARE stmt_chk;

SET @idx_existe = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND INDEX_NAME = 'idx_ligne_type_prestation'
);
SET @ddl_idx = IF(
    @idx_existe = 0,
    'CREATE INDEX idx_ligne_type_prestation ON ligne_service(type_prestation)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @ddl_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
