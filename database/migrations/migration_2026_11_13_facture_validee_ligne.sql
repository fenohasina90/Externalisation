-- =========================================================
-- Migration : lignes des factures validées (facture combinée du mois)
-- Date : 2026-11-12
-- À exécuter APRÈS migration_2026_11_12_cheque.sql.
--
-- Modèle :
--  - Une facture validée peut être MULTI-LIGNES (facture combinée
--    du mois d'un abonnement) : chaque ligne porte sa désignation
--    et son montant ; la somme des lignes = montant de la facture.
--  - Les factures validées existantes (proforma, débours…) n'ont
--    pas de lignes et restent inchangées (le lien ligne_service_id
--    de facture_validee n'est pas remis en cause).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Table facture_validee_ligne
-- ---------------------------------------------------------
SET @has_table = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee_ligne'
);
SET @ddl_table = IF(@has_table = 0,
    'CREATE TABLE facture_validee_ligne (
        id INT AUTO_INCREMENT PRIMARY KEY,
        facture_validee_id INT NOT NULL,
        ligne_service_id INT NOT NULL,
        designation VARCHAR(255) NOT NULL,
        montant DECIMAL(15,2) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_facture_validee_ligne_facture
            FOREIGN KEY (facture_validee_id)
            REFERENCES facture_validee(id)
            ON DELETE CASCADE,
        CONSTRAINT fk_facture_validee_ligne_ligne
            FOREIGN KEY (ligne_service_id)
            REFERENCES ligne_service(id)
            ON DELETE RESTRICT,
        CONSTRAINT chk_facture_validee_ligne_montant
            CHECK (montant >= 0)
    ) ENGINE=InnoDB',
    'SELECT 1');
PREPARE stmt FROM @ddl_table;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Index (gardes information_schema)
-- ---------------------------------------------------------
SET @has_idx_facture = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee_ligne'
    AND INDEX_NAME = 'idx_facture_validee_ligne_facture'
);
SET @ddl_idx_facture = IF(@has_idx_facture = 0,
    'CREATE INDEX idx_facture_validee_ligne_facture ON facture_validee_ligne(facture_validee_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx_facture;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_ligne = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee_ligne'
    AND INDEX_NAME = 'idx_facture_validee_ligne_ligne'
);
SET @ddl_idx_ligne = IF(@has_idx_ligne = 0,
    'CREATE INDEX idx_facture_validee_ligne_ligne ON facture_validee_ligne(ligne_service_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx_ligne;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
