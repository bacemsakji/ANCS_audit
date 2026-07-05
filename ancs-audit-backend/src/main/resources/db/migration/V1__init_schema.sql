-- ============================================================
-- V1 — Schéma initial ANCS Audit
-- Toutes les tables, index et contraintes de base
-- ============================================================

-- Extension UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ORGANISME
-- ============================================================
CREATE TABLE organisme (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                  VARCHAR(255) NOT NULL,
    secteur_activite     VARCHAR(100),
    type_obligation_audit VARCHAR(50),
    adresse              TEXT,
    contact_rssi_email   VARCHAR(255),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

COMMENT ON TABLE organisme IS 'Organisme soumis à obligation d''audit SI (Décret-loi 2023-17)';
COMMENT ON COLUMN organisme.type_obligation_audit IS 'SOUMIS_AUDIT | VOLONTAIRE';

-- ============================================================
-- UTILISATEUR
-- ============================================================
CREATE TABLE utilisateur (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nom           VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    organisme_id  UUID         REFERENCES organisme(id) ON DELETE SET NULL,
    totp_secret   VARCHAR(255),
    totp_enabled  BOOLEAN      NOT NULL DEFAULT false,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    fcm_token     VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

COMMENT ON TABLE utilisateur IS 'Utilisateurs de la plateforme (Admin ANCS, Auditeur, RSSI)';
COMMENT ON COLUMN utilisateur.role IS 'ADMIN_ANCS | AUDITEUR | RSSI';
COMMENT ON COLUMN utilisateur.totp_secret IS 'Secret TOTP Base32 — obligatoire pour ADMIN_ANCS';
COMMENT ON COLUMN utilisateur.password_hash IS 'Hash BCrypt force 12 — JAMAIS stocker en clair';

CREATE INDEX idx_utilisateur_email   ON utilisateur(email);
CREATE INDEX idx_utilisateur_role    ON utilisateur(role);
CREATE INDEX idx_utilisateur_organisme ON utilisateur(organisme_id);

-- ============================================================
-- AUDITEUR
-- ============================================================
CREATE TABLE auditeur (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    utilisateur_id       UUID        NOT NULL UNIQUE REFERENCES utilisateur(id) ON DELETE CASCADE,
    numero_certification VARCHAR(100) NOT NULL UNIQUE,
    date_certification   DATE        NOT NULL,
    date_expiration      DATE        NOT NULL,
    specialites          JSONB,
    statut               VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ,
    CONSTRAINT chk_auditeur_statut CHECK (statut IN ('ACTIF', 'EXPIRE', 'REVOQUE'))
);

COMMENT ON TABLE auditeur IS 'Profil de certification ANCS des auditeurs';
COMMENT ON COLUMN auditeur.specialites IS 'Liste JSON des domaines de spécialité (ex: ["ISO 27001","EBIOS RM"])';

CREATE INDEX idx_auditeur_numero_certification ON auditeur(numero_certification);
CREATE INDEX idx_auditeur_date_expiration      ON auditeur(date_expiration);
CREATE INDEX idx_auditeur_statut               ON auditeur(statut);

-- ============================================================
-- REFERENTIEL
-- ============================================================
CREATE TABLE referentiel (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nom         VARCHAR(255) NOT NULL,
    type        VARCHAR(30),
    version     VARCHAR(50),
    source_url  TEXT,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    CONSTRAINT chk_referentiel_type CHECK (
        type IN ('LOI', 'NORME', 'METHODOLOGIE', 'CONTROLE_TECHNIQUE')
    )
);

COMMENT ON TABLE referentiel IS 'Bibliothèque réglementaire ANCS (lois, normes, méthodologies, contrôles)';

CREATE INDEX idx_referentiel_type ON referentiel(type);

-- ============================================================
-- CONTROLE
-- ============================================================
CREATE TABLE controle (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    referentiel_id   UUID         NOT NULL REFERENCES referentiel(id) ON DELETE CASCADE,
    libelle          VARCHAR(500) NOT NULL,
    description      TEXT,
    criticite        VARCHAR(20),
    categorie        VARCHAR(100),
    ordre_affichage  INT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ,
    CONSTRAINT chk_controle_criticite CHECK (
        criticite IN ('FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE')
    )
);

COMMENT ON TABLE controle IS 'Critères de contrôle de la checklist d''audit';

CREATE INDEX idx_controle_referentiel     ON controle(referentiel_id);
CREATE INDEX idx_controle_categorie       ON controle(referentiel_id, categorie);
CREATE INDEX idx_controle_criticite       ON controle(criticite);
CREATE INDEX idx_controle_ordre_affichage ON controle(referentiel_id, ordre_affichage);

-- ============================================================
-- MISSION
-- ============================================================
CREATE TABLE mission (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organisme_id   UUID        NOT NULL REFERENCES organisme(id),
    auditeur_id    UUID        NOT NULL REFERENCES auditeur(id),
    referentiel_id UUID        NOT NULL REFERENCES referentiel(id),
    date_debut     DATE,
    date_fin       DATE,
    statut         VARCHAR(30) NOT NULL DEFAULT 'PLANIFIEE',
    perimetre      TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    CONSTRAINT chk_mission_statut CHECK (
        statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE')
    )
);

COMMENT ON TABLE mission IS 'Mission d''audit assignée à un auditeur pour un organisme';

CREATE INDEX idx_mission_organisme  ON mission(organisme_id);
CREATE INDEX idx_mission_auditeur   ON mission(auditeur_id);
CREATE INDEX idx_mission_statut     ON mission(statut);
CREATE INDEX idx_mission_dates      ON mission(date_debut, date_fin);

-- ============================================================
-- CONSTAT
-- ============================================================
CREATE TABLE constat (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id   UUID        NOT NULL REFERENCES mission(id) ON DELETE CASCADE,
    controle_id  UUID        NOT NULL REFERENCES controle(id),
    resultat     VARCHAR(20),
    preuve_url   TEXT,
    commentaire  TEXT,
    date_constat TIMESTAMPTZ NOT NULL DEFAULT now(),
    synced       BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ,
    CONSTRAINT chk_constat_resultat CHECK (
        resultat IN ('CONFORME', 'NON_CONFORME', 'OBSERVATION')
    ),
    -- Un contrôle ne peut avoir qu'un seul constat par mission
    UNIQUE (mission_id, controle_id)
);

COMMENT ON TABLE constat IS 'Constat d''audit par contrôle — saisi terrain, mode offline possible';
COMMENT ON COLUMN constat.synced IS 'false = enregistré localement, pas encore transmis au serveur';

CREATE INDEX idx_constat_mission  ON constat(mission_id);
CREATE INDEX idx_constat_controle ON constat(controle_id);
CREATE INDEX idx_constat_resultat ON constat(resultat);
CREATE INDEX idx_constat_synced   ON constat(synced) WHERE synced = false;

-- ============================================================
-- ACTION
-- ============================================================
CREATE TABLE action (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    constat_id  UUID        NOT NULL REFERENCES constat(id) ON DELETE CASCADE,
    description TEXT        NOT NULL,
    responsable VARCHAR(255),
    echeance    DATE,
    priorite    VARCHAR(20),
    statut      VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    CONSTRAINT chk_action_priorite CHECK (
        priorite IN ('FAIBLE', 'MOYENNE', 'HAUTE', 'CRITIQUE')
    ),
    CONSTRAINT chk_action_statut CHECK (
        statut IN ('A_FAIRE', 'EN_COURS', 'CLOTUREE', 'EN_RETARD')
    )
);

COMMENT ON TABLE action IS 'Action corrective générée à partir d''un constat NON_CONFORME';

CREATE INDEX idx_action_constat          ON action(constat_id);
CREATE INDEX idx_action_statut           ON action(statut);
CREATE INDEX idx_action_echeance_statut  ON action(echeance, statut) WHERE statut != 'CLOTUREE';
CREATE INDEX idx_action_priorite         ON action(priorite);

-- ============================================================
-- RAPPORT
-- ============================================================
CREATE TABLE rapport (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id               UUID        NOT NULL REFERENCES mission(id),
    fichier_url              TEXT,
    date_generation          TIMESTAMPTZ DEFAULT now(),
    version                  INT         NOT NULL DEFAULT 1,
    type                     VARCHAR(10),
    synthese_generee_par_ia  BOOLEAN     NOT NULL DEFAULT false,
    synthese_ia_horodatage   TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ,
    CONSTRAINT chk_rapport_type CHECK (type IN ('PDF', 'DOCX'))
);

COMMENT ON TABLE rapport IS 'Rapport d''audit officiel ANCS (PDF et/ou DOCX)';
COMMENT ON COLUMN rapport.synthese_generee_par_ia IS
    'Trace qu''un brouillon IA a été généré — indépendant du contenu final édité';
COMMENT ON COLUMN rapport.synthese_ia_horodatage IS
    'Horodatage de la dernière génération IA de la synthèse';

CREATE INDEX idx_rapport_mission ON rapport(mission_id);

-- ============================================================
-- AUDIT_LOG
-- ============================================================
CREATE TABLE audit_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    utilisateur_id  UUID        REFERENCES utilisateur(id) ON DELETE SET NULL,
    action          VARCHAR(100),
    resource        VARCHAR(100),
    resource_id     UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE audit_log IS 'Journal immuable des accès applicatifs — traçabilité ANCS';

CREATE INDEX idx_audit_log_utilisateur  ON audit_log(utilisateur_id, created_at DESC);
CREATE INDEX idx_audit_log_resource     ON audit_log(resource, resource_id);
CREATE INDEX idx_audit_log_created_at   ON audit_log(created_at DESC);
CREATE INDEX idx_audit_log_action       ON audit_log(action);
