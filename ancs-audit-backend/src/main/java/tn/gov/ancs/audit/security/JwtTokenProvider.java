package tn.gov.ancs.audit.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Fournisseur de tokens JWT pour l'ANCS Audit.
 *
 * <p>Sécurité :</p>
 * <ul>
 *   <li>Algorithme HMAC-SHA256 (HS256)</li>
 *   <li>Clé dérivée depuis la variable d'environnement {@code JWT_SECRET} (minimum 256 bits)</li>
 *   <li>BUG CORRIGÉ : suppression du fallback silencieux vers une clé générée aléatoirement
 *       (qui changerait à chaque démarrage et invaliderait tous les tokens actifs en production).</li>
 *   <li>L'application refuse de démarrer si la clé est trop courte (fail-fast sécurisé).</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {

        byte[] secretBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // CORRIGÉ : Rejet dur si la clé est trop courte (minimum 32 octets = 256 bits pour HS256).
        // L'ancienne version tombait silencieusement sur une clé aléatoire éphémère,
        // ce qui rendrait tous les refresh tokens invalides après chaque redémarrage.
        if (secretBytes.length < 32) {
            throw new WeakKeyException(
                "La clé JWT est trop courte (" + secretBytes.length + " octets). " +
                "Configurez JWT_SECRET avec au moins 32 octets (256 bits) dans vos variables d'environnement. " +
                "Générez-en une avec : openssl rand -hex 64"
            );
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        log.info("JwtTokenProvider initialisé avec succès (longueur de clé: {} octets)", secretBytes.length);
    }

    /**
     * Génère un Token d'accès (Access Token) pour un utilisateur.
     * Le rôle est inclus dans les claims pour éviter une requête DB supplémentaire à chaque appel.
     */
    public String generateAccessToken(UserDetails userDetails, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername(), accessTokenExpirationMs);
    }

    /**
     * Génère un Token de rafraîchissement (Refresh Token) pour un utilisateur.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpirationMs);
    }

    /**
     * Génère un jeton intermédiaire à courte durée (5 min) indiquant que le mot de passe
     * a été vérifié avec succès mais que le code 2FA reste requis.
     */
    public String generateMfaToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("mfa_pending", true);
        // Expiration de 5 minutes (300 000 ms)
        return createToken(claims, email, 300000);
    }

    /**
     * Extrait l'adresse e-mail et valide que le jeton intermédiaire MFA est légitime.
     */
    public String getEmailFromMfaToken(String token) {
        Claims claims = parseClaims(token);
        Boolean mfaPending = claims.get("mfa_pending", Boolean.class);
        if (mfaPending == null || !mfaPending) {
            throw new JwtException("Jeton intermédiaire MFA invalide");
        }
        return claims.getSubject();
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    /**
     * Extrait l'adresse e-mail (subject) du token JWT.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrait le rôle du token JWT.
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Valide l'intégrité et la date d'expiration du token.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token JWT non supporté: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformé: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token JWT invalide: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims vide: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
