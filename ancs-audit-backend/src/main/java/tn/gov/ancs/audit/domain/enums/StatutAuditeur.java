package tn.gov.ancs.audit.domain.enums;

/**
 * Statut de certification d'un auditeur ANCS.
 */
public enum StatutAuditeur {

    /** Certification valide et auditeur autorisé à réaliser des missions. */
    ACTIF,

    /** Certification expirée — l'auditeur ne peut plus réaliser de nouvelles missions. */
    EXPIRE,

    /** Certification révoquée par décision de l'ANCS. */
    REVOQUE
}
