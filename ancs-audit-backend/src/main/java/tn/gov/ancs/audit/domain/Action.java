package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.PrioriteAction;
import tn.gov.ancs.audit.domain.enums.StatutAction;

import java.time.LocalDate;

/**
 * Action corrective générée automatiquement à partir d'un {@link Constat} non conforme.
 *
 * <p>Le suivi de statut et les notifications FCM avant échéance sont gérés
 * par le {@code NotificationScheduler} (Phase 4).</p>
 */
@Entity
@Table(
    name = "action",
    indexes = {
        @Index(name = "idx_action_constat", columnList = "constat_id"),
        @Index(name = "idx_action_statut", columnList = "statut"),
        @Index(name = "idx_action_echeance_statut", columnList = "echeance, statut")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constat_id", nullable = false)
    private Constat constat;

    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Nom ou email de la personne responsable du traitement de l'action. */
    @Column(name = "responsable", length = 255)
    private String responsable;

    /** Date limite de clôture de l'action. */
    @Column(name = "echeance")
    private LocalDate echeance;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", length = 20)
    private PrioriteAction priorite;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAction statut = StatutAction.A_FAIRE;
}
