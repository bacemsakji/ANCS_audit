package tn.gov.ancs.audit.config;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des beans pour le module TOTP 2FA.
 *
 * <p>Spring Boot 3.x n'activant plus les auto-configurations basées sur
 * l'ancien format `spring.factories` utilisé par `totp-spring-boot-starter` 1.7.1,
 * ces définitions explicites garantissent le bon fonctionnement du mécanisme
 * MFA (TOTP) tant au démarrage de l'application qu'au sein des suites de tests.</p>
 */
@Configuration
public class TotpConfig {

    @Bean
    public SecretGenerator secretGenerator() {
        return new DefaultSecretGenerator(64); // 64-character secret
    }

    @Bean
    public TimeProvider timeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    public CodeGenerator codeGenerator() {
        return new DefaultCodeGenerator();
    }

    @Bean
    public CodeVerifier codeVerifier(TimeProvider timeProvider, CodeGenerator codeGenerator) {
        return new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    @Bean
    public QrDataFactory qrDataFactory() {
        // Paramètres standards : SHA-1, 6 chiffres, période de 30 secondes
        return new QrDataFactory(HashingAlgorithm.SHA1, 6, 30);
    }

    @Bean
    public QrGenerator qrGenerator() {
        return new ZxingPngQrGenerator();
    }
}
