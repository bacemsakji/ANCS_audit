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
import tn.gov.ancs.audit.domain.enums.StatutAction;
import tn.gov.ancs.audit.dto.request.ActionRequest;
import tn.gov.ancs.audit.dto.response.ActionResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.ActionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('AUDITEUR')")
    public ResponseEntity<ActionResponse> createAction(
            @Valid @RequestBody ActionRequest request,
            Authentication authentication) {
        ActionResponse response = actionService.createAction(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mission/{missionId}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<Page<ActionResponse>> getActionsForMission(
            @PathVariable("missionId") UUID missionId,
            Authentication authentication,
            Pageable pageable) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        Page<ActionResponse> response = actionService.getActionsForMission(
            missionId, user.getEmail(), user.getRole(), orgId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Liste des actions correctives actives de l'organisme du RSSI connecté.
     */
    @GetMapping("/rssi/actives")
    @PreAuthorize("hasRole('RSSI')")
    public ResponseEntity<List<ActionResponse>> getActiveActionsForRssi(Authentication authentication) {
        Utilisateur user = getAuthenticatedUser(authentication);
        if (user.getOrganisme() == null) {
            throw new IllegalArgumentException("L'utilisateur connecté n'est rattaché à aucun organisme");
        }
        List<ActionResponse> response = actionService.getActiveActionsForRssi(user.getOrganisme().getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<ActionResponse> updateStatus(
            @PathVariable("id") UUID id,
            @RequestParam("statut") StatutAction statut,
            Authentication authentication) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        ActionResponse response = actionService.updateStatus(
            id, statut, user.getEmail(), user.getRole(), orgId
        );
        return ResponseEntity.ok(response);
    }

    private Utilisateur getAuthenticatedUser(Authentication auth) {
        return utilisateurRepository.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }
}
