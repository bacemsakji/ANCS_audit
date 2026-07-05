-- ============================================================
-- V3 — Seed : Référentiel réglementaire ANCS
-- Données représentatives (structure réelle, contenu fictif)
-- ============================================================

INSERT INTO referentiel (id, nom, type, version, source_url, description)
VALUES

-- ---- Textes législatifs tunisiens -------------------------
(gen_random_uuid(),
 'Décret-loi n° 2023-17 relatif à la cybersécurité',
 'LOI',
 '2023',
 'https://legislation.tn/decret-loi-2023-17',
 'Texte fondateur de la réglementation cybersécurité en Tunisie. '
 'Définit les obligations d''audit périodique des systèmes d''information '
 'des organismes publics et des opérateurs d''importance vitale.'),

(gen_random_uuid(),
 'Arrêté du 01 octobre 2019 fixant les modalités d''audit',
 'LOI',
 '2019',
 'https://legislation.tn/arrete-01-10-2019',
 'Arrêté précisant les modalités pratiques de réalisation des audits de sécurité SI, '
 'la qualification des auditeurs, et le format des rapports d''audit.'),

(gen_random_uuid(),
 'Circulaire ANCS n° 001/2022 — Bonnes pratiques SI',
 'LOI',
 '2022',
 'https://www.ancs.gov.tn/circulaires/001-2022',
 'Circulaire de l''ANCS précisant les bonnes pratiques de sécurité des systèmes '
 'd''information applicables aux organismes soumis à l''obligation d''audit.'),

-- ---- Normes internationales --------------------------------
(gen_random_uuid(),
 'ISO/IEC 27001:2022 — Systèmes de management de la sécurité de l''information',
 'NORME',
 '2022',
 'https://www.iso.org/standard/27001',
 'Norme internationale de référence pour la mise en place, la maintenance et '
 'l''amélioration continue d''un SMSI. Définit 93 mesures de sécurité organisées '
 'en 4 thèmes : organisationnel, personnes, physique, technologique.'),

(gen_random_uuid(),
 'ISO/IEC 27004:2016 — Surveillance, mesure, analyse et évaluation',
 'NORME',
 '2016',
 'https://www.iso.org/standard/27004',
 'Lignes directrices pour l''élaboration et l''utilisation de métriques et de '
 'mesures de performance de la sécurité de l''information.'),

(gen_random_uuid(),
 'ISO/IEC 27005:2022 — Gestion des risques de sécurité de l''information',
 'NORME',
 '2022',
 'https://www.iso.org/standard/27005',
 'Lignes directrices pour la gestion des risques de sécurité de l''information, '
 'compatible avec ISO 27001. Couvre l''identification, l''analyse, l''évaluation '
 'et le traitement des risques.'),

-- ---- Méthodologies de gestion des risques -----------------
(gen_random_uuid(),
 'EBIOS Risk Manager (EBIOS RM)',
 'METHODOLOGIE',
 '2018',
 'https://www.ssi.gouv.fr/ebios-risk-manager',
 'Méthode d''évaluation et de traitement des risques numériques développée par '
 'l''ANSSI. Version Risk Manager 2018. Approche par scénarios de risque et '
 'parties prenantes. Recommandée par l''ANCS pour les organismes d''importance vitale.'),

(gen_random_uuid(),
 'MEHARI — Méthode Harmonisée d''Analyse de Risques',
 'METHODOLOGIE',
 '2010',
 'https://clusif.fr/mehari',
 'Méthode de gestion des risques SI développée par le CLUSIF. '
 'Approche qualitative et quantitative, adaptée aux organismes de toutes tailles.'),

(gen_random_uuid(),
 'COBIT 2019 — Control Objectives for Information Technologies',
 'METHODOLOGIE',
 '2019',
 'https://www.isaca.org/cobit',
 'Cadre de gouvernance et de gestion des systèmes d''information de l''ISACA. '
 'Fournit un ensemble complet de contrôles et de pratiques pour aligner '
 'la DSI sur les objectifs métier.'),

(gen_random_uuid(),
 'ITIL 4 — Information Technology Infrastructure Library',
 'METHODOLOGIE',
 '4',
 'https://www.axelos.com/certifications/itil-service-management',
 'Référentiel de bonnes pratiques pour la gestion des services informatiques. '
 'ITIL 4 introduit le Système de valeur de service (SVS) et les pratiques '
 'de gestion moderne des services IT.');
