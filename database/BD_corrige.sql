CREATE DATABASE IF NOT EXISTS gestion_externalisation
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE gestion_externalisation;

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. ROLES
-- =========================================================

DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS piece_jointe;
DROP TABLE IF EXISTS paiement;
DROP TABLE IF EXISTS echeance_paiement;
DROP TABLE IF EXISTS propos_paiement;
DROP TABLE IF EXISTS historique_ligne_service;
DROP TABLE IF EXISTS ligne_service;
DROP TABLE IF EXISTS facture;
DROP TABLE IF EXISTS dossier_associe;
DROP TABLE IF EXISTS dossier_creation_entreprise;
DROP TABLE IF EXISTS demande_reinitialisation;
DROP TABLE IF EXISTS historique_demande;
DROP TABLE IF EXISTS demande_service;
DROP TABLE IF EXISTS client_responsable;
DROP TABLE IF EXISTS commercial;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS domaine_service;
DROP TABLE IF EXISTS societe;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS role_utilisateur;
DROP TABLE IF EXISTS statut_demande;
DROP TABLE IF EXISTS mode_paiement;
DROP TABLE IF EXISTS configuration;

-- =========================================================
-- 2. ROLE UTILISATEUR
-- =========================================================

CREATE TABLE role_utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom_role VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- 3. PERMISSIONS
-- =========================================================

CREATE TABLE permission (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code_permission VARCHAR(100) NOT NULL UNIQUE,
    nom_permission VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    module VARCHAR(100),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- 4. ROLE - PERMISSION
-- =========================================================

CREATE TABLE role_permission (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
        REFERENCES role_utilisateur(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
        REFERENCES permission(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5. UTILISATEURS
-- =========================================================

CREATE TABLE utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,

    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,

    username VARCHAR(100) NOT NULL UNIQUE,

    email VARCHAR(150) NOT NULL UNIQUE,

    mdp_hash VARCHAR(255) NOT NULL,

    role_id INT NOT NULL,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    doit_changer_mdp BOOLEAN NOT NULL DEFAULT FALSE,

    date_expiration_mdp_temporaire DATETIME NULL,

    derniere_connexion DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_utilisateur_role
        FOREIGN KEY (role_id)
        REFERENCES role_utilisateur(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_utilisateur_role
ON utilisateur(role_id);

CREATE INDEX idx_utilisateur_actif
ON utilisateur(actif);

-- =========================================================
-- 6. SOCIETES / CLIENTS
-- =========================================================

CREATE TABLE domaine_service (
    id INT AUTO_INCREMENT PRIMARY KEY,

    nom_domaine VARCHAR(150) NOT NULL UNIQUE,

    description VARCHAR(255),

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE societe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom_societe VARCHAR(200) NOT NULL,
    activite VARCHAR(255),
    contact VARCHAR(100),
    email VARCHAR(150),
    fb VARCHAR(255),
    adresse VARCHAR(255),
    date_creation DATE,
    nombre_personnel INT,

    chiffre_affaire_mensuel DECIMAL(15,2),

    domaine_id INT NOT NULL,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    archived_at DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_societe_nombre_personnel
        CHECK (
            nombre_personnel IS NULL
            OR nombre_personnel > 0
        ),

    CONSTRAINT fk_societe_domaine
        FOREIGN KEY (domaine_id)
        REFERENCES domaine_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_societe_ca
        CHECK (
            chiffre_affaire_mensuel IS NULL
            OR chiffre_affaire_mensuel >= 0
        )
) ENGINE=InnoDB;

CREATE INDEX idx_societe_nom
ON societe(nom_societe);

CREATE INDEX idx_societe_actif
ON societe(actif);

CREATE INDEX idx_societe_domaine
ON societe(domaine_id);

-- =========================================================
-- 7. RESPONSABLE CLIENT
-- =========================================================

CREATE TABLE client_responsable (
    id INT AUTO_INCREMENT PRIMARY KEY,

    societe_id INT NULL COMMENT 'NULL si responsable libre (création d’entreprise)',

    nom_client VARCHAR(150) NOT NULL,

    contact VARCHAR(100),

    email VARCHAR(150),

    fonction VARCHAR(100),

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_responsable_societe
        FOREIGN KEY (societe_id)
        REFERENCES societe(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_responsable_societe
ON client_responsable(societe_id);

-- =========================================================
-- 8. COMMERCIAL
-- =========================================================

CREATE TABLE commercial (
    id INT AUTO_INCREMENT PRIMARY KEY,

    nom_commercial VARCHAR(150) NOT NULL,

    immatricule VARCHAR(100) NOT NULL UNIQUE,

    email VARCHAR(150),

    contact VARCHAR(100),

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- 9. DOMAINE SERVICE
-- =========================================================



-- =========================================================
-- 10. SERVICES
-- =========================================================

CREATE TABLE services (
    id INT AUTO_INCREMENT PRIMARY KEY,

    nom_service VARCHAR(200) NOT NULL,

    code_service VARCHAR(50) NOT NULL UNIQUE COMMENT 'Code unique auto-généré (SRV-0001, …)',

    type_service VARCHAR(30) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD ou CREATION (dossier de création)',

    description TEXT,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    archived_at DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_type
        CHECK (type_service IN ('STANDARD', 'CREATION'))
) ENGINE=InnoDB;

CREATE INDEX idx_service_nom
ON services(nom_service);

CREATE INDEX idx_service_code
ON services(code_service);

CREATE INDEX idx_service_type
ON services(type_service);

-- =========================================================
-- 11. STATUT DES DEMANDES
-- =========================================================

CREATE TABLE statut_demande (
    id INT AUTO_INCREMENT PRIMARY KEY,

    statut VARCHAR(50) NOT NULL UNIQUE,

    description VARCHAR(255),

    ordre INT NOT NULL DEFAULT 0,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- 12. DEMANDE DE SERVICE
-- En-tête de la demande (multi-services : le service, le tarif et les
-- dates de réalisation sont portés par ligne_service, §33).
-- =========================================================

CREATE TABLE demande_service (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reference_demande VARCHAR(100) NOT NULL UNIQUE,
    date_demande DATE NOT NULL,
    societe_id INT NULL COMMENT 'NULL si création d’entreprise (société inexistante)',
    commercial_id INT NULL,
    client_responsable_id INT NULL,
    commentaire TEXT,
    created_by INT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    archived_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_demande_societe
        FOREIGN KEY (societe_id)
        REFERENCES societe(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_demande_commercial
        FOREIGN KEY (commercial_id)
        REFERENCES commercial(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_demande_responsable
        FOREIGN KEY (client_responsable_id)
        REFERENCES client_responsable(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_demande_created_by
        FOREIGN KEY (created_by)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_demande_societe
ON demande_service(societe_id);

CREATE INDEX idx_demande_commercial
ON demande_service(commercial_id);

CREATE INDEX idx_demande_reference
ON demande_service(reference_demande);

-- =========================================================
-- 13. DOSSIER DE CRÉATION D'ENTREPRISE OU DE SOCIÉTÉ
-- (1 ligne par demande_service ; voir migration_dossier_creation_entreprise.sql)
-- =========================================================

CREATE TABLE dossier_creation_entreprise (
    id INT AUTO_INCREMENT PRIMARY KEY,
    demande_id INT NOT NULL UNIQUE,
    forme_juridique ENUM('INDIVIDUELLE', 'SARL', 'SARLU', 'SA', 'AUTRE') NOT NULL,
    forme_juridique_precision VARCHAR(100) NULL,
    denomination VARCHAR(255) NOT NULL,
    siege_social_actuel VARCHAR(255) NULL,
    capital_social DECIMAL(15,2) NULL,
    nombre_associes_declare INT NULL,
    nom_gerant VARCHAR(150) NULL,
    fonction_gerant VARCHAR(100) NULL,
    location_gerance BOOLEAN NOT NULL DEFAULT FALSE,
    secteur_activite VARCHAR(150) NULL,
    autorisation_speciale VARCHAR(255) NULL,
    adresse_siege VARCHAR(255) NULL,
    ville VARCHAR(100) NULL,
    commune VARCHAR(100) NULL,
    district VARCHAR(100) NULL,
    region VARCHAR(100) NULL,
    type_hebergement ENUM('DOMICILE_GERANT', 'LOCATION_CABINET', 'AUTRE') NULL,
    hebergement_precision VARCHAR(255) NULL,
    contrat_bail BOOLEAN NULL,
    contrat_bail_detail VARCHAR(255) NULL,
    objet_social TEXT NULL,
    date_souhaitee_debut DATE NULL,
    regime_activite ENUM('PERMANENTE', 'SAISONNIERE', 'AMBULANT') NULL,
    activites_etablissement TEXT NULL,
    observations TEXT NULL,
    assujettissement_tva VARCHAR(100) NULL,
    autre_societe VARCHAR(255) NULL,
    situation_matrimoniale VARCHAR(50) NULL,
    regime_imposition VARCHAR(100) NULL,
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

CREATE TABLE dossier_associe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dossier_id INT NOT NULL,
    ordre INT NOT NULL DEFAULT 0,
    nom VARCHAR(150) NOT NULL,
    part_sociale VARCHAR(50) NULL,
    email VARCHAR(150) NULL,
    contact VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_associe_dossier
        FOREIGN KEY (dossier_id)
        REFERENCES dossier_creation_entreprise(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 14. HISTORIQUE DES DEMANDES
-- =========================================================

CREATE TABLE historique_demande (
    id INT AUTO_INCREMENT PRIMARY KEY,

    demande_id INT NOT NULL,

    statut_id INT NOT NULL,

    commentaire TEXT,

    utilisateur_id INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historique_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_historique_statut
        FOREIGN KEY (statut_id)
        REFERENCES statut_demande(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_historique_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_historique_demande_demande
ON historique_demande(demande_id);

CREATE INDEX idx_historique_demande_date
ON historique_demande(created_at);

-- =========================================================
-- 15. PROPOSITION / PLAN DE PAIEMENT
-- Rattaché à la demande ET à la ligne de service : chaque service
-- d'une demande possède son propre plan (voir ligne_service, §33).
-- =========================================================

CREATE TABLE propos_paiement (
    id INT AUTO_INCREMENT PRIMARY KEY,

    demande_id INT NOT NULL,

    ligne_service_id INT NOT NULL,

    date_debut DATE NOT NULL,

    date_fin DATE NOT NULL,

    montant_total DECIMAL(15,2) NOT NULL,

    reste_payer DECIMAL(15,2) NOT NULL,

    nombre_echeances INT NOT NULL DEFAULT 1,

    commentaire TEXT,

    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF',

    created_by INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_propos_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_propos_ligne
        FOREIGN KEY (ligne_service_id)
        REFERENCES ligne_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_propos_created_by
        FOREIGN KEY (created_by)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_propos_dates
        CHECK (date_fin >= date_debut),

    CONSTRAINT chk_propos_montant
        CHECK (montant_total >= 0),

    CONSTRAINT chk_propos_reste
        CHECK (
            reste_payer >= 0
            AND reste_payer <= montant_total
        ),

    CONSTRAINT chk_propos_echeances
        CHECK (nombre_echeances > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_propos_demande
ON propos_paiement(demande_id);

CREATE INDEX idx_propos_ligne
ON propos_paiement(ligne_service_id);

-- =========================================================
-- 16. ECHEANCES DE PAIEMENT
-- =========================================================

CREATE TABLE echeance_paiement (
    id INT AUTO_INCREMENT PRIMARY KEY,

    propos_paiement_id INT NOT NULL,

    numero_echeance INT NOT NULL,

    date_echeance DATE NOT NULL,

    montant_attendu DECIMAL(15,2) NOT NULL,

    montant_paye DECIMAL(15,2) NOT NULL DEFAULT 0,

    commentaire TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_echeance_propos
        FOREIGN KEY (propos_paiement_id)
        REFERENCES propos_paiement(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_echeance_numero
        UNIQUE (propos_paiement_id, numero_echeance),

    CONSTRAINT chk_echeance_numero
        CHECK (numero_echeance > 0),

    CONSTRAINT chk_echeance_montant
        CHECK (montant_attendu > 0),

    CONSTRAINT chk_echeance_paye
        CHECK (
            montant_paye >= 0
            AND montant_paye <= montant_attendu
        )
) ENGINE=InnoDB;

CREATE INDEX idx_echeance_date
ON echeance_paiement(date_echeance);

CREATE INDEX idx_echeance_propos
ON echeance_paiement(propos_paiement_id);

-- =========================================================
-- 17. MODE DE PAIEMENT
-- =========================================================

CREATE TABLE mode_paiement (
    id INT AUTO_INCREMENT PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,

    libelle VARCHAR(100) NOT NULL,

    actif BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- =========================================================
-- 18. PAIEMENTS
-- =========================================================

CREATE TABLE paiement (
    id INT AUTO_INCREMENT PRIMARY KEY,

    reference_paiement VARCHAR(100) NOT NULL UNIQUE,

    demande_id INT NOT NULL,

    echeance_id INT NULL,

    mode_paiement_id INT NOT NULL,

    date_paiement DATE NOT NULL,

    montant_paye DECIMAL(15,2) NOT NULL,

    reference_virement VARCHAR(150) NULL,

    commentaire TEXT,

    piece_justificative VARCHAR(255) NULL,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_by INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    archived_at DATETIME NULL,

    CONSTRAINT fk_paiement_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_paiement_echeance
        FOREIGN KEY (echeance_id)
        REFERENCES echeance_paiement(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_paiement_mode
        FOREIGN KEY (mode_paiement_id)
        REFERENCES mode_paiement(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_paiement_utilisateur
        FOREIGN KEY (created_by)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_paiement_montant
        CHECK (montant_paye > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_paiement_demande
ON paiement(demande_id);

CREATE INDEX idx_paiement_echeance
ON paiement(echeance_id);

CREATE INDEX idx_paiement_date
ON paiement(date_paiement);

CREATE INDEX idx_paiement_mode
ON paiement(mode_paiement_id);

-- =========================================================
-- 19. PIECES JOINTES
-- =========================================================

CREATE TABLE piece_jointe (
    id INT AUTO_INCREMENT PRIMARY KEY,

    demande_id INT NOT NULL,

    paiement_id INT NULL,

    nom_fichier VARCHAR(255) NOT NULL,

    nom_original VARCHAR(255) NOT NULL,

    chemin_fichier VARCHAR(500) NOT NULL,

    type_fichier VARCHAR(100),

    taille_fichier BIGINT,

    description VARCHAR(255),

    uploaded_by INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_piece_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_piece_paiement
        FOREIGN KEY (paiement_id)
        REFERENCES paiement(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_piece_utilisateur
        FOREIGN KEY (uploaded_by)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_piece_taille
        CHECK (
            taille_fichier IS NULL
            OR taille_fichier >= 0
        )
) ENGINE=InnoDB;

CREATE INDEX idx_piece_demande
ON piece_jointe(demande_id);

CREATE INDEX idx_piece_paiement
ON piece_jointe(paiement_id);

-- =========================================================
-- 20. NOTIFICATIONS
-- =========================================================

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    utilisateur_id INT NOT NULL,

    type_notification VARCHAR(50) NOT NULL,

    titre VARCHAR(200) NOT NULL,

    message TEXT NOT NULL,

    reference_type VARCHAR(100),

    reference_id BIGINT,

    lu BOOLEAN NOT NULL DEFAULT FALSE,

    date_lecture DATETIME NULL,

    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    date_envoi DATETIME NULL,

    CONSTRAINT fk_notification_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_notification_utilisateur
ON notification(utilisateur_id);

CREATE INDEX idx_notification_lu
ON notification(utilisateur_id, lu);

CREATE INDEX idx_notification_date
ON notification(date_creation);

-- =========================================================
-- 21. AUDIT LOG
-- =========================================================

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    utilisateur_id INT NULL,

    action VARCHAR(50) NOT NULL,

    module VARCHAR(100) NOT NULL,

    entite VARCHAR(100),

    entite_id BIGINT,

    ancienne_valeur LONGTEXT NULL,

    nouvelle_valeur LONGTEXT NULL,

    adresse_ip VARCHAR(45),

    user_agent VARCHAR(500),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_audit_utilisateur
ON audit_log(utilisateur_id);

CREATE INDEX idx_audit_module
ON audit_log(module);

CREATE INDEX idx_audit_entite
ON audit_log(entite, entite_id);

CREATE INDEX idx_audit_date
ON audit_log(created_at);

-- =========================================================
-- 22. CONFIGURATION GENERALE
-- =========================================================

CREATE TABLE configuration (
    id INT AUTO_INCREMENT PRIMARY KEY,

    cle VARCHAR(100) NOT NULL UNIQUE,

    valeur TEXT,

    type_valeur VARCHAR(30) NOT NULL DEFAULT 'STRING',

    description VARCHAR(255),

    modifiable BOOLEAN NOT NULL DEFAULT TRUE,

    updated_by INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_configuration_utilisateur
        FOREIGN KEY (updated_by)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 23. DEMANDES DE REINITIALISATION MOT DE PASSE
--     Flux « mot de passe temporaire » : l'administrateur approuve la
--     demande puis genere un mot de passe temporaire que l'utilisateur
--     doit changer a sa premiere connexion.
-- =========================================================

CREATE TABLE demande_reinitialisation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    utilisateur_id INT NOT NULL,

    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',

    motif_refus VARCHAR(500) NULL,

    traite_par INT NULL,

    date_traitement DATETIME NULL,

    genere_par INT NULL,

    date_generation_mdp DATETIME NULL,

    date_expiration_mdp DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_demande_reset_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_demande_reset_traite_par
        FOREIGN KEY (traite_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_demande_reset_genere_par
        FOREIGN KEY (genere_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_demande_reset_utilisateur
ON demande_reinitialisation(utilisateur_id);

CREATE INDEX idx_demande_reset_statut
ON demande_reinitialisation(statut);

-- =========================================================
-- 24. DONNEES INITIALES : ROLES
-- =========================================================

INSERT INTO role_utilisateur
(nom_role, description)
VALUES
('SUPER_ADMIN', 'Accès complet à toutes les fonctionnalités'),
('ADMIN', 'Administrateur : accès complet, validation des demandes de réinitialisation'),
('ACCUEIL', 'Gestion des clients et demandes de service'),
('ECONOMAT', 'Gestion des paiements et du suivi financier');

-- =========================================================
-- 25. DONNEES INITIALES : STATUTS
-- =========================================================

INSERT INTO statut_demande
(statut, description, ordre)
VALUES
('EN_ATTENTE', 'Demande créée et en attente de traitement', 1),
('VALIDATION', 'Demande validée par le paiement du premier tranche du plan de paiement', 2),
('EN_COURS', 'Service actuellement en cours', 3),
('TERMINEE', 'Service terminé', 4),
('ANNULEE', 'Demande annulée', 5);

-- =========================================================
-- 26. DONNEES INITIALES : MODES DE PAIEMENT
-- =========================================================

INSERT INTO mode_paiement
(code, libelle)
VALUES
('ESPECES', 'Espèces'),
('VIREMENT', 'Virement bancaire');

-- =========================================================
-- 27. PERMISSIONS
-- =========================================================

INSERT INTO permission
(code_permission, nom_permission, module, description)
VALUES

-- Dashboard
('DASHBOARD_VIEW', 'Consulter le tableau de bord',
 'DASHBOARD', 'Accéder au tableau de bord'),

-- Utilisateurs
('USER_VIEW', 'Consulter les utilisateurs',
 'UTILISATEUR', 'Voir les utilisateurs'),

('USER_CREATE', 'Créer un utilisateur',
 'UTILISATEUR', 'Créer un nouvel utilisateur'),

('USER_UPDATE', 'Modifier un utilisateur',
 'UTILISATEUR', 'Modifier un utilisateur'),

('USER_DELETE', 'Supprimer un utilisateur',
 'UTILISATEUR', 'Supprimer un utilisateur'),

-- ('PASSWORD_RESET_MANAGE', 'Gérer les réinitialisations de mot de passe',
--  'UTILISATEUR', 'Valider ou rejeter les demandes de réinitialisation de mot de passe'),

-- Clients
('CLIENT_VIEW', 'Consulter les clients',
 'CLIENT', 'Voir les sociétés clientes'),

('CLIENT_CREATE', 'Créer un client',
 'CLIENT', 'Créer une société'),

('CLIENT_UPDATE', 'Modifier un client',
 'CLIENT', 'Modifier une société'),

('CLIENT_DELETE', 'Supprimer un client',
 'CLIENT', 'Supprimer une société'),

-- Responsables
('RESPONSABLE_VIEW', 'Consulter les responsables',
 'RESPONSABLE', 'Voir les responsables'),

('RESPONSABLE_CREATE', 'Créer un responsable',
 'RESPONSABLE', 'Créer un responsable'),

('RESPONSABLE_UPDATE', 'Modifier un responsable',
 'RESPONSABLE', 'Modifier un responsable'),

('RESPONSABLE_DELETE', 'Supprimer un responsable',
 'RESPONSABLE', 'Supprimer un responsable'),

-- Commercial
('COMMERCIAL_VIEW', 'Consulter les commerciaux',
 'COMMERCIAL', 'Voir les commerciaux'),

('COMMERCIAL_CREATE', 'Créer un commercial',
 'COMMERCIAL', 'Créer un commercial'),

('COMMERCIAL_UPDATE', 'Modifier un commercial',
 'COMMERCIAL', 'Modifier un commercial'),

('COMMERCIAL_DELETE', 'Supprimer un commercial',
 'COMMERCIAL', 'Supprimer un commercial'),

-- Domaines
('DOMAINE_VIEW', 'Consulter les domaines',
 'SERVICE', 'Voir les domaines de services'),

('DOMAINE_CREATE', 'Créer un domaine',
 'SERVICE', 'Créer un domaine'),

('DOMAINE_UPDATE', 'Modifier un domaine',
 'SERVICE', 'Modifier un domaine'),

('DOMAINE_DELETE', 'Supprimer un domaine',
 'SERVICE', 'Supprimer un domaine'),

-- Services
('SERVICE_VIEW', 'Consulter les services',
 'SERVICE', 'Voir les services'),

('SERVICE_CREATE', 'Créer un service',
 'SERVICE', 'Créer un service'),

('SERVICE_UPDATE', 'Modifier un service',
 'SERVICE', 'Modifier un service'),

('SERVICE_DELETE', 'Supprimer un service',
 'SERVICE', 'Supprimer un service'),

-- Demandes
('DEMANDE_VIEW', 'Consulter les demandes',
 'DEMANDE', 'Voir les demandes'),

('DEMANDE_CREATE', 'Créer une demande',
 'DEMANDE', 'Créer une demande'),

('DEMANDE_UPDATE', 'Modifier une demande',
 'DEMANDE', 'Modifier une demande'),

('DEMANDE_DELETE', 'Supprimer une demande',
 'DEMANDE', 'Supprimer une demande'),

('DEMANDE_STATUS_UPDATE', 'Modifier le statut',
 'DEMANDE', 'Modifier le statut d’une demande'),

-- Paiements
('PAIEMENT_VIEW', 'Consulter les paiements',
 'PAIEMENT', 'Voir les paiements'),

('PAIEMENT_CREATE', 'Enregistrer un paiement',
 'PAIEMENT', 'Enregistrer un paiement'),

('PAIEMENT_UPDATE', 'Modifier un paiement',
 'PAIEMENT', 'Modifier un paiement'),

('PAIEMENT_DELETE', 'Supprimer un paiement',
 'PAIEMENT', 'Supprimer un paiement'),

-- Rapports
('REPORT_VIEW', 'Consulter les rapports',
 'RAPPORT', 'Consulter les statistiques'),

('REPORT_EXPORT', 'Exporter les rapports',
 'RAPPORT', 'Exporter PDF/Excel/CSV'),

-- Documents
('DOCUMENT_VIEW', 'Consulter les documents',
 'DOCUMENT', 'Voir les pièces jointes'),

('DOCUMENT_UPLOAD', 'Ajouter un document',
 'DOCUMENT', 'Télécharger une pièce jointe'),

('DOCUMENT_DELETE', 'Supprimer un document',
 'DOCUMENT', 'Supprimer une pièce jointe'),

-- Audit
('AUDIT_VIEW', 'Consulter les journaux',
 'AUDIT', 'Consulter le journal d’audit'),

-- Configuration
('CONFIG_VIEW', 'Consulter la configuration',
 'CONFIGURATION', 'Consulter les paramètres'),

('CONFIG_UPDATE', 'Modifier la configuration',
 'CONFIGURATION', 'Modifier les paramètres'),

-- Dashboard personnalisé (blocs demande / paiement)
('DASHBOARD_DEMANDE_VIEW', 'Consulter les statistiques de demandes',
 'DASHBOARD', 'Voir les blocs demandes du tableau de bord'),

('DASHBOARD_PAIEMENT_VIEW', 'Consulter les statistiques de paiements',
 'DASHBOARD', 'Voir les blocs paiement du tableau de bord'),

-- Modes de paiement
('MODE_PAIEMENT_VIEW', 'Consulter les modes de paiement',
 'PAIEMENT', 'Voir les modes de paiement'),

('MODE_PAIEMENT_UPDATE', 'Gérer les modes de paiement',
 'PAIEMENT', 'Créer / modifier / désactiver les modes de paiement'),

-- Rapports (séparés demande / paiement)
('RAPPORT_DEMANDE_VIEW', 'Consulter les rapports liés aux demandes',
 'RAPPORT', 'Rapports général, par client et des demandes'),

('RAPPORT_DEMANDE_EXPORT', 'Exporter les rapports liés aux demandes',
 'RAPPORT', 'Export PDF / Excel / CSV des rapports demandes'),

('RAPPORT_PAIEMENT_VIEW', 'Consulter les rapports liés aux paiements',
 'RAPPORT', 'Rapports financier, par service et des paiements'),

('RAPPORT_PAIEMENT_EXPORT', 'Exporter les rapports liés aux paiements',
 'RAPPORT', 'Export PDF / Excel / CSV des rapports paiements'),

-- Documents (séparés demande / paiement)
('DOCUMENT_DEMANDE_VIEW', 'Consulter les documents des demandes',
 'DOCUMENT', 'Pièces jointes et PDF liés aux demandes'),

('DOCUMENT_DEMANDE_UPLOAD', 'Ajouter un document de demande',
 'DOCUMENT', 'Télécharger une pièce jointe à une demande'),

('DOCUMENT_DEMANDE_DELETE', 'Supprimer un document de demande',
 'DOCUMENT', 'Supprimer une pièce jointe de demande'),

('DOCUMENT_PAIEMENT_VIEW', 'Consulter les documents de paiement',
 'DOCUMENT', 'Reçus de paiement et documents financiers'),

-- Réinitialisation mot de passe (validation administrateur)
('PASSWORD_RESET_MANAGE', 'Gérer les réinitialisations de mot de passe',
 'UTILISATEUR', 'Valider ou rejeter les demandes de réinitialisation de mot de passe'),

-- Dossier de création d'entreprise ou de société
('DOSSIER_VIEW', 'Consulter le dossier de création',
 'DOSSIER', 'Voir la fiche de création d’entreprise ou de société'),

('DOSSIER_CREATE', 'Créer le dossier de création',
 'DOSSIER', 'Saisir la fiche de création d’entreprise ou de société'),

('DOSSIER_UPDATE', 'Modifier le dossier de création',
 'DOSSIER', 'Modifier la fiche et les associés'),

-- Factures (demandes multi-services)
('FACTURE_VIEW', 'Consulter les factures',
 'FACTURE', 'Voir les factures proforma et validées'),

('FACTURE_CREATE', 'Créer une facture proforma',
 'FACTURE', 'Générer les factures proforma d''une demande'),

('FACTURE_VALIDATE', 'Valider une facture',
 'FACTURE', 'Valider une facture proforma'),

-- Sauvegarde (module hors ligne)
('BACKUP_VIEW', 'Consulter les sauvegardes',
 'SAUVEGARDE', 'Lister et télécharger les sauvegardes de la base'),

('BACKUP_CREATE', 'Créer une sauvegarde',
 'SAUVEGARDE', 'Générer une sauvegarde manuelle ou ponctuelle'),

('BACKUP_DELETE', 'Supprimer une sauvegarde',
 'SAUVEGARDE', 'Supprimer une archive de sauvegarde'),

('BACKUP_RESTORE', 'Restaurer une sauvegarde',
 'SAUVEGARDE', 'Restaurer la base depuis un fichier de sauvegarde');

-- =========================================================
-- 28. ATTRIBUTION DES PERMISSIONS AU SUPER ADMIN ET A L'ADMIN
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM role_utilisateur r
CROSS JOIN permission p
WHERE r.nom_role IN ('SUPER_ADMIN', 'ADMIN');

-- =========================================================
-- 29. PERMISSIONS ACCUEIL
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM role_utilisateur r
JOIN permission p
WHERE r.nom_role = 'ACCUEIL'
AND p.code_permission IN (
    'DASHBOARD_VIEW',
    'DASHBOARD_DEMANDE_VIEW',

    'CLIENT_VIEW',
    'CLIENT_CREATE',
    'CLIENT_UPDATE',

    'RESPONSABLE_VIEW',
    'RESPONSABLE_CREATE',
    'RESPONSABLE_UPDATE',
    'RESPONSABLE_DELETE',

    'DOMAINE_VIEW',
    'DOMAINE_CREATE',
    'DOMAINE_UPDATE',
    'DOMAINE_DELETE',

    'SERVICE_VIEW',
    'SERVICE_CREATE',
    'SERVICE_UPDATE',

    'DEMANDE_VIEW',
    'DEMANDE_CREATE',
    'DEMANDE_UPDATE',
    'DEMANDE_STATUS_UPDATE',

    'RAPPORT_DEMANDE_VIEW',
    'RAPPORT_DEMANDE_EXPORT',

    'DOCUMENT_DEMANDE_VIEW',
    'DOCUMENT_DEMANDE_UPLOAD',
    'DOCUMENT_DEMANDE_DELETE',

    'DOSSIER_VIEW',
    'DOSSIER_CREATE',
    'DOSSIER_UPDATE',

    'FACTURE_VIEW',
    'FACTURE_CREATE'
);

-- =========================================================
-- 30. PERMISSIONS ECONOMAT
-- =========================================================

INSERT INTO role_permission (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM role_utilisateur r
JOIN permission p
WHERE r.nom_role = 'ECONOMAT'
AND p.code_permission IN (
    'DASHBOARD_VIEW',
    'DASHBOARD_PAIEMENT_VIEW',

    'CLIENT_VIEW',

    'DEMANDE_VIEW',

    'PAIEMENT_VIEW',
    'PAIEMENT_CREATE',
    'PAIEMENT_UPDATE',

    'MODE_PAIEMENT_VIEW',
    'MODE_PAIEMENT_UPDATE',

    'RAPPORT_PAIEMENT_VIEW',
    'RAPPORT_PAIEMENT_EXPORT',

    'DOCUMENT_PAIEMENT_VIEW',

    'DOSSIER_VIEW',

    'FACTURE_VIEW'
);

-- =========================================================
-- 31. CONFIGURATION INITIALE
-- =========================================================

INSERT INTO configuration
(cle, valeur, type_valeur, description)
VALUES

(
    'NOM_APPLICATION',
    'Gestion Externalisation',
    'STRING',
    'Nom de l’application'
),

(
    'DEVISE',
    'MGA',
    'STRING',
    'Devise utilisée dans l’application'
),

(
    'JOURS_AVANCE_ALERTE_ECHEANCE',
    '3',
    'INTEGER',
    'Nombre de jours avant échéance pour déclencher une alerte'
),

(
    'TAUX_TVA',
    '0',
    'DECIMAL',
    'Taux de TVA par défaut'
),

(
    'TAILLE_MAX_FICHIER',
    '10485760',
    'INTEGER',
    'Taille maximale des fichiers en octets'
),

(
    'DUREE_SESSION_MINUTES',
    '60',
    'INTEGER',
    'Durée maximale de session'
),

(
    'PAGINATION_TAILLE_DEFAUT',
    '20',
    'INTEGER',
    'Nombre de résultats par page'
),

(
    'DUREE_VALIDITE_DEMANDE_RESET',
    '1440',
    'INTEGER',
    'Durée de validité (minutes) d une demande de réinitialisation en attente avant expiration'
),

(
    'DUREE_VALIDITE_MDP_TEMPORAIRE',
    '1440',
    'INTEGER',
    'Durée de validité (minutes) du mot de passe temporaire avant changement obligatoire'
),

(
    'URL_APPLICATION',
    'http://localhost:5173',
    'STRING',
    'URL de base du frontend utilisée pour construire les liens envoyés par email'
);

-- =========================================================
-- 32. FACTURES (PROFORMA PUIS VALIDÉES)
-- =========================================================

CREATE TABLE facture (
    id INT AUTO_INCREMENT PRIMARY KEY,

    reference_facture VARCHAR(100) NOT NULL UNIQUE,

    demande_id INT NOT NULL,

    mode_facturation ENUM('UNIQUE', 'PAR_SERVICE') NOT NULL DEFAULT 'PAR_SERVICE',

    montant DECIMAL(15,2) NOT NULL,

    statut VARCHAR(50) NOT NULL DEFAULT 'PROFORMA',

    valide_par INT NULL,

    valide_le DATETIME NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_facture_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_facture_valide_par
        FOREIGN KEY (valide_par)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_facture_montant
        CHECK (montant >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_facture_demande
ON facture(demande_id);

CREATE INDEX idx_facture_reference
ON facture(reference_facture);

-- =========================================================
-- 33. LIGNES DE SERVICE (UN SERVICE AU SEIN D'UNE DEMANDE)
-- =========================================================

CREATE TABLE ligne_service (
    id INT AUTO_INCREMENT PRIMARY KEY,

    demande_id INT NOT NULL,

    service_id INT NOT NULL,

    tarif DECIMAL(15,2) NOT NULL,

    debut_service DATE NOT NULL,

    fin_service DATE NOT NULL,

    statut VARCHAR(50) NULL,

    facture_id INT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ligne_demande
        FOREIGN KEY (demande_id)
        REFERENCES demande_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ligne_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ligne_facture
        FOREIGN KEY (facture_id)
        REFERENCES facture(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_ligne_dates
        CHECK (fin_service >= debut_service),

    CONSTRAINT chk_ligne_tarif
        CHECK (tarif >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_ligne_demande
ON ligne_service(demande_id);

CREATE INDEX idx_ligne_service
ON ligne_service(service_id);

CREATE INDEX idx_ligne_facture
ON ligne_service(facture_id);

-- =========================================================
-- 34. HISTORIQUE DES LIGNES DE SERVICE
-- =========================================================

CREATE TABLE historique_ligne_service (
    id INT AUTO_INCREMENT PRIMARY KEY,

    ligne_service_id INT NOT NULL,

    statut VARCHAR(50) NOT NULL,

    commentaire TEXT,

    utilisateur_id INT NULL,

    origine VARCHAR(20) NOT NULL DEFAULT 'MANUEL',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historique_ligne
        FOREIGN KEY (ligne_service_id)
        REFERENCES ligne_service(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_historique_ligne_utilisateur
        FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_historique_ligne_ligne
ON historique_ligne_service(ligne_service_id);

CREATE INDEX idx_historique_ligne_date
ON historique_ligne_service(created_at);

SET FOREIGN_KEY_CHECKS = 1;