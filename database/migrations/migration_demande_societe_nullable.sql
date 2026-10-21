-- =========================================================
-- Migration : societe_id NULL autorisé sur demande_service
-- Une demande de type « Création d'entreprise ou de société » porte
-- sur une société qui n'existe pas encore : aucun societe_id à lier.
-- Les lignes existantes ont toutes une société : aucune reprise.
-- =========================================================

USE gestion_externalisation;

ALTER TABLE demande_service
    MODIFY societe_id INT NULL
    COMMENT 'NULL si création d’entreprise (société inexistante)';

-- La contrainte fk_demande_societe est conservée : elle ne s'applique
-- qu'aux valeurs non NULL (ON DELETE RESTRICT inchangé).
