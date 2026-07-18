-- ============================================================
-- V7 — Référentiel ANCS v3.1 (ISO/IEC 27002:2022)
--      + Modèle de soumission ANCS (Issue #7)
-- ============================================================
-- Remplace les 7 catégories fictives de V4 par les 4 domaines
-- officiels du Référentiel d'Audit SI ANCS v3.1 (18/09/2023),
-- alignés sur la norme ISO/IEC 27002:2022.
--
-- 4 domaines → 93 contrôles numérotés 5.1–8.34 :
--   5.x  Contrôles organisationnels   (37 contrôles)
--   6.x  Contrôles liés aux personnes  (8 contrôles)
--   7.x  Contrôles physiques          (14 contrôles)
--   8.x  Contrôles technologiques     (34 contrôles)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. Nouveau champ sous_critere sur controle
-- ─────────────────────────────────────────────────────────────
ALTER TABLE controle
    ADD COLUMN IF NOT EXISTS sous_critere VARCHAR(10);

-- ─────────────────────────────────────────────────────────────
-- 2. Modèle de soumission ANCS sur rapport (Issue #7)
-- ─────────────────────────────────────────────────────────────
ALTER TABLE rapport
    ADD COLUMN IF NOT EXISTS statut_soumission_ancs VARCHAR(20) NOT NULL DEFAULT 'NON_SOUMIS',
    ADD COLUMN IF NOT EXISTS date_soumission_ancs   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motif_rejet            TEXT,
    ADD COLUMN IF NOT EXISTS date_limite_resoumission DATE;

-- ─────────────────────────────────────────────────────────────
-- 3. Entité ReunionTravail (Issue #7 — Art. décret 2004-1250)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reunion_travail (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id       UUID NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    date_reunion     TIMESTAMP NOT NULL,
    participants     TEXT,
    compte_rendu     TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reunion_mission ON reunion_travail(mission_id);

-- ─────────────────────────────────────────────────────────────
-- 4. Reseed : suppression du référentiel fictif V4
-- ─────────────────────────────────────────────────────────────
DO $$
DECLARE
    old_ref_id UUID;
BEGIN
    -- Identifier l'ancien référentiel fictif (V4, catégories inventées)
    SELECT id INTO old_ref_id
    FROM referentiel
    WHERE type = 'CONTROLE_TECHNIQUE'
      AND (version = '2.0' OR description LIKE '%Gouvernance%' OR description LIKE '%fictif%')
    LIMIT 1;

    IF old_ref_id IS NOT NULL THEN
        -- Supprimer les constats liés aux anciens contrôles fictifs
        -- (uniquement ceux non encore synchronisés / liés à une mission réelle)
        DELETE FROM constat
        WHERE controle_id IN (
            SELECT id FROM controle WHERE referentiel_id = old_ref_id
        )
          AND mission_id IN (
            SELECT id FROM mission WHERE statut IN ('PLANIFIEE', 'EN_COURS')
        );

        -- Supprimer les anciens contrôles fictifs
        DELETE FROM controle WHERE referentiel_id = old_ref_id;

        -- Mettre à jour les métadonnées du référentiel
        UPDATE referentiel
        SET nom         = 'Référentiel d''Audit de la Sécurité des SI — ANCS v3.1',
            version     = '3.1',
            source_url  = 'https://www.ancs.gov.tn/referentiel-audit-3.1',
            description = 'Référentiel officiel de l''ANCS version 3.1 (18/09/2023), aligné sur '
                          'la norme ISO/IEC 27002:2022. Comprend 4 domaines et 93 contrôles : '
                          'organisationnels (5.x), liés aux personnes (6.x), physiques (7.x) '
                          'et technologiques (8.x).'
        WHERE id = old_ref_id;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────
-- 5. Insertion des 93 contrôles officiels ANCS v3.1
-- ─────────────────────────────────────────────────────────────
DO $$
DECLARE
    ref_id UUID;
BEGIN
    -- Obtenir (ou créer) le référentiel ANCS v3.1
    SELECT id INTO ref_id
    FROM referentiel
    WHERE type = 'CONTROLE_TECHNIQUE'
      AND version = '3.1'
    LIMIT 1;

    IF ref_id IS NULL THEN
        INSERT INTO referentiel (id, nom, type, version, source_url, description)
        VALUES (
            gen_random_uuid(),
            'Référentiel d''Audit de la Sécurité des SI — ANCS v3.1',
            'CONTROLE_TECHNIQUE',
            '3.1',
            'https://www.ancs.gov.tn/referentiel-audit-3.1',
            'Référentiel officiel de l''ANCS version 3.1 (18/09/2023), aligné sur '
            'la norme ISO/IEC 27002:2022. Comprend 4 domaines et 93 contrôles.'
        )
        RETURNING id INTO ref_id;
    END IF;

    -- ================================================================
    -- DOMAINE 5 : CONTRÔLES ORGANISATIONNELS (5.1 – 5.37)
    -- ================================================================
    INSERT INTO controle (id, referentiel_id, libelle, description, criticite, categorie, sous_critere, ordre_affichage)
    VALUES

    (gen_random_uuid(), ref_id,
     'Politiques de sécurité de l''information',
     'Une politique de sécurité de l''information et des politiques spécifiques doivent être définies, approuvées par la direction, publiées, communiquées au personnel et aux parties intéressées et révisées à intervalles planifiés.',
     'ELEVE', 'Contrôles organisationnels', '5.1', 501),

    (gen_random_uuid(), ref_id,
     'Fonctions et responsabilités liées à la sécurité de l''information',
     'Les fonctions et les responsabilités liées à la sécurité de l''information doivent être définies et attribuées selon les besoins de l''organisme.',
     'ELEVE', 'Contrôles organisationnels', '5.2', 502),

    (gen_random_uuid(), ref_id,
     'Séparation des tâches',
     'Les tâches et les domaines de responsabilité incompatibles doivent être cloisonnés pour éviter qu''une personne ne puisse réaliser seule des tâches potentiellement incompatibles.',
     'ELEVE', 'Contrôles organisationnels', '5.3', 503),

    (gen_random_uuid(), ref_id,
     'Responsabilités de la direction',
     'La direction doit demander à tout le personnel d''appliquer les mesures de sécurité de l''information conformément à la politique de sécurité et aux procédures établies.',
     'MOYEN', 'Contrôles organisationnels', '5.4', 504),

    (gen_random_uuid(), ref_id,
     'Contacts avec les autorités',
     'Le contact avec les autorités appropriées doit être établi et maintenu.',
     'MOYEN', 'Contrôles organisationnels', '5.5', 505),

    (gen_random_uuid(), ref_id,
     'Contacts avec des groupes d''intérêt spécifiques',
     'Des contacts avec des groupes d''intérêt spécifiques, des forums spécialisés en sécurité et des associations professionnelles doivent être établis et maintenus.',
     'FAIBLE', 'Contrôles organisationnels', '5.6', 506),

    (gen_random_uuid(), ref_id,
     'Renseignements sur les menaces',
     'Des informations relatives aux menaces à la sécurité de l''information doivent être recueillies, analysées et communiquées de manière appropriée.',
     'ELEVE', 'Contrôles organisationnels', '5.7', 507),

    (gen_random_uuid(), ref_id,
     'Sécurité de l''information dans la gestion de projet',
     'La sécurité de l''information doit être intégrée dans la gestion de projet.',
     'MOYEN', 'Contrôles organisationnels', '5.8', 508),

    (gen_random_uuid(), ref_id,
     'Inventaire des informations et autres actifs associés',
     'Un inventaire des informations et autres actifs associés, y compris leurs propriétaires, doit être élaboré et tenu à jour.',
     'ELEVE', 'Contrôles organisationnels', '5.9', 509),

    (gen_random_uuid(), ref_id,
     'Utilisation correcte des informations et autres actifs associés',
     'Les règles d''utilisation correcte et les procédures de traitement des informations et autres actifs associés doivent être identifiées, documentées et mises en œuvre.',
     'MOYEN', 'Contrôles organisationnels', '5.10', 510),

    (gen_random_uuid(), ref_id,
     'Restitution des actifs',
     'Le personnel et les autres parties intéressées doivent restituer tous les actifs de l''organisme qui sont en leur possession au moment du changement ou de la fin de leur emploi.',
     'MOYEN', 'Contrôles organisationnels', '5.11', 511),

    (gen_random_uuid(), ref_id,
     'Classification des informations',
     'Les informations doivent être classifiées en fonction des exigences de sécurité de l''information de l''organisme selon leur confidentialité, leur intégrité et leur disponibilité.',
     'ELEVE', 'Contrôles organisationnels', '5.12', 512),

    (gen_random_uuid(), ref_id,
     'Marquage des informations',
     'Un ensemble approprié de procédures de marquage des informations doit être élaboré et mis en œuvre conformément au système de classification adopté par l''organisme.',
     'MOYEN', 'Contrôles organisationnels', '5.13', 513),

    (gen_random_uuid(), ref_id,
     'Transfert des informations',
     'Des règles, procédures ou accords formels de transfert des informations doivent être mis en place pour tous les types de supports de communication.',
     'ELEVE', 'Contrôles organisationnels', '5.14', 514),

    (gen_random_uuid(), ref_id,
     'Contrôle d''accès',
     'Des règles pour contrôler l''accès physique et logique aux informations et aux autres actifs doivent être établies et mises en œuvre.',
     'CRITIQUE', 'Contrôles organisationnels', '5.15', 515),

    (gen_random_uuid(), ref_id,
     'Gestion des identités',
     'Le cycle de vie complet des identités doit être géré.',
     'CRITIQUE', 'Contrôles organisationnels', '5.16', 516),

    (gen_random_uuid(), ref_id,
     'Informations d''authentification',
     'L''attribution et la gestion des informations secrètes d''authentification doivent être contrôlées par un processus de gestion approprié.',
     'CRITIQUE', 'Contrôles organisationnels', '5.17', 517),

    (gen_random_uuid(), ref_id,
     'Droits d''accès',
     'Les droits d''accès aux informations et aux autres actifs associés doivent être provisionnés, révisés, modifiés et supprimés en accord avec la politique de contrôle d''accès.',
     'CRITIQUE', 'Contrôles organisationnels', '5.18', 518),

    (gen_random_uuid(), ref_id,
     'Sécurité de l''information dans les relations avec les fournisseurs',
     'Des processus et des procédures doivent être définis et mis en œuvre pour gérer les risques de sécurité de l''information liés à l''utilisation des produits et services des fournisseurs.',
     'ELEVE', 'Contrôles organisationnels', '5.19', 519),

    (gen_random_uuid(), ref_id,
     'Prise en compte de la sécurité dans les accords avec les fournisseurs',
     'Les exigences pertinentes en matière de sécurité de l''information doivent être établies et convenues avec chaque fournisseur selon le type de relation.',
     'ELEVE', 'Contrôles organisationnels', '5.20', 520),

    (gen_random_uuid(), ref_id,
     'Gestion de la sécurité dans la chaîne d''approvisionnement des TIC',
     'Des processus et des procédures doivent être définis pour gérer les risques de sécurité dans la chaîne d''approvisionnement des produits et services TIC.',
     'ELEVE', 'Contrôles organisationnels', '5.21', 521),

    (gen_random_uuid(), ref_id,
     'Suivi, révision et gestion des changements des services des fournisseurs',
     'L''organisme doit surveiller, réviser, évaluer et gérer régulièrement les changements apportés aux pratiques de sécurité de l''information des fournisseurs.',
     'MOYEN', 'Contrôles organisationnels', '5.22', 522),

    (gen_random_uuid(), ref_id,
     'Sécurité de l''information pour l''utilisation des services en nuage',
     'Des processus pour l''acquisition, l''utilisation, la gestion et la fin des services en nuage doivent être établis selon les exigences de sécurité.',
     'ELEVE', 'Contrôles organisationnels', '5.23', 523),

    (gen_random_uuid(), ref_id,
     'Planification et préparation de la gestion des incidents de sécurité',
     'L''organisme doit planifier et se préparer à la gestion des incidents de sécurité de l''information en définissant des processus, des rôles et des responsabilités.',
     'CRITIQUE', 'Contrôles organisationnels', '5.24', 524),

    (gen_random_uuid(), ref_id,
     'Évaluation des événements de sécurité de l''information',
     'Les événements de sécurité de l''information doivent être évalués et une décision doit être prise quant à leur classification comme incidents.',
     'ELEVE', 'Contrôles organisationnels', '5.25', 525),

    (gen_random_uuid(), ref_id,
     'Réponse aux incidents de sécurité de l''information',
     'Les incidents de sécurité de l''information doivent faire l''objet d''une réponse conformément aux procédures documentées, y compris la déclaration à l''ANCS dans les délais réglementaires (72h).',
     'CRITIQUE', 'Contrôles organisationnels', '5.26', 526),

    (gen_random_uuid(), ref_id,
     'Tirer des enseignements des incidents de sécurité de l''information',
     'Les connaissances acquises à l''issue des incidents de sécurité de l''information doivent être utilisées pour réduire la probabilité ou l''impact des incidents futurs.',
     'MOYEN', 'Contrôles organisationnels', '5.27', 527),

    (gen_random_uuid(), ref_id,
     'Collecte des preuves',
     'L''organisme doit établir et mettre en œuvre des procédures pour l''identification, la collecte, l''acquisition et la préservation des preuves liées aux incidents de sécurité.',
     'ELEVE', 'Contrôles organisationnels', '5.28', 528),

    (gen_random_uuid(), ref_id,
     'Sécurité de l''information lors d''une perturbation',
     'L''organisme doit planifier la manière dont il maintient la sécurité de l''information à un niveau approprié lors des perturbations.',
     'CRITIQUE', 'Contrôles organisationnels', '5.29', 529),

    (gen_random_uuid(), ref_id,
     'Préparation des TIC pour la continuité des activités',
     'La préparation des TIC doit être planifiée, mise en œuvre, maintenue et testée sur la base des objectifs de continuité des activités et des exigences de continuité des TIC.',
     'CRITIQUE', 'Contrôles organisationnels', '5.30', 530),

    (gen_random_uuid(), ref_id,
     'Exigences légales, réglementaires et contractuelles',
     'Les exigences légales, réglementaires (dont le décret-loi 2023-17) et contractuelles pertinentes pour la sécurité de l''information doivent être identifiées, documentées et maintenues à jour.',
     'CRITIQUE', 'Contrôles organisationnels', '5.31', 531),

    (gen_random_uuid(), ref_id,
     'Droits de propriété intellectuelle',
     'L''organisme doit mettre en œuvre des procédures appropriées pour protéger les droits de propriété intellectuelle.',
     'MOYEN', 'Contrôles organisationnels', '5.32', 532),

    (gen_random_uuid(), ref_id,
     'Protection des enregistrements',
     'Les enregistrements doivent être protégés contre leur perte, leur destruction, leur falsification, leur accès non autorisé et leur divulgation non autorisée.',
     'ELEVE', 'Contrôles organisationnels', '5.33', 533),

    (gen_random_uuid(), ref_id,
     'Protection de la vie privée et des données à caractère personnel',
     'L''organisme doit identifier et satisfaire les exigences relatives à la préservation de la vie privée et à la protection des données à caractère personnel.',
     'CRITIQUE', 'Contrôles organisationnels', '5.34', 534),

    (gen_random_uuid(), ref_id,
     'Revue indépendante de la sécurité de l''information',
     'L''approche de l''organisme en matière de gestion de la sécurité de l''information et sa mise en œuvre, y compris les personnes, les processus et les technologies, doivent faire l''objet de revues indépendantes.',
     'ELEVE', 'Contrôles organisationnels', '5.35', 535),

    (gen_random_uuid(), ref_id,
     'Conformité aux politiques, règles et normes de sécurité',
     'La conformité à la politique de sécurité de l''information, aux politiques spécifiques, aux règles et aux normes de l''organisme doit être régulièrement vérifiée.',
     'ELEVE', 'Contrôles organisationnels', '5.36', 536),

    (gen_random_uuid(), ref_id,
     'Procédures d''exploitation documentées',
     'Les procédures d''exploitation relatives au traitement de l''information doivent être documentées et mises à la disposition du personnel qui en a besoin.',
     'MOYEN', 'Contrôles organisationnels', '5.37', 537),

    -- ================================================================
    -- DOMAINE 6 : CONTRÔLES LIÉS AUX PERSONNES (6.1 – 6.8)
    -- ================================================================

    (gen_random_uuid(), ref_id,
     'Sélection des candidats',
     'Des vérifications des antécédents de tous les candidats à un emploi doivent être effectuées avant leur embauche et de manière régulière, conformément aux lois, réglementations et exigences éthiques applicables.',
     'ELEVE', 'Contrôles liés aux personnes', '6.1', 601),

    (gen_random_uuid(), ref_id,
     'Conditions d''emploi',
     'Les accords contractuels conclus avec le personnel doivent stipuler les responsabilités qui leur incombent ainsi que celles de l''organisme en matière de sécurité de l''information.',
     'ELEVE', 'Contrôles liés aux personnes', '6.2', 602),

    (gen_random_uuid(), ref_id,
     'Sensibilisation, enseignement et formation à la sécurité de l''information',
     'Le personnel de l''organisme et, le cas échéant, les parties intéressées pertinentes doivent recevoir une sensibilisation, une formation et des instructions appropriées sur la sécurité de l''information.',
     'ELEVE', 'Contrôles liés aux personnes', '6.3', 603),

    (gen_random_uuid(), ref_id,
     'Processus disciplinaire',
     'Un processus disciplinaire formel doit être établi et communiqué pour prendre des mesures à l''égard des membres du personnel ayant commis une violation de la politique de sécurité de l''information.',
     'MOYEN', 'Contrôles liés aux personnes', '6.4', 604),

    (gen_random_uuid(), ref_id,
     'Responsabilités après la fin ou la modification d''un contrat de travail',
     'Les responsabilités et obligations en matière de sécurité de l''information qui restent valables après la fin ou la modification d''un emploi doivent être définies, communiquées et appliquées.',
     'MOYEN', 'Contrôles liés aux personnes', '6.5', 605),

    (gen_random_uuid(), ref_id,
     'Accords de confidentialité et de non-divulgation',
     'Les accords de confidentialité ou de non-divulgation reflétant les besoins de l''organisme en matière de protection des informations doivent être identifiés, documentés, révisés régulièrement et signés.',
     'ELEVE', 'Contrôles liés aux personnes', '6.6', 606),

    (gen_random_uuid(), ref_id,
     'Télétravail',
     'Des mesures de sécurité doivent être mises en œuvre lorsque le personnel travaille à distance pour protéger les informations consultées, traitées ou stockées en dehors des locaux de l''organisme.',
     'MOYEN', 'Contrôles liés aux personnes', '6.7', 607),

    (gen_random_uuid(), ref_id,
     'Signalement des événements de sécurité de l''information',
     'L''organisme doit fournir un mécanisme permettant au personnel de signaler les événements de sécurité de l''information observés ou suspectés via les canaux de direction appropriés.',
     'ELEVE', 'Contrôles liés aux personnes', '6.8', 608),

    -- ================================================================
    -- DOMAINE 7 : CONTRÔLES PHYSIQUES (7.1 – 7.14)
    -- ================================================================

    (gen_random_uuid(), ref_id,
     'Périmètres de sécurité physique',
     'Des périmètres de sécurité doivent être définis et utilisés pour protéger les zones qui contiennent des informations et autres actifs associés.',
     'ELEVE', 'Contrôles physiques', '7.1', 701),

    (gen_random_uuid(), ref_id,
     'Entrée physique',
     'Les zones sécurisées doivent être protégées par des contrôles d''entrée appropriés et des points d''accès afin que seul le personnel autorisé soit admis.',
     'CRITIQUE', 'Contrôles physiques', '7.2', 702),

    (gen_random_uuid(), ref_id,
     'Sécurisation des bureaux, des salles et des installations',
     'Une sécurité physique doit être conçue et mise en œuvre pour les bureaux, les salles et les installations.',
     'MOYEN', 'Contrôles physiques', '7.3', 703),

    (gen_random_uuid(), ref_id,
     'Surveillance de la sécurité physique',
     'Les locaux doivent être continuellement surveillés pour détecter tout accès physique non autorisé.',
     'ELEVE', 'Contrôles physiques', '7.4', 704),

    (gen_random_uuid(), ref_id,
     'Protection contre les menaces physiques et environnementales',
     'Une protection contre les menaces physiques et environnementales, telles que les catastrophes naturelles et autres menaces intentionnelles ou accidentelles pour l''infrastructure, doit être conçue et mise en œuvre.',
     'ELEVE', 'Contrôles physiques', '7.5', 705),

    (gen_random_uuid(), ref_id,
     'Travail dans les zones sécurisées',
     'Des mesures de sécurité pour le travail dans les zones sécurisées doivent être conçues et mises en œuvre.',
     'MOYEN', 'Contrôles physiques', '7.6', 706),

    (gen_random_uuid(), ref_id,
     'Bureau vide et écran vide',
     'Des règles pour bureau vide et écran vide doivent être définies et appliquées de manière appropriée.',
     'MOYEN', 'Contrôles physiques', '7.7', 707),

    (gen_random_uuid(), ref_id,
     'Emplacement et protection du matériel',
     'Le matériel doit être placé de façon à réduire les risques liés aux menaces physiques et environnementales et aux possibilités d''accès non autorisé.',
     'MOYEN', 'Contrôles physiques', '7.8', 708),

    (gen_random_uuid(), ref_id,
     'Sécurité des actifs hors des locaux',
     'Les actifs hors site doivent être protégés en prenant en compte les différents risques liés au fait de travailler hors des locaux de l''organisme.',
     'ELEVE', 'Contrôles physiques', '7.9', 709),

    (gen_random_uuid(), ref_id,
     'Supports de stockage',
     'Les supports de stockage doivent être gérés tout au long de leur cycle de vie d''acquisition, d''utilisation, de transport et de mise au rebut conformément au système de classification et aux exigences de gestion de l''organisme.',
     'ELEVE', 'Contrôles physiques', '7.10', 710),

    (gen_random_uuid(), ref_id,
     'Services supports',
     'Les équipements de traitement de l''information doivent être protégés contre les pannes de courant et autres perturbations causées par des défaillances des services supports.',
     'CRITIQUE', 'Contrôles physiques', '7.11', 711),

    (gen_random_uuid(), ref_id,
     'Sécurité du câblage',
     'Les câbles transportant l''alimentation électrique ou les télécommunications ou les données doivent être protégés contre les interceptions, les interférences ou les dommages.',
     'MOYEN', 'Contrôles physiques', '7.12', 712),

    (gen_random_uuid(), ref_id,
     'Maintenance du matériel',
     'Le matériel doit être correctement entretenu pour assurer la disponibilité continue et l''intégrité des informations.',
     'MOYEN', 'Contrôles physiques', '7.13', 713),

    (gen_random_uuid(), ref_id,
     'Mise au rebut ou recyclage sécurisé du matériel',
     'Les éléments du matériel contenant des supports de stockage doivent être vérifiés pour garantir que les données sensibles et les logiciels sous licence ont été retirés ou écrasés de manière sécurisée avant leur mise au rebut ou leur réutilisation.',
     'CRITIQUE', 'Contrôles physiques', '7.14', 714),

    -- ================================================================
    -- DOMAINE 8 : CONTRÔLES TECHNOLOGIQUES (8.1 – 8.34)
    -- ================================================================

    (gen_random_uuid(), ref_id,
     'Terminaux utilisateurs',
     'Les informations stockées sur les terminaux utilisateurs, traitées par ceux-ci ou accessibles via ceux-ci doivent être protégées.',
     'ELEVE', 'Contrôles technologiques', '8.1', 801),

    (gen_random_uuid(), ref_id,
     'Droits d''accès privilégiés',
     'L''attribution et l''utilisation des droits d''accès privilégiés doivent être restreintes et gérées.',
     'CRITIQUE', 'Contrôles technologiques', '8.2', 802),

    (gen_random_uuid(), ref_id,
     'Restriction d''accès à l''information',
     'L''accès aux informations et aux autres actifs associés doit être restreint conformément à la politique de contrôle d''accès établie.',
     'CRITIQUE', 'Contrôles technologiques', '8.3', 803),

    (gen_random_uuid(), ref_id,
     'Accès au code source',
     'L''accès en lecture et en écriture au code source, aux outils de développement et aux bibliothèques logicielles doit être géré de façon appropriée.',
     'ELEVE', 'Contrôles technologiques', '8.4', 804),

    (gen_random_uuid(), ref_id,
     'Authentification sécurisée',
     'Des technologies et des procédures d''authentification sécurisée doivent être mises en œuvre en fonction des restrictions d''accès aux informations et de la politique de contrôle d''accès.',
     'CRITIQUE', 'Contrôles technologiques', '8.5', 805),

    (gen_random_uuid(), ref_id,
     'Dimensionnement',
     'L''utilisation des ressources doit être surveillée et ajustée conformément aux exigences de dimensionnement actuelles et prévisionnelles.',
     'MOYEN', 'Contrôles technologiques', '8.6', 806),

    (gen_random_uuid(), ref_id,
     'Protection contre les logiciels malveillants',
     'Des mesures de protection contre les logiciels malveillants doivent être mises en œuvre et soutenues par une sensibilisation appropriée des utilisateurs.',
     'CRITIQUE', 'Contrôles technologiques', '8.7', 807),

    (gen_random_uuid(), ref_id,
     'Gestion des vulnérabilités techniques',
     'Des informations sur les vulnérabilités techniques des systèmes d''information en exploitation doivent être obtenues, l''exposition de l''organisme à ces vulnérabilités évaluée et les mesures appropriées prises.',
     'CRITIQUE', 'Contrôles technologiques', '8.8', 808),

    (gen_random_uuid(), ref_id,
     'Gestion des configurations',
     'Les configurations des matériels, logiciels, services et réseaux doivent être établies, documentées, mises en œuvre, surveillées et revues.',
     'ELEVE', 'Contrôles technologiques', '8.9', 809),

    (gen_random_uuid(), ref_id,
     'Suppression des informations',
     'Les informations stockées dans les systèmes d''information, les dispositifs ou tout autre support de stockage doivent être supprimées lorsqu''elles ne sont plus nécessaires.',
     'MOYEN', 'Contrôles technologiques', '8.10', 810),

    (gen_random_uuid(), ref_id,
     'Masquage des données',
     'Le masquage des données doit être utilisé conformément à la politique de contrôle d''accès de l''organisme et aux autres politiques spécifiques, ainsi qu''aux exigences métier, légales et réglementaires.',
     'ELEVE', 'Contrôles technologiques', '8.11', 811),

    (gen_random_uuid(), ref_id,
     'Prévention de la fuite de données',
     'Des mesures de prévention de la fuite de données doivent être appliquées aux systèmes, réseaux et tout autre dispositif qui traitent, stockent ou transmettent des informations sensibles.',
     'CRITIQUE', 'Contrôles technologiques', '8.12', 812),

    (gen_random_uuid(), ref_id,
     'Sauvegarde des informations',
     'Des copies de sauvegarde des informations, des logiciels et des systèmes doivent être effectuées et testées régulièrement conformément à la politique de sauvegarde convenue.',
     'CRITIQUE', 'Contrôles technologiques', '8.13', 813),

    (gen_random_uuid(), ref_id,
     'Redondance des équipements de traitement de l''information',
     'Les équipements de traitement de l''information doivent être mis en œuvre avec une redondance suffisante pour satisfaire les exigences de disponibilité.',
     'CRITIQUE', 'Contrôles technologiques', '8.14', 814),

    (gen_random_uuid(), ref_id,
     'Journalisation',
     'Des journaux qui enregistrent les activités, les exceptions, les défaillances et autres événements pertinents doivent être produits, stockés, protégés et analysés.',
     'CRITIQUE', 'Contrôles technologiques', '8.15', 815),

    (gen_random_uuid(), ref_id,
     'Activités de surveillance',
     'Les réseaux, les systèmes et les applications doivent être surveillés pour détecter les comportements anormaux, et des actions appropriées doivent être engagées pour évaluer les incidents de sécurité de l''information potentiels.',
     'ELEVE', 'Contrôles technologiques', '8.16', 816),

    (gen_random_uuid(), ref_id,
     'Synchronisation des horloges',
     'Les horloges des systèmes de traitement de l''information utilisés par l''organisme doivent être synchronisées avec des sources de temps approuvées.',
     'MOYEN', 'Contrôles technologiques', '8.17', 817),

    (gen_random_uuid(), ref_id,
     'Utilisation de programmes utilitaires privilégiés',
     'L''utilisation de programmes utilitaires susceptibles de contourner les contrôles des systèmes et des applications doit être restreinte et strictement contrôlée.',
     'ELEVE', 'Contrôles technologiques', '8.18', 818),

    (gen_random_uuid(), ref_id,
     'Installation de logiciels sur des systèmes en exploitation',
     'Des procédures et des mesures doivent être mises en œuvre pour gérer de façon sécurisée l''installation de logiciels sur des systèmes en exploitation.',
     'ELEVE', 'Contrôles technologiques', '8.19', 819),

    (gen_random_uuid(), ref_id,
     'Sécurité des réseaux',
     'Les réseaux et les équipements réseau doivent être sécurisés, gérés et contrôlés pour protéger les informations dans les systèmes et les applications.',
     'CRITIQUE', 'Contrôles technologiques', '8.20', 820),

    (gen_random_uuid(), ref_id,
     'Sécurité des services en réseau',
     'Les mécanismes de sécurité, les niveaux de service et les exigences de gestion de tous les services réseau doivent être identifiés, mis en œuvre et surveillés.',
     'ELEVE', 'Contrôles technologiques', '8.21', 821),

    (gen_random_uuid(), ref_id,
     'Cloisonnement des réseaux',
     'Les groupes de services d''information, d''utilisateurs et de systèmes d''information doivent être cloisonnés dans les réseaux de l''organisme.',
     'CRITIQUE', 'Contrôles technologiques', '8.22', 822),

    (gen_random_uuid(), ref_id,
     'Filtrage web',
     'L''accès à des sites web externes doit être géré pour réduire l''exposition aux contenus malveillants.',
     'ELEVE', 'Contrôles technologiques', '8.23', 823),

    (gen_random_uuid(), ref_id,
     'Utilisation de la cryptographie',
     'Des règles pour l''utilisation efficace de la cryptographie, dont la gestion des clés cryptographiques, doivent être définies et mises en œuvre.',
     'CRITIQUE', 'Contrôles technologiques', '8.24', 824),

    (gen_random_uuid(), ref_id,
     'Cycle de développement sécurisé',
     'Des règles pour le développement sécurisé des logiciels et des systèmes doivent être établies et appliquées.',
     'ELEVE', 'Contrôles technologiques', '8.25', 825),

    (gen_random_uuid(), ref_id,
     'Exigences de sécurité des applications',
     'Les exigences de sécurité de l''information doivent être identifiées, spécifiées et approuvées lors du développement ou de l''acquisition d''applications.',
     'ELEVE', 'Contrôles technologiques', '8.26', 826),

    (gen_random_uuid(), ref_id,
     'Principes d''architecture et d''ingénierie de systèmes sécurisés',
     'Des principes pour la conception des systèmes sécurisés doivent être établis, documentés, maintenus et appliqués aux activités d''ingénierie des systèmes d''information.',
     'ELEVE', 'Contrôles technologiques', '8.27', 827),

    (gen_random_uuid(), ref_id,
     'Codage sécurisé',
     'Des principes de codage sécurisé doivent être appliqués au développement de logiciels.',
     'ELEVE', 'Contrôles technologiques', '8.28', 828),

    (gen_random_uuid(), ref_id,
     'Tests de sécurité en développement et en acceptation',
     'Des processus de tests de sécurité doivent être définis et mis en œuvre dans le cycle de développement.',
     'ELEVE', 'Contrôles technologiques', '8.29', 829),

    (gen_random_uuid(), ref_id,
     'Tests d''intrusion',
     'Des tests d''intrusion doivent être planifiés, conçus, préparés et exécutés sur les systèmes, réseaux et applications.',
     'CRITIQUE', 'Contrôles technologiques', '8.30', 830),

    (gen_random_uuid(), ref_id,
     'Séparation des environnements de développement, de test et de production',
     'Les environnements de développement, de test et de production doivent être séparés et sécurisés.',
     'ELEVE', 'Contrôles technologiques', '8.31', 831),

    (gen_random_uuid(), ref_id,
     'Gestion des changements',
     'Les changements apportés à l''organisation, aux processus métier, aux équipements de traitement de l''information et aux systèmes doivent être soumis à des procédures de gestion des changements.',
     'ELEVE', 'Contrôles technologiques', '8.32', 832),

    (gen_random_uuid(), ref_id,
     'Informations de test',
     'Les informations de test doivent être sélectionnées, protégées et gérées de manière appropriée.',
     'MOYEN', 'Contrôles technologiques', '8.33', 833),

    (gen_random_uuid(), ref_id,
     'Protection des systèmes d''information lors d''un audit',
     'Les audits et autres activités d''assurance impliquant une évaluation des systèmes en exploitation doivent être planifiés et convenus entre les parties concernées.',
     'MOYEN', 'Contrôles technologiques', '8.34', 834);

    RAISE NOTICE 'Référentiel ANCS v3.1 chargé avec succès (93 contrôles, 4 domaines) pour ref_id = %', ref_id;
END $$;
