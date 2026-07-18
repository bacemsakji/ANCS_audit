package tn.gov.ancs.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test de démarrage du contexte Spring Boot.
 * Vérifie que l'application démarre correctement avec une base H2 en mémoire.
 *
 * <p>Points clés de la configuration de test :
 * <ul>
 *   <li>Flyway désactivé — le schéma est créé par Hibernate (ddl-auto=create-drop)</li>
 *   <li>H2 en mémoire en mode PostgreSQL (compatibilité syntaxique)</li>
 *   <li>Firebase, MinIO et Ollama désactivés (pas de dépendances externes)</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // ---- Base de données in-memory (pas de Docker requis) ----
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    // Hibernate crée le schéma depuis les entités — Flyway ne tourne PAS
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",

    // ---- JWT test uniquement (>= 64 chars = 512 bits) ----
    "jwt.secret=test_secret_key_for_unit_tests_only_NOT_for_production_pad64chars12345678",
    "jwt.access-token-expiration-ms=900000",
    "jwt.refresh-token-expiration-ms=604800000",

    // ---- Services externes désactivés ----
    "app.cors.allowed-origins=http://localhost:3000,http://localhost:8080",
    "firebase.enabled=false",
    "minio.endpoint=http://localhost:9000",
    "minio.access-key=test",
    "minio.secret-key=testpassword",
    "ai.provider=ollama",
    "ai.ollama.base-url=http://localhost:11434"
})
class AuditApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void testPasswords() {
        System.out.println("======================================");
        System.out.println("ADMIN MATCH: " + passwordEncoder.matches("Admin@ANCS2024!", "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFqVExL1sRl2qzu"));
        System.out.println("AUDITEUR MATCH: " + passwordEncoder.matches("Auditeur@ANCS2024!", "$2a$12$QoW6t8pU7SiXuGqP4nHHiOF/3PZi0/pDW7z5lR2vZ2X1K9mXQlJGG"));
        System.out.println("RSSI MATCH: " + passwordEncoder.matches("Rssi@ANCS2024!", "$2a$12$NxZmVoY8pJ3qKlR5tW2HhOKdF6aBXTzP0mWk8sLnQ4uY2vC1jRqI."));
        System.out.println("--------------------------------------");
        System.out.println("NEW ADMIN HASH: " + passwordEncoder.encode("Admin@ANCS2024!"));
        System.out.println("NEW AUDITEUR HASH: " + passwordEncoder.encode("Auditeur@ANCS2024!"));
        System.out.println("NEW RSSI HASH: " + passwordEncoder.encode("Rssi@ANCS2024!"));
        System.out.println("======================================");
    }

    @Test
    void contextLoads() {
        // Vérifie que le contexte Spring démarre sans erreur
    }
}
