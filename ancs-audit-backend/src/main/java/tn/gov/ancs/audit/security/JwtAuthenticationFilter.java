package tn.gov.ancs.audit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre d'authentification JWT appliqué une seule fois par requête HTTP.
 *
 * <p>Extrait le token JWT de l'en-tête {@code Authorization: Bearer <token>},
 * valide sa signature et son expiration, puis positionne l'authentification
 * dans le {@code SecurityContextHolder} pour la durée de la requête.</p>
 *
 * <p>Sécurité :</p>
 * <ul>
 *   <li>CORRIGÉ : En cas d'erreur lors du chargement de l'utilisateur (compte supprimé, désactivé),
 *       la requête est rejetée avec un 401 plutôt que de silencieusement continuer.</li>
 *   <li>Les exceptions JWT sont traitées distinctement (token expiré vs. malformé vs. invalide).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                // CORRIGÉ : Propager les exceptions de chargement utilisateur (compte inactif,
                // utilisateur supprimé) plutôt que de les avaler silencieusement.
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    log.warn("Utilisateur du token JWT introuvable en base: {} — requête rejetée.", username);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("{\"error\":\"Utilisateur non reconnu\"}");
                    return;
                }

                // Vérifier que le compte est encore actif (via UserDetails.isEnabled())
                if (!userDetails.isEnabled()) {
                    log.warn("Compte désactivé pour l'utilisateur: {} — requête rejetée.", username);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("{\"error\":\"Compte désactivé\"}");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Erreur non attendue dans JwtAuthenticationFilter — requête ignorée.", ex);
            // Ne pas bloquer la requête ici — le SecurityContextHolder sera vide,
            // ce qui déclenchera un 401/403 par Spring Security.
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
