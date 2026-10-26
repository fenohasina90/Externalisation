-- Données minimales pour les tests (H2, create-drop) :
-- les rôles doivent exister pour l'initialisation du compte admin.
INSERT INTO role_utilisateur (nom_role, description, actif, created_at, updated_at) VALUES
    ('SUPER_ADMIN', 'Accès complet', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN', 'Administrateur', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ACCUEIL', 'Accueil', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ECONOMAT', 'Économat', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
