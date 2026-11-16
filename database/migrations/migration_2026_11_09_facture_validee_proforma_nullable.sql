-- =========================================================
-- Correctif : facture_validee.facture_proforma_id NULL
-- (le débours n'a pas de facture proforma)
-- Date : 2026-11-09
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Une facture validée peut exister sans facture proforma
-- (cas DEBOURS). L'unicité MySQL autorise plusieurs NULL.
-- =========================================================

USE gestion_externalisation;

ALTER TABLE facture_validee MODIFY COLUMN facture_proforma_id INT NULL;
