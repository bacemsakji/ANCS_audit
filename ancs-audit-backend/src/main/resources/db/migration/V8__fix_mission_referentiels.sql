-- ============================================================
-- V8 — Fix missions associated with incorrect referentiel types
-- ============================================================

-- If any mission points to a referentiel that is NOT of type 'CONTROLE_TECHNIQUE',
-- update it to point to the actual checklist referentiel.
UPDATE mission
SET referentiel_id = (SELECT id FROM referentiel WHERE type = 'CONTROLE_TECHNIQUE' LIMIT 1)
WHERE referentiel_id NOT IN (
    SELECT id FROM referentiel WHERE type = 'CONTROLE_TECHNIQUE'
);
