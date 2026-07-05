package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête de validation de la double authentification (2FA).
 *
 * <p>SÉCURITÉ : utilise le jeton intermédiaire `mfaToken` à la place de l'email
 * en clair. Le serveur vérifie la signature de ce jeton pour authentifier
 * l'auditeur/admin avant de valider le code TOTP.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpVerifyRequest {

    @NotBlank(message = "Le jeton intermédiaire MFA est obligatoire")
    private String mfaToken;

    @NotBlank(message = "Le code TOTP est obligatoire")
    @Size(min = 6, max = 6, message = "Le code TOTP doit contenir exactement 6 chiffres")
    private String code;
}
