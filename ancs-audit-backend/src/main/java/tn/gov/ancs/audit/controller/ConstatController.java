package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.dto.request.ConstatRequest;
import tn.gov.ancs.audit.dto.response.ConstatResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.ConstatService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/constats")
@RequiredArgsConstructor
public class ConstatController {

    private final ConstatService constatService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('AUDITEUR')")
    public ResponseEntity<ConstatResponse> submitConstat(
            @Valid @RequestBody ConstatRequest request,
            Authentication authentication) {
        ConstatResponse response = constatService.submitConstat(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{id}/preuve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUDITEUR')")
    public ResponseEntity<ConstatResponse> uploadPreuve(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        ConstatResponse response = constatService.uploadPreuve(id, file, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mission/{missionId}")
    @PreAuthorize("hasAnyRole('AUDITEUR', 'RSSI')")
    public ResponseEntity<List<ConstatResponse>> getConstatsByMission(
            @PathVariable("missionId") UUID missionId,
            Authentication authentication) {
        Utilisateur user = getAuthenticatedUser(authentication);
        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        List<ConstatResponse> response = constatService.getConstatsByMissionId(
            missionId, user.getEmail(), user.getRole().name(), orgId
        );
        return ResponseEntity.ok(response);
    }

    private Utilisateur getAuthenticatedUser(Authentication auth) {
        return utilisateurRepository.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }
}
