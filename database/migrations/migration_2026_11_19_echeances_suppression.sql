-- =========================================================
-- Migration : suppression du module legacy « plan + échéance de paiement »
-- Les tables propos_paiement et echeance_paiement ne sont plus utilisées ;
-- les encaissements sont liés à factures validées (cf. 2026_11_10_paiement_facture_validee.sql).
--
-- À exécuter APRÈS la migration 2026_11_10_paiement_facture_validee.sql.
-- ATTENTION : DESTRUCTIF — sauvegarder `gestion_externalisation` avant d'exécuter.
-- =========================================================

USE gestion_externalisation;

-- 1. Paiement : colonne echeance_id (NULL de fait depuis la migration 2026_11_10)
ALTER TABLE paiement DROP FOREIGN KEY fk_paiement_echeance;
ALTER TABLE paiement DROP COLUMN echeance_id;

-- 2. echeance_paiement
DROP TABLE IF EXISTS echeance_paiement;

-- 3. propos_paiement
DROP TABLE IF EXISTS propos_paiement;
