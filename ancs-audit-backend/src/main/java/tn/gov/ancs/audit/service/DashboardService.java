package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.domain.enums.StatutAction;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;
import tn.gov.ancs.audit.domain.enums.StatutMission;
import tn.gov.ancs.audit.dto.response.ActionResponse;
import tn.gov.ancs.audit.dto.response.DashboardAdminResponse;
import tn.gov.ancs.audit.dto.response.DashboardRssiResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MissionRepository missionRepository;
    private final AuditeurRepository auditeurRepository;
    private final OrganismeRepository organismeRepository;
    private final ConstatRepository constatRepository;
    private final ActionRepository actionRepository;
    private final ActionService actionService;

    @Transactional(readOnly = true)
    public DashboardAdminResponse getAdminDashboard() {
        long enCours = missionRepository.countByStatut(StatutMission.EN_COURS);
        long planifiees = missionRepository.countByStatut(StatutMission.PLANIFIEE);
        long terminees = missionRepository.countByStatut(StatutMission.TERMINEE);
        
        long auditeursActifs = auditeurRepository.count(); // simplifiée
        long totalOrganismes = organismeRepository.count();

        // Alertes expirations certifications (dans les 30 prochains jours)
        LocalDate limite = LocalDate.now().plusDays(30);
        long alertes = auditeurRepository.findActifsExpirantAvant(limite).size();

        // Calculer le taux de conformité global (moyenne sur tous les constats de la base)
        long totalConstats = constatRepository.count();
        long conformes = constatRepository.countByResultat(ResultatConstat.CONFORME);
        double tauxGlobal = totalConstats > 0 ? (conformes * 100.0 / totalConstats) : 0.0;

        // Missions récentes
        List<DashboardAdminResponse.RecentMissionDto> recents = missionRepository.findAll(
            PageRequest.of(0, 5, Sort.by("createdAt").descending())
        ).stream().map(m -> DashboardAdminResponse.RecentMissionDto.builder()
            .id(m.getId().toString())
            .organismeNom(m.getOrganisme().getNom())
            .auditeurNom(m.getAuditeur().getUtilisateur().getNom())
            .statut(m.getStatut().name())
            .dateDebut(m.getDateDebut() != null ? m.getDateDebut().toString() : null)
            .build()
        ).collect(Collectors.toList());

        return DashboardAdminResponse.builder()
            .totalMissionsEnCours(enCours)
            .totalMissionsPlanifiees(planifiees)
            .totalMissionsTerminees(terminees)
            .totalAuditeursActifs(auditeursActifs)
            .totalOrganismes(totalOrganismes)
            .alertesCertificationExpiration(alertes)
            .tauxConformiteGlobal(tauxGlobal)
            .missionsRecentes(recents)
            .build();
    }

    @Transactional(readOnly = true)
    public DashboardRssiResponse getRssiDashboard(UUID organismeId) {
        Organisme org = organismeRepository.findById(organismeId)
            .orElseThrow(() -> new ResourceNotFoundException("Organisme non trouvé"));

        long totalRealisees = missionRepository.countTermineesParOrganisme(organismeId);

        // Trouver la dernière mission terminée pour obtenir son taux de conformité réel
        List<Mission> missions = missionRepository.findByOrganismeIdAndStatutOrderByCreatedAtDesc(organismeId, StatutMission.TERMINEE);
        double scoreDernierAudit = 0.0;
        if (!missions.isEmpty()) {
            Double score = constatRepository.calculateTauxConformite(missions.get(0).getId());
            scoreDernierAudit = score != null ? score : 0.0;
        }

        // Récupérer et mapper les actions correctives actives pour le dashboard
        List<ActionResponse> actionsActives = actionRepository.findActifsByOrganismeId(organismeId).stream()
            .map(actionService::mapToResponse)
            .collect(Collectors.toList());

        long aFaire = actionsActives.stream().filter(a -> "A_FAIRE".equals(a.getStatut())).count();
        long enCours = actionsActives.stream().filter(a -> "EN_COURS".equals(a.getStatut())).count();
        long enRetard = actionsActives.stream().filter(a -> "EN_RETARD".equals(a.getStatut())).count();
        long cloturees = actionRepository.countClotureesParOrganisme(organismeId);

        return DashboardRssiResponse.builder()
            .organismeNom(org.getNom())
            .scoreDernierAudit(scoreDernierAudit)
            .totalMissionsRealisees(totalRealisees)
            .totalActionsAFaire(aFaire)
            .totalActionsEnCours(enCours)
            .totalActionsCloturees(cloturees)
            .totalActionsEnRetard(enRetard)
            .actionsPrioritaires(actionsActives.stream().limit(5).collect(Collectors.toList()))
            .build();
    }
}
