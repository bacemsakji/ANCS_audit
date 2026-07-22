package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.StatutSoumissionAncs;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Rapport d'audit officiel généré au format PDF et/ou DOCX.
 *
 * <p>Le fichier est stocké dans MinIO (bucket {@code ancs-rapports}).
 * L'accès est contrôlé par des liens signés à durée limitée (24h par défaut).</p>
 *
 * <p>Les champs {@code syntheseGenereeParIa} et {@code syntheseIaHorodatage}
 * tracent l'utilisation du module de génération IA, indépendamment du contenu
 * final du rapport (qui peut avoir été édité manuellement après génération).</p>
 */
@Entity
@Table(
    name = "rapport",
    indexes = {
        @Index(name = "idx_rapport_mission", columnList = "mission_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rapport_mission_version", columnNames = {"mission_id", "version"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rapport extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    /** Chemin/clé objet dans le bucket MinIO. */
    @Column(name = "fichier_url", columnDefinition = "TEXT")
    private String fichierUrl;

    @Column(name = "date_generation")
    private Instant dateGeneration;

    /** Numéro de version incrémenté à chaque régénération du rapport. */
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /** Format du fichier généré : {@code PDF} ou {@code DOCX}. */
    @Column(name = "type", length = 10)
    private String type;

    @Column(name = "nom_auditeur", length = 255)
    private String nomAuditeur;

    @Column(name = "numero_certification_ancs", length = 100)
    private String numeroCertificationAncs;

    @Column(name = "contact_auditeur", length = 255)
    private String contactAuditeur;

    @Column(name = "texte_confidentialite", columnDefinition = "TEXT")
    private String texteConfidentialite;

    @Column(name = "historique_versions", columnDefinition = "TEXT")
    private String historiqueVersions;

    // -------------------------------------------------------
    // Processus de soumission officielle à l'ANCS
    // (décret 2004-1250 + décret-loi 2023-17)
    // -------------------------------------------------------

    /**
     * Statut de la soumission du rapport à l'ANCS.
     * Cycle : NON_SOUMIS → SOUMIS → ACCEPTE | REJETE.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_soumission_ancs", length = 20, nullable = false)
    private StatutSoumissionAncs statutSoumissionAncs = StatutSoumissionAncs.NON_SOUMIS;

    /**
     * Date de transmission effective du rapport à l'ANCS.
     * Doit intervenir dans les 10 jours suivant la fin de la mission.
     */
    @Column(name = "date_soumission_ancs")
    private Instant dateSoumissionAncs;

    /**
     * Motif de rejet communiqué par l'ANCS.
     * Renseigné uniquement si {@code statutSoumissionAncs == REJETE}.
     */
    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    /**
     * Date limite de resoumission après rejet par l'ANCS.
     * L'organisme dispose de 2 mois pour refaire l'audit.
     * Calculée automatiquement : {@code dateSoumissionAncs + 2 mois}.
     */
    @Column(name = "date_limite_resoumission")
    private LocalDate dateLimiteResoumission;

    // -------------------------------------------------------
    // Traçabilité de la génération IA (module synthèse)
    // -------------------------------------------------------

    /**
     * Indique si un brouillon de synthèse a été généré par IA pour ce rapport.
     * Reste {@code true} même si l'auditeur a édité le texte a posteriori.
     */
    @Builder.Default
    @Column(name = "synthese_generee_par_ia", nullable = false)
    private Boolean syntheseGenereeParIa = false;

    /**
     * Horodatage de la dernière génération IA de la synthèse.
     * Null si aucune génération IA n'a eu lieu.
     */
    @Column(name = "synthese_ia_horodatage")
    private Instant syntheseIaHorodatage;
}
