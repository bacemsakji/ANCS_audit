package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstatResponse {

    private UUID id;
    private UUID missionId;
    private UUID controleId;
    private String controleLibelle;
    private String controleCategorie;
    private String resultat;
    private String preuveUrl;
    private String commentaire;
    private Instant dateConstat;
    private boolean synced;
    private String criticite;
    private String preuveDescription;
    private String recommandation;
    private String composantesImpactees;
}
