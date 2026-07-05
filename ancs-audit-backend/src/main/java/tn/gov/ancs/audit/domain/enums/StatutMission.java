package tn.gov.ancs.audit.domain.enums;

/**
 * Cycle de vie d'une mission d'audit.
 */
public enum StatutMission {

    /** Mission planifiée, non encore démarrée. */
    PLANIFIEE,

    /** Mission en cours — auditeur sur le terrain. */
    EN_COURS,

    /** Mission terminée, rapport généré. */
    TERMINEE,

    /** Mission annulée avant son terme. */
    ANNULEE
}
