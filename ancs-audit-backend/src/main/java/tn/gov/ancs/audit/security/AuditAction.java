package tn.gov.ancs.audit.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes dont l'exécution doit être tracée
 * dans les logs d'audit (table audit_log).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /** Action réalisée (ex. : LOGIN, READ_RAPPORT, GENERATE_SYNTHESE_IA). */
    String action();

    /** Type de ressource concernée (ex. : RAPPORT, MISSION, UTILISATEUR). */
    String resource() default "";

    /**
     * Indique si l'identifiant de la ressource doit être extrait des arguments de la méthode.
     * Si vrai, l'aspect cherchera un argument de type UUID ou String nommé id/missionId/etc.
     */
    boolean extractResourceId() default false;
}
