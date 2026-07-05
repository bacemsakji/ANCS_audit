package tn.gov.ancs.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'application ANCS Audit Backend.
 *
 * <p>Agence Nationale de Cybersécurité (ANCS) — Tunisie<br>
 * Plateforme de gestion des audits de sécurité des systèmes d'information<br>
 * Référence réglementaire : Décret-loi 2023-17, Arrêté du 01/10/2019</p>
 *
 * @author Équipe ANCS
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableAsync
@EnableScheduling
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
