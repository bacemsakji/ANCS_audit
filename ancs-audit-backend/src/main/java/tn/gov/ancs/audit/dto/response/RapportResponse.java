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
public class RapportResponse {

    private UUID id;
    private UUID missionId;
    private String organismeNom;
    private String auditeurNom;
    private String type;           // PDF or DOCX
    private int version;
    private Instant dateGeneration;
    private boolean syntheseGenereeParIa;
    private String statutSoumissionAncs; // NON_SOUMIS / EN_ATTENTE_VALIDATION / VALIDE / REJETE
    private String motifRejet;
    private String numeroCertificationAncs;
    private String contactAuditeur;
}
