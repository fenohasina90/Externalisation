-- =========================================================
-- Correctif : rendre NULL les colonnes EXTERNALISATION
-- de ligne_service (service/tarif/dates) pour les lignes DEBOURS
-- Date : 2026-11-09
-- =========================================================
-- Symptôme (2026-11-09 13:09) :
--   Column 'debut_service' cannot be null
--   insert into ligne_service (...) — ligne DEBOURS sans dates
--
-- Cause : l'entité JPA accepte NULL (Service/tarif/dates
-- nullables) mais la table ligne_service est restée en
-- NOT NULL (migration 2026_11_09 non appliquée, ou
-- BD_corrige.sql ré-importé après la migration).
--
-- Action : passer les 4 colonnes en NULL. Idempotent
-- (MODIFY COLUMN vers NULL peut être relancé sans effet).
-- =========================================================

USE gestion_externalisation;

-- Forcer NULL (service, tarif, début, fin) — DEBOURS sans catalogue.
ALTER TABLE ligne_service MODIFY COLUMN service_id INT NULL;
ALTER TABLE ligne_service MODIFY COLUMN tarif DECIMAL(15,2) NULL;
ALTER TABLE ligne_service MODIFY COLUMN debut_service DATE NULL;
ALTER TABLE ligne_service MODIFY COLUMN fin_service DATE NULL;

-- Vérification (à exécuter ensuite, doit afficher 4 lignes avec IS_NULLABLE=YES) :
-- SELECT COLUMN_NAME, IS_NULLABLE
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = 'gestion_externalisation'
--   AND TABLE_NAME = 'ligne_service'
--   AND COLUMN_NAME IN ('service_id', 'tarif', 'debut_service', 'fin_service');
