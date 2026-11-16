-- =========================================================
-- Migration : tarif figé de la proforma (snapshot par ligne)
-- Date : 2026-11-15
-- À exécuter APRÈS migration_2026_11_14_facture_validee_mois.sql.
--
-- Modèle :
--  - ligne_service : + colonne tarif_proforma (tarif tel qu'à
--    la génération de la proforma). La ligne (ligne_service.tarif)
--    devient la valeur validée lors de la validation de la
--    facture ; la valeur du tarif dans la proforma ne change pas.
--  - Pour les bases existantes déjà validées/en cours :
--    les lignes ayant une proforma gardent leur tarif actuel en
--    guise de tarif_proforma (aucune donnée perdue).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. ligne_service : colonne tarif_proforma
-- ---------------------------------------------------------
SET @has_tp = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'tarif_proforma'
);
SET @ddl_tp = IF(@has_tp = 0,
    'ALTER TABLE ligne_service ADD COLUMN tarif_proforma DECIMAL(15,2) NULL AFTER tarif COMMENT ''Tarif figé à la génération de la proforma''',
    'SELECT 1');
PREPARE stmt FROM @ddl_tp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Rattrapage (optionnel) : initialiser le snapshot des
--    lignes déjà rattachées à une proforma, pour les bases
--    existantes.
-- ---------------------------------------------------------
UPDATE ligne_service l
  JOIN facture_proforma p ON p.id = l.facture_id
   SET l.tarif_proforma = l.tarif
 WHERE l.tarif_proforma IS NULL;
