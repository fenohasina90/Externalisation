-- =========================================================
-- Migration : cycle de statuts de demande
-- EN_ATTENTE → VALIDATION (auto, paiement du 1er tranche) → EN_COURS (auto) → TERMINEE
-- Remplace APPROUVEE par VALIDATION.
-- =========================================================

-- 1. Rétablir le statut VALIDATION s'il existe déjà sous ce nom
UPDATE statut_demande
SET description = 'Demande validée par le paiement du premier tranche du plan de paiement',
    ordre = 2
WHERE statut = 'VALIDATION';

-- 2. Renommer APPROUVEE → VALIDATION (conserve références historique_demande)
UPDATE statut_demande
SET statut = 'VALIDATION',
    description = 'Demande validée par le paiement du premier tranche du plan de paiement',
    ordre = 2
WHERE statut = 'APPROUVEE';

-- 3. Au cas où VALIDATION n'existait pas (INSERT sans doublon)
INSERT INTO statut_demande (statut, description, ordre)
SELECT 'VALIDATION',
       'Demande validée par le paiement du premier tranche du plan de paiement',
       2
WHERE NOT EXISTS (SELECT 1 FROM statut_demande WHERE statut = 'VALIDATION');