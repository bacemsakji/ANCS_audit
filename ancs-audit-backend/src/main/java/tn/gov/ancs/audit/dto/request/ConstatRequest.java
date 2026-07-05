package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstatRequest {

    @NotNull(message = "L'identifiant de la mission est obligatoire")
    private UUID missionId;

    @NotNull(message = "L'identifiant du contrôle est obligatoire")
    private UUID controleId;

    @NotNull(message = "Le résultat du constat est obligatoire")
    private ResultatConstat resultat;

    private String commentaire;
    
    /** Optionnel lors de la première saisie, mis à jour par l'upload de fichiers. */
    private String preuveUrl;
}
