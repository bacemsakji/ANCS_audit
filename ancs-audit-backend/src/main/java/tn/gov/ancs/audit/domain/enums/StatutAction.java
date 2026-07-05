package tn.gov.ancs.audit.domain.enums;

/**
 * Cycle de vie d'une action corrective.
 */
public enum StatutAction {

    /** Action créée, non encore prise en charge. */
    A_FAIRE,

    /** Action en cours de traitement. */
    EN_COURS,

    /** Action clôturée — conformité rétablie. */
    CLOTUREE,

    /**
     * Échéance dépassée et action non clôturée.
     * Ce statut est calculé automatiquement par le scheduler.
     */
    EN_RETARD
}
