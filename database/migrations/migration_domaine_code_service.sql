-- =========================================================
-- Migration : le domaine est désormais lié au SERVICE
-- (et plus à la SOCIÉTÉ) + nouveau code_domaine saisi
-- manuellement à la création d'un domaine.
--
-- À exécuter sur la base avant de relancer l'application
-- (spring.jpa.hibernate.ddl-auto=validate vérifiera le schéma).
-- =========================================================

USE gestion_externalisation;

-- 1. Supprimer le lien société <-> domaine (colonne domaine_id sur societe)
ALTER TABLE societe DROP FOREIGN KEY fk_societe_domaine;
DROP INDEX idx_societe_domaine ON societe;
ALTER TABLE societe DROP COLUMN domaine_id;

-- 2. Ajouter le lien service <-> domaine (colonne domaine_id sur services), nullable
ALTER TABLE services
    ADD COLUMN domaine_id INT NULL AFTER code_service;

ALTER TABLE services
    ADD CONSTRAINT fk_service_domaine
        FOREIGN KEY (domaine_id)
        REFERENCES domaine_service(id)
        ON DELETE RESTRICT;
CREATE INDEX IF NOT EXISTS idx_service_domaine ON services(domaine_id);

-- 3. Ajouter la colonne code_domaine (saisie manuelle) sur domaine_service
ALTER TABLE domaine_service
    ADD COLUMN code_domaine VARCHAR(50) NULL AFTER nom_domaine;

-- 4. Renseigner les domaines existants avec un code provisoire (DOM-0001, ...)
--    L'administrateur pourra les ajuster via la modification de domaine.
UPDATE domaine_service
SET code_domaine = CONCAT('DOM-', LPAD(id, 4, '0'))
WHERE code_domaine IS NULL;

-- 5. Rendre code_domaine obligatoire et unique
ALTER TABLE domaine_service
    MODIFY COLUMN code_domaine VARCHAR(50) NOT NULL;
ALTER TABLE domaine_service
    ADD CONSTRAINT uq_domaine_code UNIQUE (code_domaine);
CREATE INDEX IF NOT EXISTS idx_domaine_code ON domaine_service(code_domaine);
