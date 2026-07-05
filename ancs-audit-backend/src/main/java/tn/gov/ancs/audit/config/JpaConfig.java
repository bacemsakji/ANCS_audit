package tn.gov.ancs.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration JPA — active l'audit automatique ({@code @CreatedDate}, {@code @LastModifiedDate}).
 *
 * <p>Le bean {@code springSecurityAuditorAware} est référencé dans {@link AuditApplication}
 * via {@code @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")}.</p>
 */
@Configuration
public class JpaConfig {

    @Bean
    public AuditorAware<String> springSecurityAuditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(auth.getName());
        };
    }
}
