package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRssiResponse {

    private String organismeNom;
    
    /** Score de conformité du dernier rapport d'audit validé. */
    private double scoreDernierAudit;
    
    private long totalMissionsRealisees;
    private long totalActionsAFaire;
    private long totalActionsEnCours;
    private long totalActionsCloturees;
    private long totalActionsEnRetard;

    private List<ActionResponse> actionsPrioritaires;
}
