package tn.gov.ancs.audit.domain.enums;

/**
 * Statut de la soumission officielle du rapport d'audit à l'ANCS.
 *
 * <p>Cycle de vie réglementaire (décret 2004-1250 et décret-loi 2023-17) :
 * <pre>
 *   NON_SOUMIS → SOUMIS → ACCEPTE
 *                       ↘ REJETE → (correction dans les 2 mois) → SOUMIS
 * </pre>
 */
public enum StatutSoumissionAncs {

    /**
     * Rapport généré mais pas encore transmis à l'ANCS.
     * État initial par défaut.
     */
    NON_SOUMIS,

    /**
     * Rapport transmis à l'ANCS (délai réglementaire : 10 jours après la mission).
     */
    SOUMIS,

    /**
     * Rapport accepté par l'ANCS.
     */
    ACCEPTE,

    /**
     * Rapport rejeté par l'ANCS.
     * L'organisme dispose de 2 mois pour refaire l'audit et soumettre un nouveau rapport.
     * Voir {@code dateLimiteResoumission} dans l'entité {@code Rapport}.
     */
    REJETE
}
