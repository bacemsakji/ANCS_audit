package tn.gov.ancs.audit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryRequest {

    private String organismeNom;
    private String perimetre;
    private String dateDebut;
    private String dateFin;
    private String referentielNom;
    private String referentielVersion;
    private double tauxConformite;
    
    /** Langue de rédaction demandée : "FR" ou "AR". */
    private String langue;
    
    private List<ConstatInfo> constats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstatInfo {
        private String controleLibelle;
        private String resultat;
        private String criticite;
        private String commentaire;
    }
}
