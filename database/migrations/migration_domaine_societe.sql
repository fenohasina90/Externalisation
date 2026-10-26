-- =========================================================
-- Migration : le domaine est désormais lié à la SOCIÉTÉ,
-- et plus au SERVICE.
-- À exécuter sur la base avant de relancer l'application
-- (spring.jpa.hibernate.ddl-auto=validate vérifiera le schéma).
-- =========================================================

-- 1. Garantir qu'au moins un domaine existe
INSERT INTO domaine_service (nom_domaine, description, actif)
SELECT 'Général', 'Domaine par défaut (migration)', TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM domaine_service);

-- 2. Ajouter la colonne domaine_id à societe (nullable temporairement)
ALTER TABLE societe ADD COLUMN domaine_id INT NULL AFTER chiffre_affaire_mensuel;

-- 3. Affecter chaque société au domaine le plus fréquent des services
--    utilisés dans ses demandes (le plus proche de l'ancien modèle).
UPDATE societe s
SET s.domaine_id = (
    SELECT sv.domaine_id
    FROM demande_service ds
    JOIN services sv ON sv.id = ds.service_id
    WHERE ds.societe_id = s.id AND sv.domaine_id IS NOT NULL
    GROUP BY sv.domaine_id
    ORDER BY COUNT(*) DESC
    LIMIT 1
)
WHERE s.domaine_id IS NULL;

-- 4. Fallback : domaine par défaut (actif de préférence) pour les
--    sociétés sans aucune demande / service.
UPDATE societe
SET domaine_id = (SELECT id FROM domaine_service ORDER BY actif DESC, id ASC LIMIT 1)
WHERE domaine_id IS NULL;

-- 5. Rendre la colonne obligatoire + FK + index
ALTER TABLE societe MODIFY domaine_id INT NOT NULL;
ALTER TABLE societe ADD CONSTRAINT fk_societe_domaine
    FOREIGN KEY (domaine_id) REFERENCES domaine_service(id) ON DELETE RESTRICT;
CREATE INDEX idx_societe_domaine ON societe(domaine_id);

-- 6. Retirer la colonne domaine_id du service
--    (l'index/FK éventuels sur cette colonne sont supprimés automatiquement)
ALTER TABLE services DROP COLUMN domaine_id;