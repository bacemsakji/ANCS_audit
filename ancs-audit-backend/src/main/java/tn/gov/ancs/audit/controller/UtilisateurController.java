package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.dto.request.CreateUserRequest;
import tn.gov.ancs.audit.dto.response.UtilisateurResponse;
import tn.gov.ancs.audit.service.UtilisateurService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UtilisateurResponse response = utilisateurService.getUserByEmail(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<UtilisateurResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UtilisateurResponse response = utilisateurService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<List<UtilisateurResponse>> getAllUsers() {
        List<UtilisateurResponse> response = utilisateurService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<UtilisateurResponse> getUserById(@PathVariable("id") UUID id) {
        UtilisateurResponse response = utilisateurService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<UtilisateurResponse> toggleUserStatus(@PathVariable("id") UUID id) {
        UtilisateurResponse response = utilisateurService.toggleUserStatus(id);
        return ResponseEntity.ok(response);
    }
}
