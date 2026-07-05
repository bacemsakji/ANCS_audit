package tn.gov.ancs.audit.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.repository.UtilisateurRepository;

import java.util.List;

/**
 * Implémentation de {@link UserDetailsService} pour l'ANCS Audit.
 *
 * <p>Sécurité :</p>
 * <ul>
 *   <li>CORRIGÉ : le message d'erreur ne révèle jamais l'email recherché
 *       (prévention de l'énumération de comptes / user enumeration).</li>
 *   <li>CORRIGÉ : les comptes inactifs sont gérés via le flag {@code enabled}
 *       de l'objet {@link User}, ce qui lève la bonne exception {@code DisabledException}
 *       au lieu de {@code UsernameNotFoundException} (qui était trompeur et incorrectement
 *       traité par Spring Security).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
            // CORRIGÉ : message générique — ne révèle pas si l'email existe ou non en base
            .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));

        // Spring Security requiert le préfixe "ROLE_" pour @PreAuthorize("hasRole('...')")
        String authorityRole = "ROLE_" + utilisateur.getRole().name();

        // CORRIGÉ : passer isActive comme flag 'enabled' à Spring Security User.
        // Si isActive=false, Spring Security lève DisabledException au moment de l'authentification,
        // ce qui est sémantiquement correct et traité différemment de UsernameNotFoundException.
        return new User(
            utilisateur.getEmail(),
            utilisateur.getPasswordHash(),
            utilisateur.getIsActive(),  // enabled
            true,                        // accountNonExpired
            true,                        // credentialsNonExpired
            true,                        // accountNonLocked
            List.of(new SimpleGrantedAuthority(authorityRole))
        );
    }
}
