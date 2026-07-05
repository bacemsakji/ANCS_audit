package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

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
