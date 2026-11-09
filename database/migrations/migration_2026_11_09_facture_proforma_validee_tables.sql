-- =========================================================
-- Migration : deux tables facture (proforma / validée)
-- Date : 2026-11-09
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Modèle :
--  - facture_proforma (ex facture) : montant proforma, modifiable
--    tant que PROFORMA, toujours visible après validation.
--  - facture_validee (nouvelle) : montant validé (peut différer
--    du montant proforma), référence XXXXXEXT/TNR/YYYY,
--    validée par / le.
-- Une facture proforma validée ne peut pas avoir deux validées
-- (UNIQUE sur facture_proforma_id).
--
-- Reprise : les factures déjà VALIDEE avec référence validée
-- sont copiées dans facture_validee. Les anciennes VALIDEE
-- sans référence restent sans ligne validée (legacy).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Renommer facture -> facture_proforma
-- ---------------------------------------------------------
SET @facture_existe = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture'
);
SET @proforma_existe = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_proforma'
);
SET @ddl_rename = IF(
    @facture_existe = 1 AND @proforma_existe = 0,
    'RENAME TABLE facture TO facture_proforma',
    'SELECT 1'
);
PREPARE stmt FROM @ddl_rename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. Table facture_validee
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS facture_validee (
    id INT AUTO_INCREMENT PRIMARY KEY,

    reference_facture_validee VARCHAR(50) NOT NULL UNIQUE,

    facture_proforma_id INT NOT NULL UNIQUE,

    demande_id INT NOT NULL,

    montant DECIMAL(15,2) NOT NULL COMMENT 'Montant validé (peut différer du montant proforma)',

    valide_par INT NULL,

    valide_le DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_facture_validee_proforma
        FOREIGN KEY (facture_proforma_id)
        REFERENCES facture_proforma(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_facture_validee_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_facture_validee_valide_par
        FOREIGN KEY (valide_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_facture_validee_montant
        CHECK (montant >= 0)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_facture_validee_demande ON facture_validee(demande_id);

-- ---------------------------------------------------------
-- 3. Reprise : factures déjà validées -> facture_validee
-- ---------------------------------------------------------
INSERT INTO facture_validee
    (reference_facture_validee, facture_proforma_id, demande_id, montant, valide_par, valide_le, created_at)
SELECT
    p.reference_facture_validee,
    p.id,
    p.demande_id,
    p.montant,
    p.valide_par,
    COALESCE(p.valide_le, p.updated_at),
    p.created_at
FROM facture_proforma p
WHERE p.statut = 'VALIDEE'
  AND p.reference_facture_validee IS NOT NULL
ON DUPLICATE KEY UPDATE
    reference_facture_validee = VALUES(reference_facture_validee),
    demande_id = VALUES(demande_id),
    montant = VALUES(montant),
    valide_par = VALUES(valide_par),
    valide_le = VALUES(valide_le);

-- ---------------------------------------------------------
-- 4. Nettoyer l'ancienne colonne validée sur proforma
--    (données déjà reprises en 3)
-- ---------------------------------------------------------
SET @has_ref_validee = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_proforma'
    AND COLUMN_NAME = 'reference_facture_validee'
);
SET @ddl_drop_ref = IF(@has_ref_validee = 1,
    'ALTER TABLE facture_proforma DROP COLUMN reference_facture_validee',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_ref;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_valide_par = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_proforma'
    AND COLUMN_NAME = 'valide_par'
);
SET @ddl_drop_vp = IF(@has_valide_par = 1,
    'ALTER TABLE facture_proforma DROP COLUMN valide_par',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_vp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_valide_le = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_proforma'
    AND COLUMN_NAME = 'valide_le'
);
SET @ddl_drop_vl = IF(@has_valide_le = 1,
    'ALTER TABLE facture_proforma DROP COLUMN valide_le',
    'SELECT 1');
PREPARE stmt FROM @ddl_drop_vl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
