-- =========================================================
-- Migration : renomme la colonne tarif de demande_service
-- tarif_service_applique -> tarif
-- La valeur reste l'instantané du tarif du service au moment
-- de la création de la demande (snapshot, insensible aux
-- modifications ultérieures du tarif du service).
-- =========================================================

ALTER TABLE demande_service
    CHANGE COLUMN tarif_service_applique tarif DECIMAL(15,2) NOT NULL;