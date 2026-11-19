-- ==================================================================
-- Migration 2026-11-18 (b) : correction du snapshot « montant deja paye »
-- ==================================================================
-- Objectif : le montant deja paye = somme des paiements actifs
--   AVANT celui affiche sur la meme facture validée (celui-ci exclu).
-- La migration précédente incluait par erreur ce paiement lui-meme.
-- ==================================================================

UPDATE paiement p
JOIN facture_validee fv ON fv.id = p.facture_validee_id
SET p.montant_deja_paye = (
        SELECT COALESCE(SUM(q.montant_paye), 0)
        FROM paiement q
        WHERE q.facture_validee_id = fv.id
          AND q.actif = 1
          AND q.id < p.id
    );
