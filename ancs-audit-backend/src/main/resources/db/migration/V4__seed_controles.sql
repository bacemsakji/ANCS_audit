-- ============================================================
-- V4 — Seed : Référentiel de contrôles techniques ANCS
-- Données représentatives organisées par domaine
-- Basées sur la structure du référentiel ANCS 2019
-- ============================================================

-- Récupération de l'ID du référentiel technique ANCS
-- (créé dans V3 comme type CONTROLE_TECHNIQUE)
DO $$
DECLARE
    ref_id UUID;
BEGIN
    -- Créer le référentiel de contrôles techniques ANCS si absent
    INSERT INTO referentiel (id, nom, type, version, source_url, description)
    VALUES (
        gen_random_uuid(),
        'Référentiel de contrôles techniques ANCS',
        'CONTROLE_TECHNIQUE',
        '2.0',
        'https://www.ancs.gov.tn/referentiel',
        'Liste officielle des contrôles de sécurité à vérifier lors d''un audit ANCS. '
        'Organisée par domaines : Gouvernance, IAM, Réseau, Cryptographie, SI, '
        'Continuité, Conformité.'
    )
    ON CONFLICT DO NOTHING
    RETURNING id INTO ref_id;

    -- Si déjà existant, récupérer l'ID
    IF ref_id IS NULL THEN
        SELECT id INTO ref_id FROM referentiel
        WHERE type = 'CONTROLE_TECHNIQUE' AND nom LIKE 'Référentiel de contrôles%'
        LIMIT 1;
    END IF;

    -- ========================================================
    -- DOMAINE 1 : GOUVERNANCE DE LA SÉCURITÉ SI
    -- ========================================================
    INSERT INTO controle (id, referentiel_id, libelle, description, criticite, categorie, ordre_affichage)
    VALUES
    (gen_random_uuid(), ref_id,
     'Politique de sécurité SI formalisée et approuvée par la direction',
     'Vérifier l''existence d''une PSSI formalisée, approuvée par la direction générale, '
     'communiquée à l''ensemble du personnel et révisée au moins tous les 2 ans.',
     'ELEVE', 'Gouvernance', 101),

    (gen_random_uuid(), ref_id,
     'Comité de sécurité SI constitué et opérationnel',
     'Vérifier l''existence et le fonctionnement d''un comité de sécurité avec des '
     'réunions régulières (a minima trimestrielles) et des PV documentés.',
     'MOYEN', 'Gouvernance', 102),

    (gen_random_uuid(), ref_id,
     'Rôle de RSSI désigné avec fiche de poste et rattachement hiérarchique',
     'Le RSSI doit être nommément désigné, avoir une fiche de poste formalisée '
     'et un rattachement hiérarchique garantissant son indépendance.',
     'ELEVE', 'Gouvernance', 103),

    (gen_random_uuid(), ref_id,
     'Cartographie des actifs informationnels à jour',
     'L''organisme dispose d''un inventaire exhaustif et à jour de ses actifs '
     'informationnels (données, applications, infrastructures) avec classification '
     'par niveau de sensibilité.',
     'CRITIQUE', 'Gouvernance', 104),

    (gen_random_uuid(), ref_id,
     'Plan de traitement des risques SI formalisé',
     'Un plan de traitement des risques SI est formalisé, validé par la direction '
     'et fait l''objet d''un suivi régulier des actions de traitement.',
     'ELEVE', 'Gouvernance', 105),

    -- ========================================================
    -- DOMAINE 2 : GESTION DES IDENTITÉS ET DES ACCÈS (IAM)
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Procédure de gestion du cycle de vie des comptes utilisateurs',
     'Vérifier l''existence d''une procédure documentée de création, modification '
     'et suppression des comptes utilisateurs, incluant les départs et mutations.',
     'CRITIQUE', 'IAM', 201),

    (gen_random_uuid(), ref_id,
     'Principe du moindre privilège appliqué sur les droits d''accès',
     'Les droits d''accès sont attribués selon le principe du moindre privilège. '
     'Une revue des droits est réalisée au minimum annuellement.',
     'CRITIQUE', 'IAM', 202),

    (gen_random_uuid(), ref_id,
     'Authentification multi-facteurs (MFA) sur les accès sensibles',
     'L''authentification multi-facteurs est obligatoire pour les accès aux systèmes '
     'critiques, aux interfaces d''administration et aux accès distants.',
     'CRITIQUE', 'IAM', 203),

    (gen_random_uuid(), ref_id,
     'Politique de mots de passe conforme aux exigences ANCS',
     'La politique de mots de passe respecte les exigences minimales : longueur >= 12 '
     'caractères, complexité, historique, verrouillage après échecs.',
     'ELEVE', 'IAM', 204),

    (gen_random_uuid(), ref_id,
     'Gestion des comptes privilégiés (administrateurs, comptes à droits étendus)',
     'Les comptes à privilèges sont recensés, leurs usages traçés, et leur nombre '
     'est limité au strict nécessaire. Une solution PAM est recommandée.',
     'CRITIQUE', 'IAM', 205),

    -- ========================================================
    -- DOMAINE 3 : SÉCURITÉ DU RÉSEAU
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Architecture réseau segmentée (DMZ, zones de confiance distinctes)',
     'L''architecture réseau intègre une segmentation en zones de confiance '
     '(Internet, DMZ, LAN, OT…) avec des politiques de filtrage entre chaque zone.',
     'CRITIQUE', 'Réseau', 301),

    (gen_random_uuid(), ref_id,
     'Firewall(s) déployé(s) et règles documentées et révisées',
     'Des pare-feux sont déployés aux frontières du réseau. Les règles de filtrage '
     'sont documentées, auditées et révisées au minimum annuellement. '
     'Règles "deny all" par défaut.',
     'CRITIQUE', 'Réseau', 302),

    (gen_random_uuid(), ref_id,
     'Détection et prévention des intrusions (IDS/IPS) opérationnelle',
     'Des systèmes de détection/prévention des intrusions sont déployés aux '
     'points stratégiques du réseau et leurs alertes sont monitorées.',
     'ELEVE', 'Réseau', 303),

    (gen_random_uuid(), ref_id,
     'Chiffrement des communications sensibles (TLS 1.2 minimum)',
     'Toutes les communications véhiculant des données sensibles sont chiffrées '
     '(TLS 1.2 ou supérieur). Les protocoles obsolètes (SSL, TLS 1.0/1.1) sont '
     'désactivés.',
     'CRITIQUE', 'Réseau', 304),

    (gen_random_uuid(), ref_id,
     'Gestion sécurisée des accès distants (VPN, bastion)',
     'Les accès distants sont sécurisés via VPN avec authentification forte. '
     'Un bastion (jump server) est utilisé pour les accès d''administration.',
     'ELEVE', 'Réseau', 305),

    -- ========================================================
    -- DOMAINE 4 : CRYPTOGRAPHIE
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Politique de gestion des clés cryptographiques formalisée',
     'Une politique de gestion du cycle de vie des clés cryptographiques est '
     'documentée : génération, stockage sécurisé (HSM recommandé), rotation, '
     'révocation et destruction.',
     'ELEVE', 'Cryptographie', 401),

    (gen_random_uuid(), ref_id,
     'Algorithmes cryptographiques approuvés et à jour',
     'L''organisme n''utilise que des algorithmes approuvés par les standards en '
     'vigueur (AES-256, RSA-4096, SHA-256+, ECDH P-256+). Absence de MD5, SHA-1, '
     'DES dans les usages de production.',
     'CRITIQUE', 'Cryptographie', 402),

    -- ========================================================
    -- DOMAINE 5 : SÉCURITÉ DES SYSTÈMES ET APPLICATIONS
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Gestion des correctifs de sécurité (patch management)',
     'Un processus de gestion des correctifs de sécurité est en place avec des '
     'délais de déploiement définis selon la criticité (critique : < 72h, '
     'haut : < 1 mois, moyen : < 3 mois).',
     'CRITIQUE', 'Systèmes & Applications', 501),

    (gen_random_uuid(), ref_id,
     'Durcissement des configurations systèmes (hardening)',
     'Les systèmes d''exploitation et applications sont durcis selon des référentiels '
     'de configuration sécurisée (CIS Benchmarks ou équivalent). '
     'Vérifications périodiques réalisées.',
     'ELEVE', 'Systèmes & Applications', 502),

    (gen_random_uuid(), ref_id,
     'Antivirus/EDR déployé et mis à jour sur l''ensemble du parc',
     'Une solution de protection des postes (antivirus ou EDR) est déployée sur '
     'l''ensemble du parc, mise à jour automatiquement, et ses alertes sont traitées.',
     'ELEVE', 'Systèmes & Applications', 503),

    (gen_random_uuid(), ref_id,
     'Journalisation centralisée et monitoring de sécurité (SIEM)',
     'Les événements de sécurité sont journalisés de façon centralisée. '
     'Un SIEM ou équivalent analyse les logs. Les journaux sont conservés '
     'au minimum 1 an.',
     'ELEVE', 'Systèmes & Applications', 504),

    -- ========================================================
    -- DOMAINE 6 : CONTINUITÉ D'ACTIVITÉ
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Plan de continuité d''activité (PCA) et plan de reprise (PRA) formalisés',
     'L''organisme dispose d''un PCA et d''un PRA documentés, approuvés par la '
     'direction, et testés au moins annuellement. Les RTO/RPO sont définis '
     'pour les systèmes critiques.',
     'CRITIQUE', 'Continuité', 601),

    (gen_random_uuid(), ref_id,
     'Sauvegardes régulières testées et stockées hors site',
     'Des sauvegardes régulières sont réalisées selon la règle 3-2-1 '
     '(3 copies, 2 supports, 1 hors site). La restauration est testée '
     'au moins semestriellement.',
     'CRITIQUE', 'Continuité', 602),

    -- ========================================================
    -- DOMAINE 7 : CONFORMITÉ ET AUDIT
    -- ========================================================
    (gen_random_uuid(), ref_id,
     'Audits de sécurité internes réalisés selon la périodicité réglementaire',
     'L''organisme réalise des audits de sécurité internes à la fréquence requise '
     'par le Décret-loi 2023-17 et l''Arrêté du 01/10/2019.',
     'CRITIQUE', 'Conformité', 701),

    (gen_random_uuid(), ref_id,
     'Déclaration des incidents de sécurité à l''ANCS dans les délais',
     'L''organisme dispose d''une procédure de gestion et de déclaration des '
     'incidents de sécurité à l''ANCS dans les délais réglementaires (72h '
     'pour les incidents critiques).',
     'CRITIQUE', 'Conformité', 702),

    (gen_random_uuid(), ref_id,
     'Sensibilisation et formation du personnel à la sécurité SI',
     'Un programme de sensibilisation à la sécurité SI est en place pour '
     'l''ensemble du personnel (a minima annuel). Les formations sont documentées '
     'et leur efficacité mesurée.',
     'MOYEN', 'Conformité', 703);

END $$;
