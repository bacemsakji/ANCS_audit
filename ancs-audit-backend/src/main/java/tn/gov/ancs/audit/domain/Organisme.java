package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Organisme soumis à obligation d'audit de sécurité SI (Décret-loi 2023-17).
 * Peut être une entreprise publique, un établissement public ou une entreprise privée
 * relevant du champ d'application du texte réglementaire.
 */
@Entity
@Table(name = "organisme")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organisme extends BaseEntity {

    /** Raison sociale / Dénomination officielle de l'organisme. */
    @NotBlank
    @Size(max = 255)
    @Column(name = "nom", nullable = false)
    private String nom;

    /** Secteur d'activité (ex. : Banques, Télécommunications, Énergie, Santé…). */
    @Size(max = 100)
    @Column(name = "secteur_activite")
    private String secteurActivite;

    /**
     * Type d'obligation d'audit.
     * Valeurs attendues : {@code SOUMIS_AUDIT} (obligation réglementaire) ou
     * {@code VOLONTAIRE}.
     */
    @Size(max = 50)
    @Column(name = "type_obligation_audit")
    private String typeObligationAudit;

    /** Adresse physique du siège social. */
    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    /** Adresse email du RSSI ou du contact désigné pour les audits. */
    @Email
    @Size(max = 255)
    @Column(name = "contact_rssi_email")
    private String contactRssiEmail;

    /** Acronyme de l'organisme */
    @Size(max = 50)
    @Column(name = "acronyme")
    private String acronyme;

    /** Statut juridique/opérationnel (ex: Public, Privé) */
    @Size(max = 20)
    @Column(name = "statut")
    private String statut;

    /** Catégorie d'organisme (ex: OIV, administration, etc.) */
    @Size(max = 100)
    @Column(name = "categorie")
    private String categorie;
}
