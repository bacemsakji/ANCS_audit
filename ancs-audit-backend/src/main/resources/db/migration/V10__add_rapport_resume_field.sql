-- V10: Add fichier_resume_url column to rapport table.
-- Stores the MinIO object key for the auto-generated executive-summary (résumé) document.
-- NULL on existing rows; mobile hides the "Résumé" button when resumeDisponible == false.
ALTER TABLE rapport
    ADD COLUMN fichier_resume_url TEXT;
