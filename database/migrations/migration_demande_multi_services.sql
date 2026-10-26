-- =========================================================
-- Migration : demandes multi-services, facturation et statuts par service.
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Contenu :
--  1. Nouvelles tables : facture, ligne_service, historique_ligne_service.
--  2. propos_paiement.ligne_service_id (rattachement du plan au service).
--  3. Reprise : une ligne par demande existante (service/tarif/dates
--     recopiés, statut déduit du statut actuel de la demande), plans
--     rattachés à leur ligne, historique initial des lignes.
-- Notes :
--  - demande_service.service_id / tarif / debut_service / fin_service sont
--    conservés pour compatibilité et seront supprimés en phase 2 (backend
--    basculé sur ligne_service). Ne pas les utiliser pour le nouveau code.
--    (Dans BD_corrige.sql — installation neuve — ils sont déjà supprimés.)
--  - Script réexécutable sans doublon pour les données (5a-5c, 6) ;
--    les sections DDL (1-4, 5d) s'exécutent une seule fois.
-- =========================================================

USE gestion_externalisation;

-- =========================================================
-- 1. Table FACTURE (proforma puis validée)
-- =========================================================

CREATE TABLE IF NOT EXISTS facture (
    id INT AUTO_INCREMENT PRIMARY KEY,

    reference_facture VARCHAR(100) NOT NULL UNIQUE,

    demande_id INT NOT NULL,

    mode_facturation ENUM('UNIQUE', 'PAR_SERVICE') NOT NULL DEFAULT 'PAR_SERVICE'
        COMMENT 'UNIQUE : une facture pour toute la demande ; PAR_SERVICE : une facture par ligne de service',

    montant DECIMAL(15,2) NOT NULL,

    statut VARCHAR(50) NOT NULL DEFAULT 'PROFORMA'
        COMMENT 'PROFORMA : en attente de validation ; VALIDEE : confirmée ; ANNULEE : annulée avec une demande EN_ATTENTE',

    valide_par INT NULL,

    valide_le DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_facture_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_facture_valide_par
        FOREIGN KEY (valide_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_facture_montant
        CHECK (montant >= 0)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_facture_demande
ON facture(demande_id);

CREATE INDEX IF NOT EXISTS idx_facture_reference
ON facture(reference_facture);

-- =========================================================
-- 2. Table LIGNE_SERVICE (un service au sein d'une demande)
-- =========================================================

CREATE TABLE IF NOT EXISTS ligne_service (
    id INT AUTO_INCREMENT PRIMARY KEY,

    demande_id INT NOT NULL,

    service_id INT NOT NULL
        COMMENT 'Service du catalogue (table services)',

    tarif DECIMAL(15,2) NOT NULL
        COMMENT 'Tarif figé à la création de la ligne',

    debut_service DATE NOT NULL,

    fin_service DATE NOT NULL,

    statut VARCHAR(50) NULL
        COMMENT 'VALIDE (facture validée) / EN_ATTENTE (plan établi) / EN_COURS (1er paiement) / TERMINE (date de fin atteinte) ; NULL = en préparation',

    facture_id INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ligne_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ligne_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ligne_facture
        FOREIGN KEY (facture_id)
        REFERENCES facture(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_ligne_dates
        CHECK (fin_service >= debut_service),

    CONSTRAINT chk_ligne_tarif
        CHECK (tarif >= 0)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_ligne_demande
ON ligne_service(demande_id);

CREATE INDEX IF NOT EXISTS idx_ligne_service
ON ligne_service(service_id);

CREATE INDEX IF NOT EXISTS idx_ligne_facture
ON ligne_service(facture_id);

-- =========================================================
-- 3. Table HISTORIQUE_LIGNE_SERVICE
-- =========================================================

CREATE TABLE IF NOT EXISTS historique_ligne_service (
    id INT AUTO_INCREMENT PRIMARY KEY,

    ligne_service_id INT NOT NULL,

    statut VARCHAR(50) NOT NULL,

    commentaire TEXT,

    utilisateur_id INT NULL,

    origine VARCHAR(20) NOT NULL DEFAULT 'MANUEL'
        COMMENT 'MANUEL : action utilisateur ; AUTO : transition automatique',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historique_ligne
        FOREIGN KEY (ligne_service_id)
        REFERENCES ligne_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_historique_ligne_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_historique_ligne_ligne
ON historique_ligne_service(ligne_service_id);

CREATE INDEX IF NOT EXISTS idx_historique_ligne_date
ON historique_ligne_service(created_at);

-- =========================================================
-- 4. Rattachement des plans de paiement aux lignes de service
-- =========================================================

-- =========================================================
-- 4. Rattachement des plans de paiement aux lignes de service
-- (MySQL ne supporte pas IF NOT EXISTS sur ALTER : passage par
-- information_schema pour rester réexécutable.)
-- =========================================================

SET @col_existe = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'propos_paiement'
    AND COLUMN_NAME = 'ligne_service_id'
);
SET @ddl_colonne = IF(
    @col_existe = 0,
    'ALTER TABLE propos_paiement ADD COLUMN ligne_service_id INT NULL AFTER demande_id',
    'SELECT 1'
);
PREPARE stmt_colonne FROM @ddl_colonne;
EXECUTE stmt_colonne;
DEALLOCATE PREPARE stmt_colonne;

SET @fk_existe = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'propos_paiement'
    AND CONSTRAINT_NAME = 'fk_propos_ligne'
);
SET @ddl_fk = IF(
    @fk_existe = 0,
    'ALTER TABLE propos_paiement ADD CONSTRAINT fk_propos_ligne FOREIGN KEY (ligne_service_id) REFERENCES ligne_service(id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt_fk FROM @ddl_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

SET @idx_existe = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'gestion_externalisation'
    AND TABLE_NAME = 'propos_paiement'
    AND INDEX_NAME = 'idx_propos_ligne'
);
SET @ddl_idx = IF(
    @idx_existe = 0,
    'CREATE INDEX idx_propos_ligne ON propos_paiement(ligne_service_id)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @ddl_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- =========================================================
-- 5. Reprise des données existantes (idempotente)
-- =========================================================

-- 5a. Une ligne par demande existante (statut déduit du statut actuel).
INSERT INTO ligne_service (demande_id, service_id, tarif, debut_service, fin_service, statut)
SELECT
    d.id,
    d.service_id,
    d.tarif,
    d.debut_service,
    d.fin_service,
    CASE s.statut
        WHEN 'VALIDATION' THEN 'VALIDE'
        WHEN 'EN_COURS' THEN 'EN_COURS'
        WHEN 'TERMINEE' THEN 'TERMINE'
        ELSE NULL
    END
FROM demande_service d
LEFT JOIN (
    SELECT demande_id, MAX(id) AS max_id
    FROM historique_demande
    GROUP BY demande_id
) dernier ON dernier.demande_id = d.id
LEFT JOIN historique_demande h ON h.id = dernier.max_id
LEFT JOIN statut_demande s ON s.id = h.statut_id
WHERE NOT EXISTS (
    SELECT 1 FROM ligne_service l WHERE l.demande_id = d.id
);

-- 5b. Rattachement des plans existants à leur ligne.
UPDATE propos_paiement p
JOIN ligne_service l ON l.demande_id = p.demande_id
SET p.ligne_service_id = l.id
WHERE p.ligne_service_id IS NULL;

-- 5c. Historique initial des lignes reprises (traçabilité de la migration).
INSERT INTO historique_ligne_service (ligne_service_id, statut, commentaire, origine)
SELECT
    l.id,
    l.statut,
    'Statut repris de la demande lors de la migration multi-services',
    'AUTO'
FROM ligne_service l
WHERE l.statut IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM historique_ligne_service h WHERE h.ligne_service_id = l.id
);

-- =========================================================
-- 5d. Suppression des colonnes mono-service (reprises ci-dessus).
-- Exécutée une seule fois (sections 5a-5c et 6 réexécutables).
-- =========================================================

ALTER TABLE demande_service
    DROP FOREIGN KEY fk_demande_service,
    DROP INDEX idx_demande_service,
    DROP COLUMN service_id,
    DROP COLUMN tarif,
    DROP COLUMN debut_service,
    DROP COLUMN fin_service;

-- =========================================================
-- 6. Permissions de facturation
-- =========================================================

INSERT INTO permission (code_permission, nom_permission, module, description)
SELECT * FROM (
    SELECT 'FACTURE_VIEW' c, 'Consulter les factures' n, 'FACTURE' m, 'Voir les factures proforma et validées' d
    UNION SELECT 'FACTURE_CREATE', 'Créer une facture proforma', 'FACTURE', 'Générer les factures proforma d''une demande'
    UNION SELECT 'FACTURE_VALIDATE', 'Valider une facture', 'FACTURE', 'Valider une facture proforma'
) src
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code_permission = src.c);

-- SUPER_ADMIN et ADMIN : toutes les nouvelles permissions.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN')
AND p.code_permission IN ('FACTURE_VIEW', 'FACTURE_CREATE', 'FACTURE_VALIDATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ACCUEIL : consultation + génération (validation réservée aux administrateurs).
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ACCUEIL'
AND p.code_permission IN ('FACTURE_VIEW', 'FACTURE_CREATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ECONOMAT : consultation seule.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ECONOMAT'
AND p.code_permission = 'FACTURE_VIEW'
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
