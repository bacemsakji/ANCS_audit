package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {

    private UUID id;
    private UUID organismeId;
    private String organismeNom;
    private UUID auditeurId;
    private String auditeurNom;
    private UUID referentielId;
    private String referentielNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private String perimetre;
}
