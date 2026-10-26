-- =========================================================
-- Migration : type de service (STANDARD vs CREATION).
-- Le dossier de création d'entreprise ou de société se rattache
-- aux demandes dont le service est de type CREATION (colonne
-- explicite, plus robuste que la détection sur le nom).
-- =========================================================

USE gestion_externalisation;

-- 1. Colonne avec défaut STANDARD
ALTER TABLE services
    ADD COLUMN type_service VARCHAR(30) NOT NULL DEFAULT 'STANDARD' AFTER code_service;

-- 2. Backfill : le service de création existant (toutes variantes
--    d'accents/casse confondues)
UPDATE services
SET type_service = 'CREATION'
WHERE LOWER(nom_service) LIKE '%creation%d%entreprise%'
   OR LOWER(nom_service) LIKE '%creation%de%societe%'
   OR nom_service LIKE '%création%d%entreprise%'
   OR nom_service LIKE '%Création%d%entreprise%';

-- 3. Contrainte de valeurs
ALTER TABLE services
    ADD CONSTRAINT chk_service_type
        CHECK (type_service IN ('STANDARD', 'CREATION'));

CREATE INDEX idx_service_type
ON services(type_service);
