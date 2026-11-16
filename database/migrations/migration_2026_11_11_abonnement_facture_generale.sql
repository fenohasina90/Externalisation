-- =========================================================
-- Migration : facture générale abonnement + lien échéance ↔ paiement
-- Date : 2026-11-11
-- À exécuter sur CHAQUE base existante avant redémarrage.
-- À exécuter APRÈS migration_2026_11_10_paiement_facture_validee.sql.
--
-- Modèle après migration :
--  - facture_validee : + facture_generale_id (auto-référence, NULL si la
--    facture EST la générale ou cas simple), + ligne_service_id (débours).
--    La facture générale validée de l'abonnement a facture_generale_id NULL
--    et est référencée par les factures mensuelles enfants.
--  - paiement : + echeance_id (NULL pour DEBOURS / PRESTATION / ABONNEMENT).
--    Le paiement reste toujours lié à facture_validee_id (NOT NULL).
--  - paiement.facture_validee_id passe en NOT NULL (si aucune ligne NULL).
--
-- Script réexécutable (gardes information_schema).
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. facture_validee : facture_generale_id (auto-référence)
-- ---------------------------------------------------------
SET @has_generale = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND COLUMN_NAME = 'facture_generale_id'
);
SET @ddl_generale = IF(@has_generale = 0,
    'ALTER TABLE facture_validee ADD COLUMN facture_generale_id INT NULL AFTER facture_proforma_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_generale;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_generale = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND CONSTRAINT_NAME = 'fk_facture_validee_generale'
);
SET @ddl_fk_generale = IF(@has_fk_generale = 0,
    'ALTER TABLE facture_validee ADD CONSTRAINT fk_facture_validee_generale FOREIGN KEY (facture_generale_id) REFERENCES facture_validee(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl_fk_generale;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 2. facture_validee : ligne_service_id (DEBOURS)
-- ---------------------------------------------------------
SET @has_ligne = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND COLUMN_NAME = 'ligne_service_id'
);
SET @ddl_ligne = IF(@has_ligne = 0,
    'ALTER TABLE facture_validee ADD COLUMN ligne_service_id INT NULL AFTER facture_generale_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_ligne;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_ligne = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND CONSTRAINT_NAME = 'fk_facture_validee_ligne'
);
SET @ddl_fk_ligne = IF(@has_fk_ligne = 0,
    'ALTER TABLE facture_validee ADD CONSTRAINT fk_facture_validee_ligne FOREIGN KEY (ligne_service_id) REFERENCES ligne_service(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl_fk_ligne;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index (gardes information_schema, script réexécutable)
SET @has_idx_generale = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND INDEX_NAME = 'idx_facture_validee_generale'
);
SET @ddl_idx_generale = IF(@has_idx_generale = 0,
    'CREATE INDEX idx_facture_validee_generale ON facture_validee(facture_generale_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx_generale;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_ligne = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'facture_validee'
    AND INDEX_NAME = 'idx_facture_validee_ligne'
);
SET @ddl_idx_ligne = IF(@has_idx_ligne = 0,
    'CREATE INDEX idx_facture_validee_ligne ON facture_validee(ligne_service_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx_ligne;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 3. paiement : echeance_id (nullable) + FK + index
-- ---------------------------------------------------------
SET @has_echeance = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND COLUMN_NAME = 'echeance_id'
);
SET @ddl_echeance = IF(@has_echeance = 0,
    'ALTER TABLE paiement ADD COLUMN echeance_id INT NULL AFTER facture_validee_id',
    'SELECT 1');
PREPARE stmt FROM @ddl_echeance;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_echeance = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND CONSTRAINT_NAME = 'fk_paiement_echeance'
);
SET @ddl_fk_echeance = IF(@has_fk_echeance = 0,
    'ALTER TABLE paiement ADD CONSTRAINT fk_paiement_echeance FOREIGN KEY (echeance_id) REFERENCES echeance_paiement(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl_fk_echeance;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_echeance = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'paiement'
    AND INDEX_NAME = 'idx_paiement_echeance'
);
SET @ddl_idx_echeance = IF(@has_idx_echeance = 0,
    'CREATE INDEX idx_paiement_echeance ON paiement(echeance_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl_idx_echeance;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------
-- 5. Rattrapage (optionnel) : facture validée pour chaque
--    débours déjà validé avant cette migration.
--    Crée une référence XXXXXEXT/TNR/YYYY par année, en
--    continuant la séquence existante.
-- ---------------------------------------------------------
INSERT INTO facture_validee
    (reference_facture_validee, facture_proforma_id, ligne_service_id,
     demande_id, montant, valide_le, created_at, updated_at)
SELECT
    CONCAT(LPAD(vals.seq, 5, '0'), 'EXT/TNR/', YEAR(vals.date_demande)),
    NULL,
    vals.ligne_id,
    vals.demande_id,
    vals.montant,
    NOW(),
    NOW(),
    NOW()
FROM (
    SELECT
        l.id AS ligne_id,
        l.demande_id,
        l.montant_debours AS montant,
        dem.date_demande,
        ROW_NUMBER() OVER (PARTITION BY YEAR(dem.date_demande) ORDER BY l.id)
            + COALESCE((
                SELECT MAX(CAST(SUBSTRING_INDEX(reference_facture_validee, 'EXT', 1) AS UNSIGNED))
                FROM facture_validee fv2
                WHERE fv2.reference_facture_validee LIKE CONCAT('%EXT/TNR/', YEAR(dem.date_demande))
              ), 0) AS seq
    FROM ligne_service l
    JOIN demande_service dem ON dem.id = l.demande_id
    WHERE l.categorie = 'DEBOURS'
      AND l.montant_debours IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM facture_validee fv WHERE fv.ligne_service_id = l.id
      )
) vals;

-- ---------------------------------------------------------
-- 5bis. Permission FACTURE_ABONNEMENT_CREATE + attribution
--     SUPER_ADMIN / ADMIN (toutes permissions) et ECONOMAT.
-- ---------------------------------------------------------
SET @perm = (SELECT COUNT(*) FROM permission WHERE code_permission = 'FACTURE_ABONNEMENT_CREATE');
SET @ddl_perm = IF(@perm = 0,
    'INSERT INTO permission (code_permission, nom_permission, module, description)
     VALUES (''FACTURE_ABONNEMENT_CREATE'', ''Créer la facture mensuelle d''''abonnement'',
             ''FACTURE'', ''Créer la facture du mois d''''un abonnement (enfant de la facture générale validée)'')',
    'SELECT 1');
PREPARE stmt FROM @ddl_perm;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Attribution aux SUPER_ADMIN / ADMIN (idempotente)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
JOIN permission p ON p.code_permission = 'FACTURE_ABONNEMENT_CREATE'
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Attribution à l'ECONOMAT (idempotente)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
JOIN permission p ON p.code_permission = 'FACTURE_ABONNEMENT_CREATE'
WHERE r.nom_role = 'ECONOMAT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ---------------------------------------------------------
-- 6. paiement.facture_validee_id : passage en NOT NULL
-- Refuse de modifier si des lignes existent encore avec NULL.
-- ---------------------------------------------------------
SET @nulls = (
    SELECT COUNT(*) FROM paiement WHERE facture_validee_id IS NULL
);
SET @ddl_nn = IF(@nulls = 0,
    'ALTER TABLE paiement MODIFY COLUMN facture_validee_id INT NOT NULL COMMENT "Facture validée liée au paiement (toujours renseignée)"',
    'SELECT "AVERTISSEMENT : des paiements sans facture_validee_id existent ; NOT NULL non appliqué. Corrigez-les puis relancez ce script."');
PREPARE stmt FROM @ddl_nn;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
