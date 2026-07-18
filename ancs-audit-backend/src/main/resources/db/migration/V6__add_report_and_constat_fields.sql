-- ============================================================
-- V6 — Nouveaux champs pour rapports d'audit officiels ANCS v2.1
-- ============================================================

-- Ajout des colonnes à la table constat
ALTER TABLE constat ADD COLUMN IF NOT EXISTS criticite VARCHAR(20);
ALTER TABLE constat ADD COLUMN IF NOT EXISTS preuve_description TEXT;
ALTER TABLE constat ADD COLUMN IF NOT EXISTS recommandation TEXT;
ALTER TABLE constat ADD COLUMN IF NOT EXISTS composantes_impactees TEXT;

-- Ajout des colonnes à la table rapport
ALTER TABLE rapport ADD COLUMN IF NOT EXISTS nom_auditeur VARCHAR(255);
ALTER TABLE rapport ADD COLUMN IF NOT EXISTS numero_certification_ancs VARCHAR(100);
ALTER TABLE rapport ADD COLUMN IF NOT EXISTS contact_auditeur VARCHAR(255);
ALTER TABLE rapport ADD COLUMN IF NOT EXISTS texte_confidentialite TEXT;
ALTER TABLE rapport ADD COLUMN IF NOT EXISTS historique_versions TEXT;
