-- =========================================================
-- Migration : pieces jointes rattachables a un paiement
-- ---------------------------------------------------------
-- Ajoute paiement_id (nullable) sur piece_jointe afin de
-- pouvoir joindre des justificatifs lors de l'enregistrement
-- d'un paiement, sans remettre en cause les documents de
-- demande existants (paiement_id NULL).
--
-- Les fichiers uploades sont acceptes sans restriction de
-- type (pdf, images, tableurs, documents bureautiques,
-- archives, texte, ...). Seule la taille maximale reste
-- controlee (configuration TAILLE_MAX_FICHIER).
-- =========================================================

ALTER TABLE piece_jointe
    ADD COLUMN paiement_id INT NULL AFTER demande_id;

ALTER TABLE piece_jointe
    ADD CONSTRAINT fk_piece_paiement
        FOREIGN KEY (paiement_id)
        REFERENCES paiement(id)
        ON DELETE CASCADE;

CREATE INDEX idx_piece_paiement
ON piece_jointe(paiement_id);
