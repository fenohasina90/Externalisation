-- =========================================================
-- Migration : suppression de la colonne tarif_service (catalogue).
-- Le tarif catalogue n'est plus géré : seul le montant de chaque
-- demande (demande_service.tarif) est conservé.
-- L'historique des tarifs (table historique_tarif_service) est
-- également supprimé : il ne contenait que des doublons du tarif.
-- =========================================================

USE gestion_externalisation;

DROP TABLE IF EXISTS historique_tarif_service;

ALTER TABLE services
    DROP COLUMN tarif_service;
