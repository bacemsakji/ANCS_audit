package tn.gov.ancs.audit.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;

/**
 * Configuration du module IA — synthèse exécutive assistée.
 *
 * <p>Le provider actif est déterminé par la propriété {@code ai.provider}.
 * Par défaut : {@code ollama} (modèle auto-hébergé, souveraineté des données garantie).</p>
 *
 * <p><strong>⚠️  AVERTISSEMENT SOUVERAINETÉ DES DONNÉES :</strong><br>
 * L'activation du provider {@code openai} ou {@code anthropic} implique l'envoi de données
 * de constats d'audit vers des serveurs tiers. Ne jamais activer sans validation formelle
 * de la DGSI et de la direction générale de l'ANCS.</p>
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** Provider actif. Valeurs : {@code ollama}, {@code openai}, {@code anthropic}. */
    private String provider = "ollama";

    private OllamaProperties ollama = new OllamaProperties();
    private OpenAiProperties openai = new OpenAiProperties();

    @PostConstruct
    public void logConfiguration() {
        log.info("Module IA — provider actif: {}", provider);
        if ("openai".equalsIgnoreCase(provider) || "anthropic".equalsIgnoreCase(provider)) {
            log.warn(
                "⚠️  AVERTISSEMENT SOUVERAINETÉ DES DONNÉES : le provider IA '{}' est actif. " +
                "Des données de constats d'audit seront envoyées à un serveur tiers. " +
                "Assurez-vous d'avoir obtenu les validations réglementaires requises.",
                provider
            );
        }
        if ("openai".equalsIgnoreCase(provider) && !openai.isEnabled()) {
            throw new IllegalStateException(
                "Le provider 'openai' est configuré mais openai.enabled=false. " +
                "Définissez openai.enabled=true explicitement après validation DGSI/ANCS."
            );
        }
    }

    @Getter
    @Setter
    public static class OllamaProperties {
        private String baseUrl = "http://localhost:11434";
        private String model = "mistral";
        private int timeoutSeconds = 60;
    }

    @Getter
    @Setter
    public static class OpenAiProperties {
        /**
         * Doit rester {@code false} par défaut.
         * Voir l'avertissement dans la Javadoc de la classe parente.
         */
        private boolean enabled = false;
        private String apiKey = "";
        private String model = "gpt-4o";
    }
}
