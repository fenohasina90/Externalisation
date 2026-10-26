-- =========================================================
-- Migration : dates des notifications au format JJ/MM/AAAA
-- Reformate les messages « paiement en retard » déjà enregistrés.
-- Le code produit désormais directement le format français ; cette
-- migration aligne les notifications historiques sans les supprimer
-- (statut lu/non lu conservé).
-- =========================================================

SET NAMES utf8mb4;

UPDATE notification n
JOIN echeance_paiement e ON e.id = n.reference_id
JOIN propos_paiement pp ON pp.id = e.propos_paiement_id
JOIN demande_service d ON d.id = pp.demande_id
LEFT JOIN societe s ON s.id = d.societe_id
SET n.message = CONCAT(
        '⛔ L''échéance n°', e.numero_echeance,
        ' de la société ', COALESCE(s.nom_societe, 'client'),
        ' (', e.montant_attendu, ' (MGA)) est en retard depuis le ',
        DATE_FORMAT(e.date_echeance, '%d/%m/%Y'), '.'
    )
WHERE n.type_notification = 'PAIEMENT_RETARD'
  AND n.reference_type = 'ECHEANCE';
