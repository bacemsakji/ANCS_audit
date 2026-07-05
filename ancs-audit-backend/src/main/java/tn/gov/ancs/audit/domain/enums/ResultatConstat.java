package tn.gov.ancs.audit.domain.enums;

/**
 * Résultat d'un constat d'audit pour un contrôle donné.
 */
public enum ResultatConstat {

    /** Le contrôle est satisfait — aucune action requise. */
    CONFORME,

    /** Le contrôle n'est pas satisfait — action corrective obligatoire. */
    NON_CONFORME,

    /** Point d'attention signalé — ne constitue pas une non-conformité formelle. */
    OBSERVATION
}
