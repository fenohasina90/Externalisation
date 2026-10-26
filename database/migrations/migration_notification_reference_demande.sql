-- =========================================================
-- Migration : référence de la demande dans les notifications
-- Ajoute la référence de la demande aux notifications d'échéance
-- proche et de paiement en retard déjà enregistrées, sans modifier
-- leur statut de lecture ni recalculer le délai affiché.
-- =========================================================

SET NAMES utf8mb4;

UPDATE notification n
JOIN echeance_paiement e ON e.id = n.reference_id
JOIN propos_paiement pp ON pp.id = e.propos_paiement_id
JOIN demande_service d ON d.id = pp.demande_id
SET n.message = CONCAT(
        n.message,
        ' Référence de la demande : ',
        COALESCE(d.reference_demande, '-'),
        '.'
    )
WHERE n.type_notification IN ('PAIEMENT_RETARD', 'ECHEANCE_PROCHE')
  AND n.reference_type = 'ECHEANCE'
  AND n.message NOT LIKE '%Référence de la demande :%';
