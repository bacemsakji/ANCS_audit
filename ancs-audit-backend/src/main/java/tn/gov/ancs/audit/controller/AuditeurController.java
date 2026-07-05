package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;
import tn.gov.ancs.audit.dto.request.CreateAuditeurRequest;
import tn.gov.ancs.audit.dto.response.AuditeurResponse;
import tn.gov.ancs.audit.service.AuditeurService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditeurs")
@RequiredArgsConstructor
public class AuditeurController {

    private final AuditeurService auditeurService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<AuditeurResponse> certifyAuditeur(@Valid @RequestBody CreateAuditeurRequest request) {
        AuditeurResponse response = auditeurService.certifyAuditeur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_ANCS', 'AUDITEUR')")
    public ResponseEntity<AuditeurResponse> getAuditeurById(@PathVariable("id") UUID id) {
        AuditeurResponse response = auditeurService.getAuditeurById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<Page<AuditeurResponse>> getAuditeurs(
            @RequestParam(value = "statut", required = false) StatutAuditeur statut,
            Pageable pageable) {
        Page<AuditeurResponse> response = auditeurService.getAuditeurs(statut, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<List<AuditeurResponse>> getAllAuditeursList() {
        List<AuditeurResponse> response = auditeurService.getAllAuditeursList();
        return ResponseEntity.ok(response);
    }
}
