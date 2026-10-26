-- =========================================================
-- Migration : valeurs d'audit en texte brut (plus de JSON)
-- À exécuter sur la base avant redémarrage de l'application.
-- =========================================================
-- Contexte : AuditService stockait {"valeur":"..."} car les colonnes
-- sont de type JSON (qui n'accepte que du JSON valide). Les valeurs
-- sont désormais stockées en texte brut : les colonnes passent en
-- LONGTEXT. Les lignes existantes sont conservées telles quelles ;
-- l'affichage retire l'enveloppe {"valeur":"..."} des traces historiques.
-- =========================================================

ALTER TABLE audit_log MODIFY COLUMN ancienne_valeur LONGTEXT NULL;
ALTER TABLE audit_log MODIFY COLUMN nouvelle_valeur LONGTEXT NULL;
