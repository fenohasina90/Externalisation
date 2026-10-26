-- =========================================================
-- Migration : permissions du dossier de création d'entreprise
-- DOSSIER_VIEW / DOSSIER_CREATE / DOSSIER_UPDATE (module DOSSIER).
-- À exécuter sur la base avant redémarrage de l'application,
-- après migration_dossier_creation_entreprise.sql.
-- Attributions :
--   SUPER_ADMIN, ADMIN : toutes
--   ACCUEIL           : toutes (saisie du dossier avec la demande)
--   ECONOMAT          : lecture seule
-- =========================================================

USE gestion_externalisation;

-- 1. Nouvelles permissions (si absentes)
INSERT INTO permission (code_permission, nom_permission, module, description)
SELECT * FROM (
    SELECT 'DOSSIER_VIEW' c, 'Consulter le dossier de création' n, 'DOSSIER' m, 'Voir la fiche de création d’entreprise ou de société' d
    UNION SELECT 'DOSSIER_CREATE', 'Créer le dossier de création', 'DOSSIER', 'Saisir la fiche de création d’entreprise ou de société'
    UNION SELECT 'DOSSIER_UPDATE', 'Modifier le dossier de création', 'DOSSIER', 'Modifier la fiche et les associés'
) src
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code_permission = src.c);

-- 2. SUPER_ADMIN + ADMIN : toutes les permissions dossier
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN')
  AND p.code_permission IN ('DOSSIER_VIEW', 'DOSSIER_CREATE', 'DOSSIER_UPDATE')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 3. ACCUEIL : toutes les permissions dossier
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ACCUEIL'
  AND p.code_permission IN ('DOSSIER_VIEW', 'DOSSIER_CREATE', 'DOSSIER_UPDATE')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 4. ECONOMAT : lecture seule
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role = 'ECONOMAT'
  AND p.code_permission IN ('DOSSIER_VIEW')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
