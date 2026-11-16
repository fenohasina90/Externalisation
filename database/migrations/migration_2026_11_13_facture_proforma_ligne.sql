-- =========================================================
-- Facture proforma : lignes persistées (DÉSIGNATION / PU / QTÉ
-- / MONTANT HTVA / REMISE / NET COMMERCIAL). Mise en place
-- initial pour la génération et l'affichage des proformas.
-- Idempotente.
-- =========================================================

CREATE TABLE IF NOT EXISTS facture_proforma_ligne (
    id INT AUTO_INCREMENT PRIMARY KEY,

    facture_proforma_id INT NOT NULL,

    -- Ligne de service d'origine (nullable pour traçabilité après retrait)
    ligne_service_id INT NULL,

    -- Nature de la ligne : EXTERNALISATION / DEBOURS
    categorie VARCHAR(30) NOT NULL DEFAULT 'EXTERNALISATION',

    -- Sous-type EXTERNALISATION : PRESTATION / ABONNEMENT_MENSUEL (nullable pour DEBOURS)
    type_prestation VARCHAR(30) NULL,

    -- Ordre d'affichage dans la facture
    ordre INT NOT NULL DEFAULT 0,

    -- Libellé affiché dans la facture (figé à la génération)
    designation VARCHAR(255) NOT NULL,

    -- Prix unitaire affiché
    pu DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Quantité facturée (1 pour les lignes de service)
    quantite DECIMAL(10,2) NOT NULL DEFAULT 1,

    -- Montant HTVA (qté × PU)
    montant_htva DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Remise en Ariary (0 par défaut)
    remise DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Net commercial (HTVA - remise)
    net_commercial DECIMAL(15,2) NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_facture_proforma_ligne_facture (facture_proforma_id, ordre),
    INDEX idx_facture_proforma_ligne_ligne (ligne_service_id),

    FOREIGN KEY (facture_proforma_id) REFERENCES facture_proforma(id) ON DELETE CASCADE,
    FOREIGN KEY (ligne_service_id) REFERENCES ligne_service(id) ON DELETE SET NULL
) ENGINE=InnoDB;
