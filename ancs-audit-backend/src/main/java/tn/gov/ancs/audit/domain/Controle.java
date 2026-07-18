package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Contrôle technique du référentiel ANCS.
 *
 * <p>Un contrôle est un critère précis que l'auditeur doit vérifier lors d'une mission.
 * La checklist d'audit est générée dynamiquement à partir des contrôles du référentiel
 * sélectionné pour la mission.</p>
 */
@Entity
@Table(
    name = "controle",
    indexes = {
        @Index(name = "idx_controle_referentiel", columnList = "referentiel_id"),
        @Index(name = "idx_controle_categorie",   columnList = "categorie"),
        @Index(name = "idx_controle_criticite",   columnList = "criticite"),
        @Index(name = "idx_controle_sous_critere", columnList = "sous_critere")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Controle extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referentiel_id", nullable = false)
    private Referentiel referentiel;

    @NotBlank
    @Size(max = 500)
    @Column(name = "libelle", nullable = false, length = 500)
    private String libelle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Niveau de criticité du contrôle.
     * Valeurs : {@code FAIBLE}, {@code MOYEN}, {@code ELEVE}, {@code CRITIQUE}.
     */
    @Size(max = 20)
    @Column(name = "criticite", length = 20)
    private String criticite;

    /** Catégorie ou domaine thématique (ex. : "Gouvernance", "Réseau", "IAM", "SMSI"). */
    @Size(max = 100)
    @Column(name = "categorie", length = 100)
    private String categorie;

    /**
     * Référence ISO/IEC 27002:2022 du contrôle (ex. : "5.1", "8.24").
     * Null pour les contrôles hors ISO 27002.
     */
    @Size(max = 10)
    @Column(name = "sous_critere", length = 10)
    private String sousCritere;

    /** Ordre d'affichage dans la checklist d'audit. */
    @Column(name = "ordre_affichage")
    private Integer ordreAffichage;
}
