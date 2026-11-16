-- =========================================================
-- Migration CORRIGÉE : catégorie EXTERNALISATION / DEBOURS
-- sur LIGNE_SERVICE (et non sur demande_service)
-- Date : 2026-11-09
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Correction (2026-11-09) : une demande peut contenir plusieurs
-- lignes avec des catégories différentes. La catégorie est donc
-- portée par ligne_service, pas par demande_service.
--
-- Contenu :
--  1. Nettoyage de l'ancienne version qui avait altéré
--     demande_service (colonnes retirees si présentes).
--  2. Colonnes ligne_service.categorie / designation_debours /
--     montant_debours (DEBOURS uniquement).
--  3. Colonne paiement.ligne_service_id (lien direct des
--     paiements DEBOURS vers leur ligne, sans échéance).
--
-- Cycle par ligne DEBOURS :
--   EN_ATTENTE → NON_PAYEE (validation du montant)
--             → PARTIEL   (paiement partiel)
--             → PAYEE     (paiement intégral)
--   EN_ATTENTE → ANNULEE (annulation possible uniquement en EN_ATTENTE)
--
-- Cycle par ligne EXTERNALISATION (inchangé) :
--   CREE → VALIDE (facture validée) → EN_ATTENTE (plan établi)
--        → EN_COURS (1er paiement) → TERMINE (date de fin)
--
-- Les statuts de ligne sont des VARCHAR libres (aucune FK vers
-- statut_demande) : NON_PAYEE / PARTIEL / PAYEE n'ont pas besoin
-- d'entrée dans la table statut_demande.
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Nettoyage : retirer les colonnes demande_service
--    (ancienne version de cette migration, si déjà appliquée)
-- ---------------------------------------------------------
SET @has_categorie = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'demande_service'
    AND COLUMN_NAME = 'categorie_demande'
);
SET @ddl_drop_cat = IF(@has_categorie = 1,
    'ALTER TABLE demande_service DROP COLUMN categorie_demande',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_cat;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_desig = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'demande_service'
    AND COLUMN_NAME = 'designation_debours'
);
SET @ddl_drop_desig = IF(@has_desig = 1,
    'ALTER TABLE demande_service DROP COLUMN designation_debours',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_desig;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_montant = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'demande_service'
    AND COLUMN_NAME = 'montant_debours'
);
SET @ddl_drop_mont = IF(@has_montant = 1,
    'ALTER TABLE demande_service DROP COLUMN montant_debours',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_mont;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. ligne_service.categorie
-- ---------------------------------------------------------
SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'categorie'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE ligne_service ADD COLUMN categorie VARCHAR(30) NOT NULL DEFAULT ''EXTERNALISATION'' COMMENT ''EXTERNALISATION (par défaut) ou DEBOURS — par ligne''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ligne_service
SET categorie = 'EXTERNALISATION'
WHERE categorie IS NULL OR categorie = '';

SET @idx_existe = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND INDEX_NAME = 'idx_ligne_categorie'
);
SET @ddl_idx = IF(
    @idx_existe = 0,
    'CREATE INDEX idx_ligne_categorie ON ligne_service(categorie)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @ddl_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- ---------------------------------------------------------
-- 3. ligne_service.designation_debours
-- ---------------------------------------------------------
SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'designation_debours'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE ligne_service ADD COLUMN designation_debours VARCHAR(255) NULL COMMENT ''Designation du debours (DEBOURS uniquement)''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 4. ligne_service.montant_debours
-- ---------------------------------------------------------
SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND COLUMN_NAME = 'montant_debours'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE ligne_service ADD COLUMN montant_debours DECIMAL(15,2) NULL COMMENT ''Montant du debours valide (DEBOURS uniquement, NULL tant que non valide)''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @chk_existe = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'ligne_service'
    AND CONSTRAINT_NAME = 'chk_ligne_montant_debours'
);
SET @ddl_chk = IF(
    @chk_existe = 0,
    'ALTER TABLE ligne_service ADD CONSTRAINT chk_ligne_montant_debours CHECK (montant_debours IS NULL OR montant_debours > 0)',
    'SELECT 1'
);
PREPARE stmt_chk FROM @ddl_chk;
EXECUTE stmt_chk;
DEALLOCATE PREPARE stmt_chk;

-- ---------------------------------------------------------
-- 5. ligne_service : service/tarif/période optionnels
--    (NULL pour les lignes DEBOURS, sans service catalogue)
-- ---------------------------------------------------------
-- MySQL : MODIFY COLUMN vers NULL (idempotent)
ALTER TABLE ligne_service MODIFY COLUMN service_id INT NULL;
ALTER TABLE ligne_service MODIFY COLUMN tarif DECIMAL(15,2) NULL;
ALTER TABLE ligne_service MODIFY COLUMN debut_service DATE NULL;
ALTER TABLE ligne_service MODIFY COLUMN fin_service DATE NULL;

-- ---------------------------------------------------------
-- 6. paiement.ligne_service_id (paiements DEBOURS directs)
-- ---------------------------------------------------------
SET @col_existe = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'ligne_service_id'
);
SET @ddl = IF(
    @col_existe = 0,
    'ALTER TABLE paiement ADD COLUMN ligne_service_id INT NULL COMMENT ''Ligne DEBOURS payee (NULL pour EXTERNALISATION via echeance)''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_existe = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND CONSTRAINT_NAME = 'fk_paiement_ligne'
);
SET @ddl_fk = IF(
    @fk_existe = 0,
    'ALTER TABLE paiement ADD CONSTRAINT fk_paiement_ligne FOREIGN KEY (ligne_service_id) REFERENCES ligne_service(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt_fk FROM @ddl_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

SET @idx_existe = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND INDEX_NAME = 'idx_paiement_ligne'
);
SET @ddl_idx = IF(
    @idx_existe = 0,
    'CREATE INDEX idx_paiement_ligne ON paiement(ligne_service_id)',
    'SELECT 1'
);
PREPARE stmt_idx2 FROM @ddl_idx;
EXECUTE stmt_idx2;
DEALLOCATE PREPARE stmt_idx2;
