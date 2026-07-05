package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.StatutMission;

import java.time.LocalDate;

/**
 * Mission d'audit de sécurité SI.
 *
 * <p>Une mission est créée par un {@link Role#ADMIN_ANCS}, assignée à un {@link Auditeur}
 * et porte sur un {@link Organisme} selon un {@link Referentiel} défini.</p>
 */
@Entity
@Table(
    name = "mission",
    indexes = {
        @Index(name = "idx_mission_organisme", columnList = "organisme_id"),
        @Index(name = "idx_mission_auditeur", columnList = "auditeur_id"),
        @Index(name = "idx_mission_statut", columnList = "statut")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisme_id", nullable = false)
    private Organisme organisme;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditeur_id", nullable = false)
    private Auditeur auditeur;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referentiel_id", nullable = false)
    private Referentiel referentiel;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "statut", nullable = false, length = 30)
    private StatutMission statut = StatutMission.PLANIFIEE;

    /** Description du périmètre audité (systèmes, applications, infrastructure). */
    @Column(name = "perimetre", columnDefinition = "TEXT")
    private String perimetre;
}
