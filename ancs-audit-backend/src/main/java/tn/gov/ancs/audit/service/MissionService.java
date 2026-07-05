package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.domain.Referentiel;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutMission;
import tn.gov.ancs.audit.dto.request.CreateMissionRequest;
import tn.gov.ancs.audit.dto.response.MissionResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.AuditeurRepository;
import tn.gov.ancs.audit.repository.MissionRepository;
import tn.gov.ancs.audit.repository.OrganismeRepository;
import tn.gov.ancs.audit.repository.ReferentielRepository;
import tn.gov.ancs.audit.security.AuditAction;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final OrganismeRepository organismeRepository;
    private final AuditeurRepository auditeurRepository;
    private final ReferentielRepository referentielRepository;

    @Transactional
    @AuditAction(action = "CREATE_MISSION", resource = "MISSION")
    public MissionResponse createMission(CreateMissionRequest request) {
        Organisme organisme = organismeRepository.findById(request.getOrganismeId())
            .orElseThrow(() -> new ResourceNotFoundException("Organisme non trouvé avec l'id: " + request.getOrganismeId()));

        Auditeur auditeur = auditeurRepository.findById(request.getAuditeurId())
            .orElseThrow(() -> new ResourceNotFoundException("Auditeur non trouvé avec l'id: " + request.getAuditeurId()));

        Referentiel referentiel = referentielRepository.findById(request.getReferentielId())
            .orElseThrow(() -> new ResourceNotFoundException("Référentiel non trouvé avec l'id: " + request.getReferentielId()));

        Mission mission = Mission.builder()
            .organisme(organisme)
            .auditeur(auditeur)
            .referentiel(referentiel)
            .dateDebut(request.getDateDebut())
            .dateFin(request.getDateFin())
            .statut(StatutMission.PLANIFIEE)
            .perimetre(request.getPerimetre())
            .build();

        Mission saved = missionRepository.save(mission);
        log.info("Nouvelle mission d'audit créée pour l'organisme {} (Auditeur: {})", organisme.getNom(), auditeur.getUtilisateur().getNom());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public MissionResponse getMissionById(UUID id, String currentUserEmail, Role currentUserRole, UUID currentUserOrganismeId) {
        Mission mission = missionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée avec l'id: " + id));

        // Validation stricte de sécurité d'accès par rôle
        validateAccess(mission, currentUserEmail, currentUserRole, currentUserOrganismeId);

        return mapToResponse(mission);
    }

    @Transactional(readOnly = true)
    public Page<MissionResponse> getMissions(Role role, String userEmail, UUID organismeId, Pageable pageable) {
        if (role == Role.ADMIN_ANCS) {
            return missionRepository.findAll(pageable).map(this::mapToResponse);
        } else if (role == Role.AUDITEUR) {
            Auditeur auditeur = auditeurRepository.findByUtilisateurId(
                utilisateurRepositoryFindByEmail(userEmail)
            ).orElseThrow(() -> new ResourceNotFoundException("Profil auditeur introuvable"));
            return missionRepository.findByAuditeurId(auditeur.getId(), pageable).map(this::mapToResponse);
        } else { // RSSI
            if (organismeId == null) {
                throw new AccessDeniedException("L'organisme de rattachement du RSSI est requis");
            }
            return missionRepository.findByOrganismeId(organismeId, pageable).map(this::mapToResponse);
        }
    }

    @Transactional
    @AuditAction(action = "UPDATE_MISSION_STATUS", resource = "MISSION", extractResourceId = true)
    public MissionResponse updateStatus(UUID id, StatutMission statut, String currentUserEmail, Role currentUserRole, UUID currentUserOrganismeId) {
        Mission mission = missionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée avec l'id: " + id));

        validateAccess(mission, currentUserEmail, currentUserRole, currentUserOrganismeId);

        // Seul l'auditeur assigné ou l'admin peut modifier le statut de la mission
        if (currentUserRole == Role.RSSI) {
            throw new AccessDeniedException("Les RSSI ne peuvent pas changer le statut des missions");
        }

        mission.setStatut(statut);
        Mission updated = missionRepository.save(mission);
        log.info("Statut de la mission {} mis à jour : {}", updated.getId(), statut);
        return mapToResponse(updated);
    }

    private void validateAccess(Mission mission, String currentUserEmail, Role currentUserRole, UUID currentUserOrganismeId) {
        if (currentUserRole == Role.ADMIN_ANCS) {
            return;
        }

        if (currentUserRole == Role.AUDITEUR) {
            if (!mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
            }
        } else if (currentUserRole == Role.RSSI) {
            if (!mission.getOrganisme().getId().equals(currentUserOrganismeId)) {
                throw new AccessDeniedException("Accès refusé : cette mission appartient à un autre organisme");
            }
        }
    }

    // Helper direct pour éviter le couplage circulaire d'injection utilisateur
    @org.springframework.beans.factory.annotation.Autowired
    private tn.gov.ancs.audit.repository.UtilisateurRepository utilisateurRepository;
    
    private UUID utilisateurRepositoryFindByEmail(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"))
            .getId();
    }

    public MissionResponse mapToResponse(Mission mission) {
        return MissionResponse.builder()
            .id(mission.getId())
            .organismeId(mission.getOrganisme().getId())
            .organismeNom(mission.getOrganisme().getNom())
            .auditeurId(mission.getAuditeur().getId())
            .auditeurNom(mission.getAuditeur().getUtilisateur().getNom())
            .referentielId(mission.getReferentiel().getId())
            .referentielNom(mission.getReferentiel().getNom())
            .dateDebut(mission.getDateDebut())
            .dateFin(mission.getDateFin())
            .statut(mission.getStatut().name())
            .perimetre(mission.getPerimetre())
            .build();
    }
}
