package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;

import java.time.Instant;

/**
 * Constat d'audit pour un contrôle donné dans le cadre d'une mission.
 *
 * <p>Un constat est saisi par l'auditeur pour chaque contrôle de la checklist.
 * Il peut être accompagné d'une preuve (photo, capture d'écran) stockée dans MinIO.</p>
 *
 * <p><strong>Mode hors-ligne :</strong> Le champ {@code synced} indique si le constat
 * a été synchronisé avec le serveur. L'application Flutter le met à {@code true}
 * après une synchronisation réussie.</p>
 */
@Entity
@Table(
    name = "constat",
    indexes = {
        @Index(name = "idx_constat_mission", columnList = "mission_id"),
        @Index(name = "idx_constat_controle", columnList = "controle_id"),
        @Index(name = "idx_constat_resultat", columnList = "resultat")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Constat extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controle_id", nullable = false)
    private Controle controle;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat", length = 20)
    private ResultatConstat resultat;

    /**
     * URL MinIO de la preuve (image, PDF, capture…).
     * Null si aucune preuve n'a été jointe.
     */
    @Column(name = "preuve_url", columnDefinition = "TEXT")
    private String preuveUrl;

    /** Commentaire libre de l'auditeur sur ce contrôle. */
    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_constat")
    private Instant dateConstat;

    @Column(name = "criticite", length = 20)
    private String criticite;

    @Column(name = "preuve_description", columnDefinition = "TEXT")
    private String preuveDescription;

    @Column(name = "recommandation", columnDefinition = "TEXT")
    private String recommandation;

    @Column(name = "composantes_impactees", columnDefinition = "TEXT")
    private String composantesImpactees;

    /**
     * Indicateur de synchronisation.
     * {@code false} : enregistrement local non encore transmis au serveur.
     * {@code true} : synchronisé avec succès.
     */
    @Builder.Default
    @Column(name = "synced", nullable = false)
    private Boolean synced = false;
}
