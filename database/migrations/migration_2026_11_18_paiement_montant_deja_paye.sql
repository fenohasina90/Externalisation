-- ==================================================================
-- Migration 2026-11-18 (2e) : snapshot du montant deja paye associe
-- a chaque paiement
-- ==================================================================
-- Objectif : consultation du recu / detail d'un paiement affiche
--  toujours le montant deja paye enregistre au moment de ce paiement,
-- pas la somme courante (qui inclurait les paiements apparus plus tard).
-- ==================================================================

ALTER TABLE paiement
    ADD COLUMN montant_deja_paye DECIMAL(15,2) NULL COMMENT 'Somme deja payee sur la facture a ce paiement (snapshot)';

-- Backfill : somme des paiements actifs sur la meme facture validee,
-- y compris ce paiement, ordonnes par id.
UPDATE paiement p
JOIN facture_validee fv ON fv.id = p.facture_validee_id
SET p.montant_deja_paye = (
        SELECT COALESCE(SUM(q.montant_paye), 0)
        FROM paiement q
        WHERE q.facture_validee_id = fv.id
          AND q.actif = 1
          AND q.id <= p.id
    );
