package tn.gov.ancs.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.dto.response.DashboardAdminResponse;
import tn.gov.ancs.audit.dto.response.DashboardRssiResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN_ANCS')")
    public ResponseEntity<DashboardAdminResponse> getAdminDashboard() {
        DashboardAdminResponse response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rssi")
    @PreAuthorize("hasRole('RSSI')")
    public ResponseEntity<DashboardRssiResponse> getRssiDashboard(Authentication authentication) {
        Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur RSSI non trouvé"));

        if (user.getOrganisme() == null) {
            throw new IllegalArgumentException("L'utilisateur RSSI n'est associé à aucun organisme");
        }

        DashboardRssiResponse response = dashboardService.getRssiDashboard(user.getOrganisme().getId());
        return ResponseEntity.ok(response);
    }
}
