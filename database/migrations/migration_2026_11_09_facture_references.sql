-- =========================================================
-- Migration : dédouble la référence de facture
-- (proforma visible à vie + référence validée à la validation)
-- Date : 2026-11-09
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Modèle :
--  - reference_facture (existant) = référence PROFORMA
--      format : PRO-DDMMYYYY-XXXXX
--      ex. PRO-12112026-00152, XXXXX réinitialisé par année.
--  - reference_facture_validee (nouveau) = référence VALIDÉE
--      format : XXXXXEXT/TNR/YYYY
--      ex. 01522/TNR/2026, XXXXX 00001-99999 réinitialisé par année.
--
-- La proforma reste visible/appelable après validation
-- (même ligne, même document) ; la référence validée est
-- générée dans FactureService.valider().
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture'
    AND COLUMN_NAME = 'reference_facture_validee'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE facture ADD COLUMN reference_facture_validee VARCHAR(50) NULL UNIQUE COMMENT ''Reference facture validee (generee a la validation, ex. 01522/TNR/2026)''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
