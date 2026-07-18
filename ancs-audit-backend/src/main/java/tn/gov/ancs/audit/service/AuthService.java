package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.dto.request.LoginRequest;
import tn.gov.ancs.audit.dto.request.TotpVerifyRequest;
import tn.gov.ancs.audit.dto.response.AuthResponse;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.security.AuditAction;
import tn.gov.ancs.audit.security.JwtTokenProvider;
import tn.gov.ancs.audit.security.TotpService;
import tn.gov.ancs.audit.security.UserDetailsServiceImpl;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    private final Map<String, Integer> attemptsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> lockTimeCache = new java.util.concurrent.ConcurrentHashMap<>();

    private void checkBruteForce(String email) {
        String key = email.toLowerCase();
        Long lockTime = lockTimeCache.get(key);
        if (lockTime != null && lockTime > System.currentTimeMillis()) {
            throw new BadCredentialsException("Compte temporairement bloqué en raison de multiples tentatives. Réessayez plus tard.");
        }
    }

    private void recordFailure(String email) {
        String key = email.toLowerCase();
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        attemptsCache.put(key, attempts);
        if (attempts >= 5) {
            lockTimeCache.put(key, System.currentTimeMillis() + 300000); // Bloquer pendant 5 minutes
            attemptsCache.remove(key);
        }
    }

    private void recordSuccess(String email) {
        String key = email.toLowerCase();
        attemptsCache.remove(key);
        lockTimeCache.remove(key);
    }

    /**
     * Authentifie l'utilisateur. Si l'utilisateur est un Administrateur ANCS,
     * l'authentification à deux facteurs (2FA) est obligatoire.
     *
     * SÉCURITÉ : Pas de fuite d'état du compte (account status leak) — le mot de passe
     * est vérifié avant le statut actif du compte.
     */
    @Transactional(readOnly = true)
    @AuditAction(action = "LOGIN_ATTEMPT")
    public AuthResponse authenticate(LoginRequest request) {
        checkBruteForce(request.getEmail());

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> {
                recordFailure(request.getEmail());
                return new BadCredentialsException("Identifiants incorrects");
            });

        // SÉCURITÉ : valider le mot de passe avant de rejeter pour inactivité pour éviter la fuite d'état
        if (!passwordEncoder.matches(request.getPassword(), utilisateur.getPasswordHash())) {
            recordFailure(request.getEmail());
            throw new BadCredentialsException("Identifiants incorrects");
        }

        if (!utilisateur.getIsActive()) {
            recordFailure(request.getEmail());
            throw new BadCredentialsException("Identifiants incorrects");
        }

        // Vérifier si la double authentification (2FA) est requise pour cet utilisateur
        if (utilisateur.getRole() == Role.ADMIN_ANCS && utilisateur.getTotpEnabled()) {
            log.info("Authentification à deux facteurs requise pour l'administrateur: {}", utilisateur.getEmail());
            
            // SÉCURITÉ : Génération d'un token MFA temporaire signé pour lier cette étape à la validation TOTP
            String mfaToken = jwtTokenProvider.generateMfaToken(utilisateur.getEmail());

            return AuthResponse.builder()
                .email(utilisateur.getEmail())
                .name(utilisateur.getNom())
                .role(utilisateur.getRole().name())
                .mfaRequired(true)
                .mfaToken(mfaToken)
                .build();
        }

        recordSuccess(request.getEmail());

        // Pour les autres rôles (Auditeur, RSSI), générer directement les tokens JWT
        return generateAuthResponse(utilisateur);
    }

    /**
     * Vérifie le code 2FA pour l'administrateur et renvoie les tokens si valide.
     *
     * SÉCURITÉ : Utilise mfaToken au lieu de l'email pour garantir qu'un mot de passe
     * valide a bien été fourni à l'étape précédente.
     */
    @Transactional
    @AuditAction(action = "LOGIN_2FA_VERIFY")
    public AuthResponse verifyTotp(TotpVerifyRequest request) {
        String email;
        try {
            email = jwtTokenProvider.getEmailFromMfaToken(request.getMfaToken());
        } catch (Exception e) {
            throw new BadCredentialsException("Jeton intermédiaire MFA invalide ou expiré");
        }

        checkBruteForce(email);

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Identifiants ou code incorrects"));

        if (!utilisateur.getIsActive() || utilisateur.getRole() != Role.ADMIN_ANCS) {
            recordFailure(email);
            throw new BadCredentialsException("Accès non autorisé");
        }

        String secret = utilisateur.getTotpSecret();
        if (secret == null || secret.isEmpty()) {
            recordFailure(email);
            throw new BadCredentialsException("2FA non configurée pour cet administrateur. Contactez le support.");
        }

        boolean isValid = totpService.verifyCode(request.getCode(), secret);
        if (!isValid) {
            recordFailure(email);
            throw new BadCredentialsException("Code TOTP invalide");
        }

        // CORRIGÉ : Activation officielle de la TOTP 2FA et persistance en base (transaction d'écriture active)
        if (!utilisateur.getTotpEnabled()) {
            log.info("Activation officielle de la TOTP 2FA pour: {}", utilisateur.getEmail());
            utilisateur.setTotpEnabled(true);
            utilisateurRepository.save(utilisateur);
        }

        recordSuccess(email);

        return generateAuthResponse(utilisateur);
    }

    /**
     * Régénère un nouveau couple de tokens JWT à partir d'un Refresh Token valide.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Token de rafraîchissement expiré ou invalide");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new BadCredentialsException("Utilisateur non trouvé"));

        if (!utilisateur.getIsActive()) {
            throw new BadCredentialsException("Compte désactivé");
        }

        return generateAuthResponse(utilisateur);
    }

    private AuthResponse generateAuthResponse(Utilisateur utilisateur) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(utilisateur.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, utilisateur.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .mfaRequired(false)
            .email(utilisateur.getEmail())
            .name(utilisateur.getNom())
            .role(utilisateur.getRole().name())
            .build();
    }
}
