package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Journal d'audit des accès applicatifs.
 *
 * <p>Toute action sensible (consultation de rapport, génération de rapport,
 * modification de statut d'action, génération IA…) est tracée ici via
 * {@code AuditLogAspect} (AOP).</p>
 *
 * <p>Cette table ne s'étend pas de {@link BaseEntity} pour éviter la surcharge
 * de {@code updatedAt} (les logs d'audit sont immuables).</p>
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_utilisateur", columnList = "utilisateur_id, created_at"),
        @Index(name = "idx_audit_log_resource", columnList = "resource, resource_id"),
        @Index(name = "idx_audit_log_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Utilisateur à l'origine de l'action. Null pour les actions système. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    /** Action réalisée (ex. : READ_RAPPORT, GENERATE_RAPPORT, GENERATE_SYNTHESE_IA). */
    @Column(name = "action", length = 100)
    private String action;

    /** Type de ressource concernée (ex. : RAPPORT, MISSION, CONSTAT). */
    @Column(name = "resource", length = 100)
    private String resource;

    /** Identifiant de la ressource concernée. */
    @Column(name = "resource_id")
    private UUID resourceId;

    /** Adresse IP de l'appelant (IPv4 ou IPv6). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent HTTP (optionnel, pour les audits forensiques). */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
