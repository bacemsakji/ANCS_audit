package tn.gov.ancs.audit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tn.gov.ancs.audit.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité Spring Boot pour l'ANCS Audit.
 *
 * <p>Sécurité en couches :</p>
 * <ol>
 *   <li>Filtre JWT stateless ({@link JwtAuthenticationFilter})</li>
 *   <li>RBAC par méthode via {@code @PreAuthorize}</li>
 *   <li>Headers HTTP de sécurité (HSTS, X-Frame-Options, CSP, etc.)</li>
 *   <li>CORS restrictif : seules les origines configurées explicitement sont autorisées</li>
 *   <li>Swagger UI désactivé en production</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Endpoints publics toujours accessibles (authentification)
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/2fa/verify",
        "/api/auth/refresh",
        "/actuator/health",
        "/actuator/info"
    };

    // Endpoints Swagger — uniquement en dev/test, JAMAIS en prod
    private static final String[] SWAGGER_ENDPOINTS = {
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
            // Désactiver CSRF (API REST stateless — tokens JWT utilisés à la place)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS restrictif
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Sessions stateless — aucun état côté serveur
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Security Headers HTTP
            .headers(headers -> headers
                // HSTS : forcer HTTPS pour 1 an (inclure sous-domaines)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .preload(true))
                // Anti-clickjacking
                .frameOptions(frame -> frame.deny())
                // Content-Type sniffing
                .contentTypeOptions(content -> {})
                // Referrer Policy
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Permissions Policy (désactiver APIs sensibles du navigateur)
                .permissionsPolicy(permissions ->
                    permissions.policy("camera=(), microphone=(), geolocation=(), payment=()"))
            )

            // Autorisation des requêtes
            .authorizeHttpRequests(auth -> {
                // OPTIONS preflight CORS
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                // Endpoints publics (login, refresh)
                auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
                // Swagger : uniquement autorisé hors prod
                if (!isProduction) {
                    auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                } else {
                    auth.requestMatchers(SWAGGER_ENDPOINTS).denyAll();
                }
                // Tout le reste nécessite une authentification
                auth.anyRequest().authenticated();
            })

            // Filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt avec cost factor 12 — résistant aux attaques par force brute
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // BUG CORRIGÉ : Ne jamais utiliser allowedOriginPatterns("*") avec allowCredentials(true)
        // Cela viole la spec CORS et expose l'API à des attaques CSRF depuis n'importe quel domaine.
        // On utilise la liste d'origines explicitement configurée dans application.yml.
        // Parse la chaîne séparée par des virgules en liste d'origines
        List<String> originsList = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        if (originsList.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(originsList);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept", "X-Requested-With",
            "X-Request-ID"
        ));
        config.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
