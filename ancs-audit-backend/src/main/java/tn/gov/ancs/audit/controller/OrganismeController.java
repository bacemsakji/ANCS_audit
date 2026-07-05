package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.dto.request.OrganismeRequest;
import tn.gov.ancs.audit.dto.response.OrganismeResponse;
import tn.gov.ancs.audit.service.OrganismeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organismes")
@RequiredArgsConstructor
public class OrganismeController {

    private final OrganismeService organismeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<OrganismeResponse> createOrganisme(@Valid @RequestBody OrganismeRequest request) {
        OrganismeResponse response = organismeService.createOrganisme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<OrganismeResponse> getOrganismeById(@PathVariable("id") UUID id) {
        OrganismeResponse response = organismeService.getOrganismeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<Page<OrganismeResponse>> getOrganismes(
            @RequestParam(value = "secteurActivite", required = false) String secteur,
            Pageable pageable) {
        Page<OrganismeResponse> response = organismeService.getOrganismes(secteur, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<List<OrganismeResponse>> getAllOrganismesList() {
        List<OrganismeResponse> response = organismeService.getAllOrganismesList();
        return ResponseEntity.ok(response);
    }
}
