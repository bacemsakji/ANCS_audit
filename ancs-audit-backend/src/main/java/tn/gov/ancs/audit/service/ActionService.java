package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Action;
import tn.gov.ancs.audit.domain.Constat;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutAction;
import tn.gov.ancs.audit.dto.request.ActionRequest;
import tn.gov.ancs.audit.dto.response.ActionResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.ActionRepository;
import tn.gov.ancs.audit.repository.ConstatRepository;
import tn.gov.ancs.audit.repository.MissionRepository;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.security.AuditAction;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository actionRepository;
    private final ConstatRepository constatRepository;
    private final MissionRepository missionRepository;

    @Transactional
    @AuditAction(action = "CREATE_ACTION", resource = "ACTION")
    public ActionResponse createAction(ActionRequest request, String auditorEmail) {
        Constat constat = constatRepository.findById(request.getConstatId())
            .orElseThrow(() -> new ResourceNotFoundException("Constat non trouvé avec l'id: " + request.getConstatId()));

        // Sécurité : seul l'auditeur assigné peut associer des actions correctives
        if (!constat.getMission().getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(auditorEmail)) {
            throw new AccessDeniedException("Accès refusé");
        }

        if (constat.getResultat() == ResultatConstat.CONFORME) {
            throw new IllegalArgumentException("Impossible de créer une action corrective pour un constat CONFORME");
        }

        Action action = Action.builder()
            .constat(constat)
            .description(request.getDescription())
            .responsable(request.getResponsable())
            .echeance(request.getEcheance())
            .priorite(request.getPriorite())
            .statut(StatutAction.A_FAIRE)
            .build();

        Action saved = actionRepository.save(action);
        log.info("Action corrective créée pour le constat {} (Priorité: {})", constat.getId(), saved.getPriorite());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ActionResponse> getActionsForMission(UUID missionId, String userEmail, Role userRole, UUID userOrganismeId, Pageable pageable) {
        // SÉCURITÉ : validation stricte des droits d'accès (IDOR)
        if (userRole != Role.ADMIN_ANCS) {
            Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));

            if (userRole == Role.RSSI) {
                // Un RSSI ne peut voir que les actions de son propre organisme
                if (!mission.getOrganisme().getId().equals(userOrganismeId)) {
                    throw new AccessDeniedException("Accès refusé : cette mission concerne un autre organisme");
                }
            } else if (userRole == Role.AUDITEUR) {
                // Un auditeur ne peut voir que les actions d'une mission qui lui est assignée
                if (!mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(userEmail)) {
                    throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
                }
            }
        }

        return actionRepository.findByMissionId(missionId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ActionResponse> getActiveActionsForRssi(UUID organismeId) {
        return actionRepository.findActifsByOrganismeId(organismeId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(action = "UPDATE_ACTION_STATUS", resource = "ACTION", extractResourceId = true)
    public ActionResponse updateStatus(UUID id, StatutAction statut, String userEmail, Role userRole, UUID userOrganismeId) {
        Action action = actionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Action corrective non trouvée avec l'id: " + id));

        // Validation sécurité :
        // - Admin : oui
        // - Auditeur : oui, si assigné
        // - RSSI : oui, si l'action appartient à son organisme
        if (userRole == Role.RSSI) {
            UUID actionOrgId = action.getConstat().getMission().getOrganisme().getId();
            if (!actionOrgId.equals(userOrganismeId)) {
                throw new AccessDeniedException("Accès refusé : cette action corrective concerne un autre organisme");
            }
        } else if (userRole == Role.AUDITEUR) {
            String assignedAuditor = action.getConstat().getMission().getAuditeur().getUtilisateur().getEmail();
            if (!assignedAuditor.equalsIgnoreCase(userEmail)) {
                throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné");
            }
        }

        action.setStatut(statut);
        Action updated = actionRepository.save(action);
        log.info("Statut de l'action corrective {} mis à jour : {}", updated.getId(), statut);
        return mapToResponse(updated);
    }

    public ActionResponse mapToResponse(Action a) {
        return ActionResponse.builder()
            .id(a.getId())
            .constatId(a.getConstat().getId())
            .constatControleLibelle(a.getConstat().getControle().getLibelle())
            .description(a.getDescription())
            .responsable(a.getResponsable())
            .echeance(a.getEcheance())
            .priorite(a.getPriorite() != null ? a.getPriorite().name() : null)
            .statut(a.getStatut().name())
            .build();
    }
}
