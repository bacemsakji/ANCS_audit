package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutMission;
import tn.gov.ancs.audit.dto.request.CreateMissionRequest;
import tn.gov.ancs.audit.dto.response.MissionResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.MissionService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<MissionResponse> createMission(@Valid @RequestBody CreateMissionRequest request) {
        MissionResponse response = missionService.createMission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable("id") UUID id, Authentication authentication) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;
        
        MissionResponse response = missionService.getMissionById(
            id, user.getEmail(), user.getRole(), orgId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<Page<MissionResponse>> getMissions(Authentication authentication, Pageable pageable) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        Page<MissionResponse> response = missionService.getMissions(
            user.getRole(), user.getEmail(), orgId, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<MissionResponse> updateStatus(
            @PathVariable("id") UUID id,
            @RequestParam("statut") StatutMission statut,
            Authentication authentication) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        MissionResponse response = missionService.updateStatus(
            id, statut, user.getEmail(), user.getRole(), orgId
        );
        return ResponseEntity.ok(response);
    }

    private Utilisateur getAuthenticatedUser(Authentication auth) {
        return utilisateurRepository.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }
}
