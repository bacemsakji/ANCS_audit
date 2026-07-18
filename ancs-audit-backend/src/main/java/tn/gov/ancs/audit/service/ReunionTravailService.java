package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.domain.ReunionTravail;
import tn.gov.ancs.audit.dto.request.ReunionTravailRequest;
import tn.gov.ancs.audit.dto.response.ReunionTravailResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.MissionRepository;
import tn.gov.ancs.audit.repository.ReunionTravailRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReunionTravailService {

    private final ReunionTravailRepository reunionTravailRepository;
    private final MissionRepository missionRepository;

    @Transactional
    public ReunionTravailResponse createReunion(ReunionTravailRequest request, String userEmail, String userRole) {
        Mission mission = missionRepository.findById(request.getMissionId())
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée avec l'id: " + request.getMissionId()));

        boolean isAssignedAuditor = mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(userEmail);
        boolean isAdmin = "ADMIN_ANCS".equalsIgnoreCase(userRole);

        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
        }

        ReunionTravail rt = ReunionTravail.builder()
            .mission(mission)
            .dateReunion(request.getDateReunion())
            .participants(request.getParticipants())
            .compteRendu(request.getCompteRendu())
            .build();

        ReunionTravail saved = reunionTravailRepository.save(rt);
        log.info("Réunion de travail créée pour la mission {}", mission.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReunionTravailResponse> getReunionsByMission(UUID missionId, String userEmail, String userRole, UUID userOrganismeId) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée avec l'id: " + missionId));

        // Contrôle d'accès
        boolean isAdmin = "ADMIN_ANCS".equalsIgnoreCase(userRole);
        boolean isAuditeur = "AUDITEUR".equalsIgnoreCase(userRole);
        boolean isRssi = "RSSI".equalsIgnoreCase(userRole);

        if (isRssi) {
            UUID targetOrgId = mission.getOrganisme().getId();
            if (!targetOrgId.equals(userOrganismeId)) {
                throw new AccessDeniedException("Accès refusé : vous ne pouvez pas accéder aux réunions d'un autre organisme");
            }
        } else if (isAuditeur) {
            String assignedEmail = mission.getAuditeur().getUtilisateur().getEmail();
            if (!assignedEmail.equalsIgnoreCase(userEmail)) {
                throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
            }
        } else if (!isAdmin) {
            throw new AccessDeniedException("Accès refusé");
        }

        List<ReunionTravail> list = reunionTravailRepository.findByMissionIdOrderByDateReunionAsc(missionId);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteReunion(UUID id, String userEmail, String userRole) {
        ReunionTravail rt = reunionTravailRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Réunion de travail non trouvée avec l'id: " + id));

        boolean isAssignedAuditor = rt.getMission().getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(userEmail);
        boolean isAdmin = "ADMIN_ANCS".equalsIgnoreCase(userRole);

        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
        }

        reunionTravailRepository.delete(rt);
        log.info("Réunion de travail supprimée : {}", id);
    }

    private ReunionTravailResponse mapToResponse(ReunionTravail rt) {
        return ReunionTravailResponse.builder()
            .id(rt.getId())
            .missionId(rt.getMission().getId())
            .dateReunion(rt.getDateReunion())
            .participants(rt.getParticipants())
            .compteRendu(rt.getCompteRendu())
            .build();
    }
}
