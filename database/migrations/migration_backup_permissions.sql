-- =========================================================
-- Migration : permissions du module de sauvegarde hors ligne
-- BACKUP_VIEW / BACKUP_CREATE / BACKUP_DELETE / BACKUP_RESTORE (module SAUVEGARDE).
-- À exécuter sur la base avant redémarrage de l'application.
-- Attributions :
--   SUPER_ADMIN, ADMIN : toutes
--   ACCUEIL, ECONOMAT  : aucune (action réservée à l'administration)
-- =========================================================

USE gestion_externalisation;

-- 1. Nouvelles permissions (si absentes)
INSERT INTO permission (code_permission, nom_permission, module, description)
SELECT * FROM (
    SELECT 'BACKUP_VIEW' c, 'Consulter les sauvegardes' n, 'SAUVEGARDE' m, 'Lister et télécharger les sauvegardes de la base' d
    UNION SELECT 'BACKUP_CREATE', 'Créer une sauvegarde', 'SAUVEGARDE', 'Générer une sauvegarde manuelle ou ponctuelle'
    UNION SELECT 'BACKUP_DELETE', 'Supprimer une sauvegarde', 'SAUVEGARDE', 'Supprimer une archive de sauvegarde'
    UNION SELECT 'BACKUP_RESTORE', 'Restaurer une sauvegarde', 'SAUVEGARDE', 'Restaurer la base depuis un fichier de sauvegarde'
) src
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code_permission = src.c);

-- 2. SUPER_ADMIN + ADMIN : toutes les permissions de sauvegarde
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN')
  AND p.code_permission IN ('BACKUP_VIEW', 'BACKUP_CREATE', 'BACKUP_DELETE', 'BACKUP_RESTORE')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
