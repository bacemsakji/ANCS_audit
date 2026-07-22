package tn.gov.ancs.audit.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tn.gov.ancs.audit.config.AiConfig;
import tn.gov.ancs.audit.dto.request.AiSummaryRequest;
import tn.gov.ancs.audit.exception.AiUnavailableException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiSummaryService implements AiSummaryService {

    private final AiConfig aiConfig;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String generateDraft(AiSummaryRequest request) throws AiUnavailableException {
        // Double garde de souveraineté des données
        if (!aiConfig.getOpenai().isEnabled()) {
            log.error("Tentative d'utilisation du provider OpenAI alors que openai.enabled=false.");
            throw new IllegalStateException("Le provider externe OpenAI est désactivé pour des raisons de souveraineté nationale des données.");
        }

        log.warn("⚠️  ALERTE SOUVERAINETÉ : Envoi de données de constats d'audit à OpenAI ({})", aiConfig.getOpenai().getModel());

        String systemPrompt = promptBuilder.buildSystemPrompt(request.getLangue());
        String userPrompt = promptBuilder.buildUserPrompt(request);

        String apiKey = aiConfig.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException("La clé d'API OpenAI (ai.openai.api-key) n'est pas configurée.");
        }

        try {
            // Corps de requête pour l'API OpenAI Chat Completions
            Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getOpenai().getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Échec API OpenAI — code HTTP: {}, réponse: {}", response.statusCode(), response.body());
                throw new AiUnavailableException("OpenAI a retourné un statut HTTP invalide: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            String resultText = "";
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                if (!message.isMissingNode()) {
                    resultText = message.path("content").asText();
                }
            }

            if (resultText == null || resultText.isBlank()) {
                throw new AiUnavailableException("La réponse générée par OpenAI est vide ou invalide.");
            }

            log.info("Génération de la synthèse exécutive terminée avec succès via OpenAI.");
            return resultText;

        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la communication externe avec l'API OpenAI", e);
            throw new AiUnavailableException("Le service OpenAI externe est injoignable", e);
        }
    }
}
