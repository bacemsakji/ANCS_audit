-- ============================================================
-- V9 — Add organisme annexe 1 fields and unique constraint on rapport
-- ============================================================

-- Deduplicate any existing reports with duplicate (mission_id, version) before adding constraint
DELETE FROM rapport 
WHERE id NOT IN (
    SELECT DISTINCT ON (mission_id, version) id
    FROM rapport 
    ORDER BY mission_id, version, created_at DESC
);

ALTER TABLE organisme ADD COLUMN IF NOT EXISTS acronyme VARCHAR(50);
ALTER TABLE organisme ADD COLUMN IF NOT EXISTS statut VARCHAR(20);
ALTER TABLE organisme ADD COLUMN IF NOT EXISTS categorie VARCHAR(100);

ALTER TABLE rapport ADD CONSTRAINT uk_rapport_mission_version UNIQUE (mission_id, version);
