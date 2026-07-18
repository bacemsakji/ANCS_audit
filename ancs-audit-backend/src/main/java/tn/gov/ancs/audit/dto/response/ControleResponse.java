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
public class ControleResponse {

    private UUID id;
    private UUID referentielId;
    private String libelle;
    private String description;
    private String criticite;
    private String categorie;
    /** Référence ISO/IEC 27002:2022, ex. "5.1", "8.24". Null pour contrôles hors norme. */
    private String sousCritere;
    private Integer ordreAffichage;
}
