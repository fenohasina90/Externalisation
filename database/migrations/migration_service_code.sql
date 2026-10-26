-- =========================================================
-- Migration : code unique auto-généré sur services (SRV-0001, …)
-- Recherche possible par nom ou par code ; lecture par code via
-- GET /api/services/code/{code}.
-- =========================================================

USE gestion_externalisation;

-- 1. Colonne nullable pour autoriser le remplissage
ALTER TABLE services
    ADD COLUMN code_service VARCHAR(50) NULL AFTER nom_service;

-- 2. Backfill : SRV-0001, SRV-0002, … dans l'ordre des identifiants
SET @seq := 0;
UPDATE services
SET code_service = CONCAT('SRV-', LPAD((@seq := @seq + 1), 4, '0'))
WHERE code_service IS NULL
ORDER BY id ASC;

-- 3. Passage en NOT NULL + contrainte UNIQUE
ALTER TABLE services
    MODIFY code_service VARCHAR(50) NOT NULL;

ALTER TABLE services
    ADD UNIQUE KEY uq_services_code (code_service);

CREATE INDEX idx_service_code
ON services(code_service);
