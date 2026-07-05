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
public class DashboardAdminResponse {

    private long totalMissionsEnCours;
    private long totalMissionsPlanifiees;
    private long totalMissionsTerminees;
    private long totalAuditeursActifs;
    private long totalOrganismes;
    
    /** Nombre d'auditeurs ayant une certification expirant dans les 30 jours. */
    private long alertesCertificationExpiration;

    /** Taux global de conformité calculé sur l'ensemble des constats de toutes les missions. */
    private double tauxConformiteGlobal;

    private List<RecentMissionDto> missionsRecentes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentMissionDto {
        private String id;
        private String organismeNom;
        private String auditeurNom;
        private String statut;
        private String dateDebut;
    }
}
