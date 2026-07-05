package tn.gov.ancs.audit.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.gov.ancs.audit.domain.AuditLog;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.repository.AuditLogRepository;
import tn.gov.ancs.audit.repository.UtilisateurRepository;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aspect AOP de traçabilité des actions sensibles.
 *
 * <p>Intercepte toutes les méthodes annotées avec {@link AuditAction} et persiste
 * un enregistrement immuable dans la table {@code audit_log}.</p>
 *
 * <p>Corrections de sécurité appliquées :</p>
 * <ul>
 *   <li>CORRIGÉ — Validation de l'IP extraite du header {@code X-Forwarded-For}
 *       pour prévenir l'injection de valeurs arbitraires (spoofing d'IP dans les logs).</li>
 *   <li>CORRIGÉ — Troncature du User-Agent à 512 caractères maximum pour éviter
 *       les entrées de log anormalement longues (potentielle injection de log).</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Regex de validation d'adresse IPv4 et IPv6 (défense en profondeur contre le log spoofing)
    private static final Pattern IP_V4_PATTERN = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)" +
        "(\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)){3}$"
    );
    private static final Pattern IP_V6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
        "^[0-9a-fA-F]{1,4}::([0-9a-fA-F]{1,4}:){0,5}[0-9a-fA-F]{1,4}$|" +
        "^[0-9a-fA-F]{1,4}:[0-9a-fA-F]{1,4}::([0-9a-fA-F]{1,4}:){0,4}[0-9a-fA-F]{1,4}$|^::1$|^::$"
    );
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_IP_LENGTH = 45; // Max IPv6 length

    /**
     * Intercepte et trace les exécutions de méthodes annotées avec {@link AuditAction}.
     */
    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAuditAction(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            String ipAddress = request != null ? getClientIp(request) : "UNKNOWN";
            String userAgent = request != null ? sanitizeUserAgent(request.getHeader("User-Agent")) : "UNKNOWN";

            Utilisateur utilisateur = getAuthenticatedUser();

            UUID resourceId = null;
            if (auditAction.extractResourceId()) {
                resourceId = extractResourceIdFromArgs(joinPoint);
            }

            AuditLog logEntry = AuditLog.builder()
                .utilisateur(utilisateur)
                .action(auditAction.action())
                .resource(auditAction.resource())
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();

            auditLogRepository.save(logEntry);
            log.debug("Action d'audit enregistrée: {} par {} depuis {}", 
                auditAction.action(),
                utilisateur != null ? utilisateur.getEmail() : "SYSTEM",
                ipAddress);

        } catch (Exception e) {
            // Ne jamais laisser un échec de log bloquer le flux applicatif métier
            log.error("Erreur lors de l'enregistrement du log d'audit pour l'action {}", auditAction.action(), e);
        }
    }

    private Utilisateur getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return utilisateurRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    private UUID extractResourceIdFromArgs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        if (args != null && parameterNames != null) {
            for (int i = 0; i < args.length; i++) {
                String paramName = parameterNames[i].toLowerCase();
                if (paramName.contains("id") || paramName.contains("uuid") || paramName.contains("key")) {
                    if (args[i] instanceof UUID uuid) {
                        return uuid;
                    } else if (args[i] instanceof String str) {
                        try {
                            return UUID.fromString(str);
                        } catch (IllegalArgumentException ignored) {
                            // Pas un UUID — ignorer
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extrait l'IP réelle du client en tenant compte des proxies inverses.
     * Valide le format pour prévenir le log injection via X-Forwarded-For.
     *
     * <p>SÉCURITÉ : En production, le header {@code X-Forwarded-For} ne doit être
     * honoré que si le proxy inverse (Nginx/Traefik) est configuré pour le définir.
     * Sans proxy validé, un attaquant peut forger ce header.</p>
     */
    private String getClientIp(HttpServletRequest request) {
        String xffHeader = request.getHeader("X-Forwarded-For");
        String realIpHeader = request.getHeader("X-Real-IP");

        String candidateIp = null;

        if (xffHeader != null && !xffHeader.isBlank()) {
            // Prendre uniquement la première IP de la liste (client original)
            candidateIp = xffHeader.split(",")[0].trim();
        } else if (realIpHeader != null && !realIpHeader.isBlank()) {
            candidateIp = realIpHeader.trim();
        }

        // Valider le format de l'IP extraite (défense contre l'injection)
        if (candidateIp != null && candidateIp.length() <= MAX_IP_LENGTH && isValidIp(candidateIp)) {
            return candidateIp;
        }

        // Fallback sur l'IP de connexion directe (toujours fiable)
        return request.getRemoteAddr();
    }

    /**
     * Valide qu'une chaîne est bien une adresse IPv4 ou IPv6 valide.
     */
    private boolean isValidIp(String ip) {
        return IP_V4_PATTERN.matcher(ip).matches() || IP_V6_PATTERN.matcher(ip).matches();
    }

    /**
     * Tronque et nettoie le User-Agent pour éviter les logs anormalement longs.
     */
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        // Tronquer à MAX_USER_AGENT_LENGTH caractères maximum
        if (userAgent.length() > MAX_USER_AGENT_LENGTH) {
            return userAgent.substring(0, MAX_USER_AGENT_LENGTH) + "[TRUNCATED]";
        }
        return userAgent;
    }
}
