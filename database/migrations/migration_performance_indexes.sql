-- =========================================================
-- Migration : index de performance pour les listes et le dashboard
-- À exécuter une seule fois sur la base existante.
-- =========================================================

USE gestion_externalisation;

-- Liste des demandes : filtre actif + tri par date.
CREATE INDEX IF NOT EXISTS idx_demande_actif_date
    ON demande_service (actif, date_demande);

-- Échéances en retard / proches : filtre sur la date et le reste dû.
CREATE INDEX IF NOT EXISTS idx_echeance_date_paiement
    ON echeance_paiement (date_echeance, montant_paye, montant_attendu);

-- Historique des paiements : filtre actif + tri par date.
CREATE INDEX IF NOT EXISTS idx_paiement_actif_date
    ON paiement (actif, date_paiement);

-- Sommes payées par demande : filtre actif + regroupement.
CREATE INDEX IF NOT EXISTS idx_paiement_actif_demande
    ON paiement (actif, demande_id);

-- Filtre des demandes par service.
CREATE INDEX IF NOT EXISTS idx_ligne_demande_service
    ON ligne_service (demande_id, service_id);

-- Déduplication des notifications d'échéance.
CREATE INDEX IF NOT EXISTS idx_notification_dedup
    ON notification (utilisateur_id, type_notification, reference_id);
