package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;

import java.time.LocalDate;
import java.util.List;

/**
 * Profil d'un auditeur certifié ANCS.
 *
 * <p>Lié à un {@link Utilisateur} de rôle {@code AUDITEUR}.
 * La certification est identifiée par un numéro officiel délivré par l'ANCS.</p>
 */
@Entity
@Table(
    name = "auditeur",
    indexes = {
        @Index(name = "idx_auditeur_numero_certification", columnList = "numero_certification", unique = true),
        @Index(name = "idx_auditeur_date_expiration", columnList = "date_expiration")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auditeur extends BaseEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @NotBlank
    @Size(max = 100)
    @Column(name = "numero_certification", nullable = false, unique = true)
    private String numeroCertification;

    @NotNull
    @Column(name = "date_certification", nullable = false)
    private LocalDate dateCertification;

    @NotNull
    @Column(name = "date_expiration", nullable = false)
    private LocalDate dateExpiration;

    /**
     * Liste des domaines de spécialité (ex. : "ISO 27001", "EBIOS RM", "Cloud Security").
     * Stocké en JSONB dans PostgreSQL via Hibernate.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specialites", columnDefinition = "jsonb")
    private List<String> specialites;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAuditeur statut = StatutAuditeur.ACTIF;
}
