package tn.gov.ancs.audit.domain.enums;

/**
 * Rôles applicatifs (RBAC).
 *
 * <ul>
 *   <li>{@link #ADMIN_ANCS} — Administrateur de l'agence : accès complet, gestion du référentiel
 *       et des certifications.</li>
 *   <li>{@link #AUDITEUR} — Auditeur certifié ANCS : réalise les missions d'audit qui lui sont
 *       assignées.</li>
 *   <li>{@link #RSSI} — Responsable Sécurité SI de l'organisme audité : accès limité à ses propres
 *       données (rapports, actions correctives).</li>
 * </ul>
 */
public enum Role {

    /** Administrateur ANCS — 2FA obligatoire, accès complet. */
    ADMIN_ANCS,

    /** Auditeur certifié ANCS — réalise les missions. */
    AUDITEUR,

    /** RSSI / Organisme audité — consultation uniquement, périmètre restreint. */
    RSSI
}
