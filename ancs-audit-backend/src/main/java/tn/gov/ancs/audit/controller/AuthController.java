package tn.gov.ancs.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.dto.request.LoginRequest;
import tn.gov.ancs.audit.dto.request.TotpVerifyRequest;
import tn.gov.ancs.audit.dto.response.AuthResponse;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.security.TotpService;
import tn.gov.ancs.audit.service.AuthService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TotpService totpService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verify2fa(@Valid @RequestBody TotpVerifyRequest request) {
        AuthResponse response = authService.verifyTotp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody tn.gov.ancs.audit.dto.request.TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Génère et retourne les données d'enrôlement 2FA (QR Code en base64 + clé secrète)
     * pour l'administrateur connecté.
     */
    @GetMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setup2fa(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(principal.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Générer le secret si non présent
        String secret = utilisateur.getTotpSecret();
        if (secret == null || secret.isEmpty()) {
            secret = totpService.generateSecret();
            utilisateur.setTotpSecret(secret);
            utilisateurRepository.save(utilisateur);
        }

        String qrCodeUri = totpService.getQrCodeImageUri(utilisateur.getEmail(), secret);

        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeUri", qrCodeUri);

        return ResponseEntity.ok(response);
    }
}
