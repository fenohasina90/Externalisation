-- ==================================================================
-- Migration 2026-11-18 : snapshot du reste a payer sur chaque paiement
-- ==================================================================
-- Objectif : le reste a payer affiche a la consultation du recu / detail
-- est toujours celui qui etait enregistre au moment de l'encaissement,
-- quel que soit le solde courant de la facture ensuite.
-- ==================================================================

ALTER TABLE paiement
    ADD COLUMN reste_a_payer DECIMAL(15,2) NULL COMMENT 'Reste a payer sur la facture au moment du paiement (snapshot)';

-- Backfill des lignes existantes : le reste est recalculable a partir
-- des paiements actifs anterieurs et celui-ci, ordonnes par id.
UPDATE paiement p
JOIN facture_validee fv ON fv.id = p.facture_validee_id
SET p.reste_a_payer = GREATEST(0, fv.montant - (
        SELECT COALESCE(SUM(q.montant_paye), 0)
        FROM paiement q
        WHERE q.facture_validee_id = fv.id
          AND q.actif = 1
          AND q.id <= p.id
    ));
