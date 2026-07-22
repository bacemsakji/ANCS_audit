package tn.gov.ancs.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.Rapport;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.dto.response.RapportResponse;
import tn.gov.ancs.audit.dto.response.SyntheseIaResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.RapportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutSoumissionAncs;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
public class RapportController {

    private final RapportService rapportService;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Génère une proposition de synthèse exécutive par l'IA locale (Ollama).
     * Rôle requis : AUDITEUR ou ADMIN_ANCS.
     */
    @PostMapping("/missions/{missionId}/synthese-ia")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<SyntheseIaResponse> generateSyntheseIa(
            @PathVariable("missionId") UUID missionId,
            @RequestParam(value = "langue", defaultValue = "FR") String langue,
            Authentication authentication) {
        SyntheseIaResponse response = rapportService.generateSyntheseIa(missionId, langue, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Génère le rapport final PDF ou DOCX pour une mission donnée.
     * Enregistre l'objet généré dans MinIO et stocke les métadonnées.
     */
    @PostMapping("/generer/{missionId}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<Map<String, Object>> generateRapport(
            @PathVariable("missionId") UUID missionId,
            @RequestParam("type") String type,
            @RequestBody Map<String, Object> requestBody,
            org.springframework.security.core.Authentication authentication) {

        String synthese = (String) requestBody.get("syntheseExecutive");
        Boolean isIa = (Boolean) requestBody.getOrDefault("isIaGenerated", false);

        Rapport rapport = rapportService.generateRapport(missionId, type, synthese, isIa, authentication.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("rapportId", rapport.getId());
        response.put("version", rapport.getVersion());
        response.put("fichierUrl", rapport.getFichierUrl());
        response.put("dateGeneration", rapport.getDateGeneration());

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère le lien de téléchargement pré-signé et sécurisé pour un rapport.
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR', 'RSSI')")
    public ResponseEntity<Map<String, String>> downloadRapport(
            @PathVariable("id") UUID id,
            Authentication authentication) {

        Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté non trouvé"));

        UUID orgId = user.getOrganisme() != null ? user.getOrganisme().getId() : null;

        String downloadUrl = rapportService.getRapportDownloadUrl(
            id, user.getEmail(), user.getRole(), orgId
        );

        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Liste tous les rapports d'une mission donnée (toutes versions).
     * Rôle requis : AUDITEUR (assigné) ou ADMIN_ANCS.
     */
    @GetMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<List<RapportResponse>> getRapportsByMission(
            @PathVariable("missionId") UUID missionId,
            Authentication authentication) {
        List<RapportResponse> rapports = rapportService.getRapportsByMission(missionId, authentication.getName());
        return ResponseEntity.ok(rapports);
    }

    /**
     * Liste tous les rapports appartenant à l'organisme du RSSI connecté.
     * Rôle requis : RSSI ou ADMIN_ANCS.
     */
    @GetMapping("/mon-organisme")
    @PreAuthorize("hasAnyRole('RSSI', 'ADMIN_ANCS')")
    public ResponseEntity<List<RapportResponse>> getMyOrganismeRapports(Authentication authentication) {
        List<RapportResponse> rapports = rapportService.getMyOrganismeRapports(authentication.getName());
        return ResponseEntity.ok(rapports);
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<Map<String, Object>> submitRapport(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté non trouvé"));

        Rapport rapport = rapportService.submitRapport(id, user.getEmail(), user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("rapportId", rapport.getId());
        response.put("statutSoumissionAncs", rapport.getStatutSoumissionAncs().name());
        response.put("dateSoumissionAncs", rapport.getDateSoumissionAncs());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accepter")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<Map<String, Object>> acceptRapport(
            @PathVariable("id") UUID id) {
        Rapport rapport = rapportService.acceptRapport(id);

        Map<String, Object> response = new HashMap<>();
        response.put("rapportId", rapport.getId());
        response.put("statutSoumissionAncs", rapport.getStatutSoumissionAncs().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<Map<String, Object>> rejectRapport(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> requestBody) {
        String motifRejet = requestBody.get("motifRejet");
        if (motifRejet == null || motifRejet.trim().isEmpty()) {
            throw new IllegalArgumentException("Le motif de rejet est obligatoire.");
        }
        Rapport rapport = rapportService.rejectRapport(id, motifRejet);

        Map<String, Object> response = new HashMap<>();
        response.put("rapportId", rapport.getId());
        response.put("statutSoumissionAncs", rapport.getStatutSoumissionAncs().name());
        response.put("motifRejet", rapport.getMotifRejet());
        response.put("dateLimiteResoumission", rapport.getDateLimiteResoumission());
        return ResponseEntity.ok(response);
    }
}
