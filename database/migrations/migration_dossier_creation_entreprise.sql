-- =========================================================
-- Dossier « Création d'entreprise ou de société »
-- Fiche papier du cabinet : informations à enregistrer quand le
-- service de la demande est de type création d'entreprise/société.
--
-- Tables créées :
--   1. dossier_creation_entreprise : fiche principale (1 ligne par
--      demande_service, lien UNIQUE vers demande_service.id).
--   2. dossier_associe              : associés, 1 ligne par associé (N).
-- (Pas de table des signatures : les signatures ne sont ni affichées
-- ni enregistrées.)
--
-- Règle métier (niveau applicatif) : un dossier n'est créé que si le
-- service de la demande est « Création d'entreprise ou de société »,
-- et une demande ne porte qu'un seul dossier (UNIQUE sur demande_id).
-- La date de la fiche et le N° IM ne sont PAS stockés ici : ils sont
-- lus depuis la demande d'origine (demande_service.date_demande et
-- l'immatricule du commercial via demande_service.commercial_id).
-- Si la forme juridique est une société (tout sauf INDIVIDUELLE),
-- les informations des associés (dossier_associe) sont obligatoires.
-- =========================================================

USE gestion_externalisation;

-- ---------------------------------------------------------
-- 1. Fiche principale
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS dossier_creation_entreprise (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Demande d'origine (1 dossier maximum par demande).
    -- La date de la fiche = demande_service.date_demande et le N° IM =
    -- l'immatricule du commercial : lus par jointure, non dupliqués ici.
    demande_id INT NOT NULL UNIQUE,

    -- Forme juridique : Individuelle, SARL, SARLU, SA, Autre à préciser.
    forme_juridique ENUM('INDIVIDUELLE', 'SARL', 'SARLU', 'SA', 'AUTRE') NOT NULL,
    forme_juridique_precision VARCHAR(100) NULL COMMENT 'Précision si AUTRE',

    -- Identification de la future société.
    denomination VARCHAR(255) NOT NULL COMMENT 'Dénomination',
    siege_social_actuel VARCHAR(255) NULL,
    capital_social DECIMAL(15, 2) NULL COMMENT 'Capital social si société',
    nombre_associes_declare INT NULL COMMENT 'Nombre d’associé déclaré',

    -- Gérance.
    nom_gerant VARCHAR(150) NULL,
    fonction_gerant VARCHAR(100) NULL COMMENT 'Fonction du gérant',
    location_gerance BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Case « Location de gérance »',

    -- Activité.
    secteur_activite VARCHAR(150) NULL COMMENT 'SECTEUR D’ACTIVITÉ',
    autorisation_speciale VARCHAR(255) NULL COMMENT 'Autorisation spéciale',

    -- Adresse du siège et de l’établissement (champs libres de la fiche).
    adresse_siege VARCHAR(255) NULL COMMENT 'Adresse',
    ville VARCHAR(100) NULL,
    commune VARCHAR(100) NULL,
    district VARCHAR(100) NULL,
    region VARCHAR(100) NULL,

    -- Hébergement du siège : domicile du gérant, location au cabinet, autre.
    type_hebergement ENUM('DOMICILE_GERANT', 'LOCATION_CABINET', 'AUTRE') NULL,
    hebergement_precision VARCHAR(255) NULL COMMENT 'Champ libre « Autres »',
    contrat_bail BOOLEAN NULL COMMENT 'Case « contrat de bail » (NULL = non renseigné)',
    contrat_bail_detail VARCHAR(255) NULL,

    -- Objet social et démarrage.
    objet_social TEXT NULL COMMENT 'Principales activités de l’objet social',
    date_souhaitee_debut DATE NULL COMMENT 'Date souhaitée pour le début de l’activité',
    regime_activite ENUM('PERMANENTE', 'SAISONNIERE', 'AMBULANT') NULL,
    activites_etablissement TEXT NULL COMMENT 'Activités dans l’établissement / succursale / filiale',

    -- Situation fiscale et personnelle.
    observations TEXT NULL,
    assujettissement_tva VARCHAR(100) NULL COMMENT 'Assujettissement à la TVA',
    autre_societe VARCHAR(255) NULL COMMENT 'Avez-vous d’autre société ?',
    situation_matrimoniale VARCHAR(50) NULL,
    regime_imposition VARCHAR(100) NULL COMMENT 'Régime d’imposition',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_dossier_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_dossier_capital
        CHECK (capital_social IS NULL OR capital_social >= 0),

    CONSTRAINT chk_dossier_associes
        CHECK (nombre_associes_declare IS NULL OR nombre_associes_declare >= 0),

    CONSTRAINT chk_dossier_forme_autre
        CHECK (forme_juridique <> 'AUTRE' OR forme_juridique_precision IS NOT NULL)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 2. Associés (Nom des associés / Part sociale / e-mail / contact)
-- Obligatoire (niveau applicatif) quand la forme juridique est une
-- société, c’est-à-dire tout sauf INDIVIDUELLE.
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS dossier_associe (
    id INT AUTO_INCREMENT PRIMARY KEY,

    dossier_id INT NOT NULL,

    ordre INT NOT NULL DEFAULT 0 COMMENT 'Ordre de saisie sur la fiche',
    nom VARCHAR(150) NOT NULL COMMENT 'Nom des associés',
    part_sociale VARCHAR(50) NULL COMMENT 'Part sociale (ex. 50 %)',

    -- Coordonnées répétées par associé sur la fiche.
    email VARCHAR(150) NULL COMMENT 'Adresse mail des associés',
    contact VARCHAR(50) NULL COMMENT 'Contact des associés',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_associe_dossier
        FOREIGN KEY (dossier_id)
        REFERENCES dossier_creation_entreprise(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

