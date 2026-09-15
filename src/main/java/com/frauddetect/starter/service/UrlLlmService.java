package com.frauddetect.starter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetect.starter.model.OllamaRequest;
import com.frauddetect.starter.model.OllamaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Optional LLM analysis for URLs.
 *
 * If Ollama is unavailable, the URL rule engine remains
 * responsible for the final score.
 */
@Service
public class UrlLlmService {

    @Value("${ollama.url}")
    private String ollamaUrl;

    private static final String MODEL_NAME = "llama3.2";

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public UrlLlmService(
            RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    public MessageLlmService.LlmMessageAnalysis analyze(
            String url) {

        String safeUrl =
                url == null
                        ? ""
                        : url.replace("\"", "'");

        String prompt =
                "Analyze this URL for signs it could be malicious, "
                        + "a phishing link, or an impersonation of a real brand. "
                        + "Respond with ONLY a JSON object, no other text, "
                        + "in exactly this format: "
                        + "{\"isScam\": true or false, "
                        + "\"category\": \"short category like Phishing Link, "
                        + "Brand Impersonation, Suspicious Link, or Looks Safe\", "
                        + "\"confidence\": a number from 0 to 100, "
                        + "\"explanation\": \"2-3 sentences explaining "
                        + "your reasoning based only on the URL structure\"}. "
                        + "Do not include any text before or after the JSON. "
                        + "URL to analyze: \""
                        + safeUrl
                        + "\"";

        try {

            OllamaRequest request =
                    new OllamaRequest(
                            MODEL_NAME,
                            prompt,
                            false
                    );

            OllamaResponse response =
                    restTemplate.postForObject(
                            ollamaUrl,
                            request,
                            OllamaResponse.class
                    );

            if (response == null
                    || response.getResponse() == null) {

                return fallback();
            }

            String raw =
                    response.getResponse().trim();

            int start =
                    raw.indexOf('{');

            int end =
                    raw.lastIndexOf('}');

            if (start == -1
                    || end == -1
                    || end < start) {

                return fallback();
            }

            String jsonPart =
                    raw.substring(
                            start,
                            end + 1
                    );

            JsonNode node =
                    objectMapper.readTree(
                            jsonPart
                    );

            MessageLlmService.LlmMessageAnalysis result =
                    new MessageLlmService.LlmMessageAnalysis();

            result.isScam =
                    node.path(
                            "isScam"
                    ).asBoolean(false);

            result.category =
                    node.path(
                            "category"
                    ).asText("Unclear");

            result.confidence =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    node.path(
                                            "confidence"
                                    ).asInt(50)
                            )
                    );

            result.explanation =
                    node.path(
                            "explanation"
                    ).asText(
                            "No explanation provided."
                    );

            result.llmUnavailable =
                    false;

            return result;

        } catch (Exception e) {

            System.out.println(
                    "Failed to get LLM URL analysis: "
                            + e.getMessage()
            );

            return fallback();
        }
    }

    /**
     * LLM unavailable.
     *
     * This does NOT mean the URL is fraudulent.
     */
    private MessageLlmService.LlmMessageAnalysis fallback() {

        MessageLlmService.LlmMessageAnalysis result =
                new MessageLlmService.LlmMessageAnalysis();

        result.isScam = false;

        result.category =
                "Unclear (LLM unavailable)";

        result.confidence = 0;

        result.explanation =
                "AI analysis is currently unavailable. "
                        + "FraudGuard is relying on its "
                        + "URL structural security rules.";

        result.llmUnavailable = true;

        return result;
    }
}