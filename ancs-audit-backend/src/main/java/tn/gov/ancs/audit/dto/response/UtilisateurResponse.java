package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {

    private UUID id;
    private String nom;
    private String email;
    private String role;
    private UUID organismeId;
    private String organismeNom;
    private boolean totpEnabled;
    private boolean isActive;
}
