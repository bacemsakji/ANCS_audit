package tn.gov.ancs.audit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.gov.ancs.audit.domain.enums.Role;

/**
 * Utilisateur de la plateforme ANCS Audit.
 *
 * <p>Trois rôles possibles : {@link Role#ADMIN_ANCS}, {@link Role#AUDITEUR}, {@link Role#RSSI}.</p>
 *
 * <ul>
 *   <li>Pour le rôle {@link Role#RSSI}, le champ {@code organisme} est obligatoire.</li>
 *   <li>Pour le rôle {@link Role#ADMIN_ANCS}, le champ {@code totpSecret} doit être défini
 *       après l'enrôlement 2FA.</li>
 * </ul>
 */
@Entity
@Table(
    name = "utilisateur",
    indexes = {
        @Index(name = "idx_utilisateur_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "nom", nullable = false)
    private String nom;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Mot de passe haché avec BCrypt. Ne jamais stocker en clair. */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Organisme associé — obligatoire pour le rôle {@link Role#RSSI},
     * null pour {@link Role#ADMIN_ANCS} et {@link Role#AUDITEUR}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisme_id")
    private Organisme organisme;

    /**
     * Secret TOTP encodé en Base32, utilisé pour la 2FA.
     * Obligatoire pour le rôle {@link Role#ADMIN_ANCS}.
     */
    @Column(name = "totp_secret")
    private String totpSecret;

    /** Indique si l'enrôlement 2FA est complété pour l'admin. */
    @Builder.Default
    @Column(name = "totp_enabled", nullable = false)
    private Boolean totpEnabled = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Token FCM pour les notifications push. Mis à jour lors de chaque connexion. */
    @Size(max = 500)
    @Column(name = "fcm_token")
    private String fcmToken;
}
