-- =========================================================
-- Migration : dossier « Transaction immobilière »
-- Ajoute un type de service dédié et la fiche de collecte des
-- informations de la transaction.
--
-- Les informations déjà portées par la demande / le client ne sont
-- PAS dupliquées ici :
--   - date                 -> demande_service.date_demande
--   - commercial + N° IM   -> commercial.nom_commercial / immatricule
--   - nom du client        -> societe.nom_societe
--   - contact / mail / fb  -> societe.contact / email / fb
--   - adresse              -> societe.adresse
--
-- Une seule fiche par demande (UNIQUE sur demande_id), comme pour
-- le dossier de création d'entreprise. Les signatures papier ne sont
-- pas stockées.
-- =========================================================

USE gestion_externalisation;

-- 1. Autoriser le nouveau type de service.
ALTER TABLE services
    DROP CONSTRAINT chk_service_type;

ALTER TABLE services
    ADD CONSTRAINT chk_service_type
        CHECK (type_service IN ('STANDARD', 'CREATION', 'TRANSACTION_IMMOBILIERE'));

ALTER TABLE services
    MODIFY COLUMN type_service VARCHAR(30) NOT NULL DEFAULT 'STANDARD'
        COMMENT 'STANDARD, CREATION ou TRANSACTION_IMMOBILIERE';

-- 2. Fiche principale de la transaction immobilière.
CREATE TABLE IF NOT EXISTS dossier_transaction_immobiliere (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Demande d'origine (1 fiche maximum par demande).
    demande_id INT NOT NULL UNIQUE,

    -- Nom du propriétaire précédent.
    nom_proprietaire_precedent VARCHAR(255) NULL,

    -- 1/ Action(s) : la fiche papier utilise des cases à cocher.
    action_achat BOOLEAN NOT NULL DEFAULT FALSE,
    action_vente BOOLEAN NOT NULL DEFAULT FALSE,
    action_location BOOLEAN NOT NULL DEFAULT FALSE,
    action_regularisation_papiers BOOLEAN NOT NULL DEFAULT FALSE,
    action_gestion_immobiliere BOOLEAN NOT NULL DEFAULT FALSE,
    action_autre BOOLEAN NOT NULL DEFAULT FALSE,
    action_autre_precision VARCHAR(255) NULL COMMENT 'Précision si action « Autre »',

    -- 2/ Objet.
    objet ENUM('TERRAIN', 'MAISON', 'AUTRE') NULL,
    objet_autre_precision VARCHAR(255) NULL COMMENT 'Précision si objet « Autre »',

    -- Bloc ACHAT.
    achat_mesure_m2 DECIMAL(15, 2) NULL,
    achat_position VARCHAR(255) NULL COMMENT 'Position / plan',
    achat_axe_emplacement VARCHAR(255) NULL,
    achat_montant DECIMAL(15, 2) NULL,
    achat_permis_construire BOOLEAN NULL COMMENT 'Si maison : OUI / NON',

    -- Bloc VENTE.
    vente_mesure_m2 DECIMAL(15, 2) NULL,
    vente_position VARCHAR(255) NULL COMMENT 'Position / plan',
    vente_axe_emplacement VARCHAR(255) NULL,
    vente_prise_photo BOOLEAN NULL COMMENT 'OUI / NON',
    vente_visite BOOLEAN NULL COMMENT 'OUI / NON',
    vente_montant DECIMAL(15, 2) NULL,
    vente_permis_construire BOOLEAN NULL COMMENT 'Si maison : OUI / NON',

    -- Régularisation de papiers.
    regularisation_type_terrain ENUM('COMMUNAL_DOMAINE', 'PRIVE', 'HERITAGE_DON') NULL,
    regularisation_situation_terrain ENUM('TITRE_BORNE', 'RECONNAISSANCE') NULL,

    -- 5/ Liste des documents existants.
    document_pv_reconnaissance BOOLEAN NOT NULL DEFAULT FALSE,
    document_numero_demande_acquisition BOOLEAN NOT NULL DEFAULT FALSE,
    document_acte_vente_privee_communale BOOLEAN NOT NULL DEFAULT FALSE,
    document_situation_juridique BOOLEAN NOT NULL DEFAULT FALSE,
    document_acte_notoriete BOOLEAN NOT NULL DEFAULT FALSE,

    -- Observation libre.
    observations TEXT NULL,

    -- Bas de fiche.
    nombre_commerciaux_terrain INT NULL COMMENT 'Nombre de commerciaux sur terrain voulus',
    commission_cn DECIMAL(15, 2) NULL,
    prestation_abonnement_mensuel BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_dti_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_dti_action
        CHECK (action_achat OR action_vente OR action_location
            OR action_regularisation_papiers OR action_gestion_immobiliere
            OR action_autre),

    CONSTRAINT chk_dti_action_autre
        CHECK (action_autre = FALSE OR action_autre_precision IS NOT NULL),

    CONSTRAINT chk_dti_objet_autre
        CHECK (objet IS NULL OR objet <> 'AUTRE' OR objet_autre_precision IS NOT NULL),

    CONSTRAINT chk_dti_mesures
        CHECK ((achat_mesure_m2 IS NULL OR achat_mesure_m2 >= 0)
            AND (vente_mesure_m2 IS NULL OR vente_mesure_m2 >= 0)),

    CONSTRAINT chk_dti_montants
        CHECK ((achat_montant IS NULL OR achat_montant >= 0)
            AND (vente_montant IS NULL OR vente_montant >= 0)
            AND (commission_cn IS NULL OR commission_cn >= 0)),

    CONSTRAINT chk_dti_commerciaux
        CHECK (nombre_commerciaux_terrain IS NULL OR nombre_commerciaux_terrain >= 0)
) ENGINE=InnoDB;
