package tn.gov.ancs.audit.domain.enums;

/**
 * Niveau de priorité d'une action corrective.
 *
 * <p>Utilisé pour trier et colorer les actions dans le tableau de bord du RSSI
 * et dans les notifications FCM.</p>
 */
public enum PrioriteAction {

    /** Priorité faible — à traiter dans les délais standard. */
    FAIBLE,

    /** Priorité moyenne — à traiter dans un délai raisonnable. */
    MOYENNE,

    /** Priorité haute — à traiter rapidement. */
    HAUTE,

    /** Priorité critique — traitement immédiat requis, escalade possible. */
    CRITIQUE
}
