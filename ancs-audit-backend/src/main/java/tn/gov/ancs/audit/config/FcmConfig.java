package tn.gov.ancs.audit.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Configuration Firebase Cloud Messaging (FCM).
 *
 * <p>Activé uniquement si la propriété {@code firebase.enabled=true} (défaut en prod).
 * Désactivé en DEV pour éviter la dépendance au service account.</p>
 *
 * <p>Le fichier de service account JSON est chargé depuis le chemin défini dans
 * {@code firebase.service-account-path}. Ne jamais commiter ce fichier dans Git.</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FcmConfig {

    @Value("${firebase.service-account-path}")
    private Resource serviceAccountResource;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initialisation de Firebase App — service account: {}",
            serviceAccountResource.getDescription());

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.getInputStream()))
                .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}
