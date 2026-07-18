package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.dto.request.ReunionTravailRequest;
import tn.gov.ancs.audit.dto.response.ReunionTravailResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.ReunionTravailService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reunions")
@RequiredArgsConstructor
public class ReunionTravailController {

    private final ReunionTravailService reunionTravailService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<ReunionTravailResponse> createReunion(
            @Valid @RequestBody ReunionTravailRequest request,
            Authentication authentication) {
        Utilisateur caller = getAuthenticatedUser(authentication);
        ReunionTravailResponse response = reunionTravailService.createReunion(request, caller.getEmail(), caller.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mission/{missionId}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<List<ReunionTravailResponse>> getReunionsByMission(
            @PathVariable("missionId") UUID missionId,
            Authentication authentication) {
        Utilisateur caller = getAuthenticatedUser(authentication);
        UUID orgId = caller.getOrganisme() != null ? caller.getOrganisme().getId() : null;
        List<ReunionTravailResponse> response = reunionTravailService.getReunionsByMission(
            missionId, caller.getEmail(), caller.getRole().name(), orgId
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<Void> deleteReunion(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        Utilisateur caller = getAuthenticatedUser(authentication);
        reunionTravailService.deleteReunion(id, caller.getEmail(), caller.getRole().name());
        return ResponseEntity.noContent().build();
    }

    private Utilisateur getAuthenticatedUser(Authentication auth) {
        return utilisateurRepository.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }
}
