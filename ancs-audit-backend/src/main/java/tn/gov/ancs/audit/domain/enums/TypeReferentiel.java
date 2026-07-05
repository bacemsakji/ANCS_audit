package tn.gov.ancs.audit.domain.enums;

/**
 * Classification des entrées du référentiel réglementaire ANCS.
 */
public enum TypeReferentiel {

    /** Texte législatif ou réglementaire (décret-loi, arrêté, circulaire). */
    LOI,

    /** Norme internationale (ISO/IEC 27001, 27004, 27005…). */
    NORME,

    /** Méthodologie de gestion des risques (EBIOS, MEHARI, OCTAVE, COBIT, ITIL…). */
    METHODOLOGIE,

    /** Référentiel technique de contrôles ANCS à vérifier lors d'un audit. */
    CONTROLE_TECHNIQUE
}
