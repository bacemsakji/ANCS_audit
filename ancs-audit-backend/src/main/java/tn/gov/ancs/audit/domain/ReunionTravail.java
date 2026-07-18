package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Réunion de travail organisée dans le cadre d'une mission d'audit.
 *
 * <p>L'article du décret 2004-1250 impose la transmission du rapport d'audit
 * <strong>accompagné des PV des réunions de travail</strong> à l'ANCS dans
 * un délai de 10 jours après la fin de la mission.
 *
 * <p>Chaque réunion est liée à une {@link Mission} et conserve :
 * <ul>
 *   <li>la date de la réunion,</li>
 *   <li>la liste des participants,</li>
 *   <li>le compte-rendu (PV) de la réunion.</li>
 * </ul>
 */
@Entity
@Table(
    name = "reunion_travail",
    indexes = {
        @Index(name = "idx_reunion_mission", columnList = "mission_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReunionTravail extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    /**
     * Date et heure de la réunion de travail.
     */
    @NotNull
    @Column(name = "date_reunion", nullable = false)
    private Instant dateReunion;

    /**
     * Liste des participants à la réunion (nom, fonction, organisme).
     * Stocké en texte libre pour supporter différents formats.
     */
    @Column(name = "participants", columnDefinition = "TEXT")
    private String participants;

    /**
     * Compte-rendu (PV) de la réunion de travail.
     * Ce document doit être annexé au rapport transmis à l'ANCS.
     */
    @Column(name = "compte_rendu", columnDefinition = "TEXT")
    private String compteRendu;
}
