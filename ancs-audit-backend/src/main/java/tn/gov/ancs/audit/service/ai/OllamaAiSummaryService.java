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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaAiSummaryService implements AiSummaryService {

    private final AiConfig aiConfig;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String generateDraft(AiSummaryRequest request) throws AiUnavailableException {
        log.info("Lancement de la génération IA via Ollama (Modèle local: {})", aiConfig.getOllama().getModel());

        String systemPrompt = promptBuilder.buildSystemPrompt(request.getLangue());
        String userPrompt = promptBuilder.buildUserPrompt(request);

        String endpoint = aiConfig.getOllama().getBaseUrl() + "/api/generate";

        try {
            // Construire le body JSON requis par l'API Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getOllama().getModel());
            requestBody.put("prompt", userPrompt);
            requestBody.put("system", systemPrompt);
            requestBody.put("stream", false);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(aiConfig.getOllama().getTimeoutSeconds()))
                .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Échec Ollama — code statut HTTP: {}, réponse: {}", response.statusCode(), response.body());
                throw new AiUnavailableException("Ollama a retourné un statut HTTP invalide: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String resultText = root.path("response").asText();

            if (resultText == null || resultText.isBlank()) {
                throw new AiUnavailableException("La réponse générée par le modèle Ollama est vide.");
            }

            log.info("Génération de la synthèse exécutive terminée avec succès via Ollama.");
            return resultText;

        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Serveur Ollama local injoignable ou en échec de timeout", e);
            throw new AiUnavailableException("Le service de génération IA local est temporairement indisponible", e);
        }
    }
}
