-- ============================================================
-- V2 — Champs de traçabilité IA sur la table rapport
-- Note : Ces colonnes sont déjà incluses dans V1 lors de la
-- création initiale. Cette migration est conservée pour les
-- environnements ayant déployé V1 sans ces colonnes.
-- ============================================================

-- Vérification et ajout conditionnel des colonnes IA
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'rapport'
          AND column_name = 'synthese_generee_par_ia'
    ) THEN
        ALTER TABLE rapport
            ADD COLUMN synthese_generee_par_ia BOOLEAN NOT NULL DEFAULT false,
            ADD COLUMN synthese_ia_horodatage  TIMESTAMPTZ;

        COMMENT ON COLUMN rapport.synthese_generee_par_ia IS
            'Trace qu''un brouillon IA a été généré — indépendant du contenu final édité';
        COMMENT ON COLUMN rapport.synthese_ia_horodatage IS
            'Horodatage de la dernière génération IA de la synthèse exécutive';
    END IF;
END
$$;
