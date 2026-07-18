-- ============================================================
-- V5 — Seed : Utilisateur administrateur ANCS par défaut
--
-- ⚠️  SÉCURITÉ : Changer le mot de passe IMMÉDIATEMENT après
-- le premier déploiement via l'interface d'administration.
-- Le hash ci-dessous correspond à : Admin@ANCS2024!
-- (BCrypt force 12)
-- ============================================================

-- Utilisateur admin ANCS
INSERT INTO utilisateur (
    id, nom, email, password_hash, role,
    totp_enabled, is_active,
    created_at
)
VALUES (
    gen_random_uuid(),
    'Administrateur ANCS',
    'admin@ancs.gov.tn',
    -- Hash BCrypt(12) de "Admin@ANCS2024!" — CHANGER EN PRODUCTION
    '$2a$12$u0PAGakYWFotxDxRSnqJ0OTSxjW5PZ0OnGRHLGKtD4VymzPtcU8.2',
    'ADMIN_ANCS',
    false,   -- 2FA à configurer lors de la première connexion
    true,
    now()
)
ON CONFLICT (email) DO NOTHING;
