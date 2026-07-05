package tn.gov.ancs.audit.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client MinIO on-premise.
 *
 * <p>Toutes les valeurs proviennent de l'application.yml / variables d'environnement.
 * Aucune valeur en dur dans ce fichier.</p>
 *
 * <p>Buckets créés automatiquement au démarrage Docker Compose (service minio-init).
 * Le service {@code StorageService} (Phase 3) gère les opérations CRUD sur MinIO.</p>
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        log.info("Initialisation du client MinIO — endpoint: {}", endpoint);
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}
