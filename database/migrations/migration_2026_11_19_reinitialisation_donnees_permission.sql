-- =========================================================
-- Migration : permission « Réinitialisation de données »
-- REINITIALISER_DONNEES (module REINITIALISATION).
-- À exécuter sur la base avant redémarrage de l'application.
-- Attributions : SUPER_ADMIN + ADMIN uniquement.
-- =========================================================

USE gestion_externalisation;

-- 1. Nouvelle permission (si absente)
INSERT INTO permission (code_permission, nom_permission, module, description)
SELECT * FROM (
    SELECT 'REINITIALISER_DONNEES' c, 'Réinitialiser les données' n, 'REINITIALISATION' m,
           'Supprimer définitivement les données métier sélectionnées (irréversible)' d
) src
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code_permission = src.c);

-- 2. SUPER_ADMIN + ADMIN
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN')
  AND p.code_permission = 'REINITIALISER_DONNEES'
  AND NOT EXISTS (SELECT 1 FROM role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
