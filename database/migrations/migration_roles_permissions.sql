-- =========================================================
-- Migration : refonte des accès par rôle (SUPER_ADMIN / ACCUEIL / ECONOMAT)
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================

-- 1. Nouvelles permissions (si absentes)
INSERT INTO permission (code_permission, nom_permission, module, description)
SELECT * FROM (
    SELECT 'DASHBOARD_DEMANDE_VIEW' c, 'Consulter les statistiques de demandes' n, 'DASHBOARD' m, 'Voir les blocs demandes du tableau de bord' d
    UNION SELECT 'DASHBOARD_PAIEMENT_VIEW', 'Consulter les statistiques de paiements', 'DASHBOARD', 'Voir les blocs paiement du tableau de bord'
    UNION SELECT 'MODE_PAIEMENT_VIEW', 'Consulter les modes de paiement', 'PAIEMENT', 'Voir les modes de paiement'
    UNION SELECT 'MODE_PAIEMENT_UPDATE', 'Gérer les modes de paiement', 'PAIEMENT', 'Créer / modifier / désactiver les modes de paiement'
    UNION SELECT 'RAPPORT_DEMANDE_VIEW', 'Consulter les rapports liés aux demandes', 'RAPPORT', 'Rapports général, par client et des demandes'
    UNION SELECT 'RAPPORT_DEMANDE_EXPORT', 'Exporter les rapports liés aux demandes', 'RAPPORT', 'Export PDF / Excel / CSV des rapports demandes'
    UNION SELECT 'RAPPORT_PAIEMENT_VIEW', 'Consulter les rapports liés aux paiements', 'RAPPORT', 'Rapports financier, par service et des paiements'
    UNION SELECT 'RAPPORT_PAIEMENT_EXPORT', 'Exporter les rapports liés aux paiements', 'RAPPORT', 'Export PDF / Excel / CSV des rapports paiements'
    UNION SELECT 'DOCUMENT_DEMANDE_VIEW', 'Consulter les documents des demandes', 'DOCUMENT', 'Pièces jointes et PDF liés aux demandes'
    UNION SELECT 'DOCUMENT_DEMANDE_UPLOAD', 'Ajouter un document de demande', 'DOCUMENT', 'Télécharger une pièce jointe à une demande'
    UNION SELECT 'DOCUMENT_DEMANDE_DELETE', 'Supprimer un document de demande', 'DOCUMENT', 'Supprimer une pièce jointe de demande'
    UNION SELECT 'DOCUMENT_PAIEMENT_VIEW', 'Consulter les documents de paiement', 'DOCUMENT', 'Reçus de paiement et documents financiers'
) src
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code_permission = src.c);

-- 2. SUPER_ADMIN reçoit toutes les nouvelles permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 3. Réglage des attributions ACCUEIL
--    3a. Retraits
DELETE rp FROM role_permission rp
JOIN role_utilisateur r ON r.id = rp.role_id
JOIN permission p ON p.id = rp.permission_id
WHERE r.nom_role = 'ACCUEIL'
  AND p.code_permission IN ('COMMERCIAL_VIEW', 'TARIF_VIEW',
                            'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
                            'REPORT_VIEW', 'REPORT_EXPORT');

--    3b. Ajouts
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ACCUEIL'
  AND p.code_permission IN (
      'DASHBOARD_VIEW', 'DASHBOARD_DEMANDE_VIEW',
      'CLIENT_VIEW', 'CLIENT_CREATE', 'CLIENT_UPDATE',
      'RESPONSABLE_VIEW', 'RESPONSABLE_CREATE', 'RESPONSABLE_UPDATE', 'RESPONSABLE_DELETE',
      'DOMAINE_VIEW', 'DOMAINE_CREATE', 'DOMAINE_UPDATE', 'DOMAINE_DELETE',
      'SERVICE_VIEW', 'SERVICE_CREATE', 'SERVICE_UPDATE',
      'DEMANDE_VIEW', 'DEMANDE_CREATE', 'DEMANDE_UPDATE', 'DEMANDE_STATUS_UPDATE',
      'RAPPORT_DEMANDE_VIEW', 'RAPPORT_DEMANDE_EXPORT',
      'DOCUMENT_DEMANDE_VIEW', 'DOCUMENT_DEMANDE_UPLOAD', 'DOCUMENT_DEMANDE_DELETE')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 4. Réglage des attributions ECONOMAT
--    4a. Retraits
DELETE rp FROM role_permission rp
JOIN role_utilisateur r ON r.id = rp.role_id
JOIN permission p ON p.id = rp.permission_id
WHERE r.nom_role = 'ECONOMAT'
  AND p.code_permission IN ('REPORT_VIEW', 'REPORT_EXPORT',
                            'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD');

--    4b. Ajouts
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ECONOMAT'
  AND p.code_permission IN (
      'DASHBOARD_VIEW', 'DASHBOARD_PAIEMENT_VIEW',
      'CLIENT_VIEW',
      'DEMANDE_VIEW',
      'PAIEMENT_VIEW', 'PAIEMENT_CREATE', 'PAIEMENT_UPDATE',
      'MODE_PAIEMENT_VIEW', 'MODE_PAIEMENT_UPDATE',
      'RAPPORT_PAIEMENT_VIEW', 'RAPPORT_PAIEMENT_EXPORT',
      'DOCUMENT_PAIEMENT_VIEW')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);