-- =========================================================
-- Migration : societe_id nullable dans client_responsable.
-- Une demande de création d'entreprise n'a pas de société : son
-- responsable peut être rattaché à aucune société (contexte CREATION).
-- =========================================================

USE gestion_externalisation;

ALTER TABLE client_responsable
    DROP FOREIGN KEY fk_responsable_societe;

ALTER TABLE client_responsable
    MODIFY COLUMN societe_id INT NULL;

ALTER TABLE client_responsable
    ADD CONSTRAINT fk_responsable_societe
        FOREIGN KEY (societe_id)
        REFERENCES societe(id)
        ON DELETE CASCADE;
