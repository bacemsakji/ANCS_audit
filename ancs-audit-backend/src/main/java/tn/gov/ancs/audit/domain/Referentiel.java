package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.TypeReferentiel;

/**
 * Entrée du référentiel réglementaire ANCS.
 *
 * <p>Regroupe les textes juridiques (Décret-loi 2023-17, Arrêté 2019),
 * les normes (ISO/IEC 27001, 27004, 27005),
 * les méthodologies (EBIOS RM, MEHARI, OCTAVE, COBIT, ITIL)
 * et les référentiels de contrôles techniques ANCS.</p>
 */
@Entity
@Table(name = "referentiel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Referentiel extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "nom", nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private TypeReferentiel type;

    @Size(max = 50)
    @Column(name = "version")
    private String version;

    /** URL ou référence bibliographique de la source officielle. */
    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
