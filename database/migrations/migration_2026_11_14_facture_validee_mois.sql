-- =========================================================
-- Migration : mois facturé par la facture du mois
-- Date : 2026-11-11
-- À exécuter APRÈS migration_2026_11_13_facture_validee_ligne.sql.
--
-- Modèle :
--  - facture_validee : + colonne mois (DATE, premier du mois).
--    Renseignée uniquement pour les factures du mois d'un abonnement
--    (type ABONNEMENT_MENSUEL) ; NULL pour les autres factures validées.
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. facture_validee : colonne mois
-- ---------------------------------------------------------
SET @has_mois = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND COLUMN_NAME = 'mois'
);
SET @ddl_mois = IF(@has_mois = 0,
    'ALTER TABLE facture_validee ADD COLUMN mois DATE NULL AFTER valide_le',
    'SELECT 1');
PREPARE stmt FROM @ddl_mois;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
