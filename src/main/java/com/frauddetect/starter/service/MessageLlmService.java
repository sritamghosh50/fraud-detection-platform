package com.frauddetect.starter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetect.starter.model.OllamaRequest;
import com.frauddetect.starter.model.OllamaResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Asks the LLM to analyze a suspicious message and classify it.
 * We ask for a strict JSON response so we can reliably extract fields,
 * instead of trying to parse free-form paragraphs.
 */
@Service
public class MessageLlmService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "llama3.2";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageLlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LlmMessageAnalysis analyze(String messageText) {
        String prompt = "Analyze the following message for signs of a scam or fraud. "
                + "Respond with ONLY a JSON object, no other text, in exactly this format: "
                + "{\"isScam\": true or false, \"category\": \"short category name like Job Scam, Phishing, "
                + "Lottery Scam, Impersonation Scam, or Not a Scam\", "
                + "\"confidence\": a number from 0 to 100, "
                + "\"explanation\": \"2-3 sentences explaining your reasoning, based only on the message text\"}. "
                + "Do not include any text before or after the JSON. "
                + "Message to analyze: \"" + messageText.replace("\"", "'") + "\"";

        try {
            OllamaRequest request = new OllamaRequest(MODEL_NAME, prompt, false);
            OllamaResponse response = restTemplate.postForObject(OLLAMA_URL, request, OllamaResponse.class);

            if (response == null || response.getResponse() == null) {
                return fallback();
            }

            // Small models sometimes add stray text around the JSON - extract just the { ... } part
                       String raw = response.getResponse().trim();
            System.out.println("Raw LLM response for message analysis: " + raw);

            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start == -1 || end == -1 || end < start) {
                System.out.println("Could not find valid JSON braces in LLM response - using fallback.");
                return fallback();
            }
            String jsonPart = raw.substring(start, end + 1);

            JsonNode node = objectMapper.readTree(jsonPart);

            LlmMessageAnalysis result = new LlmMessageAnalysis();
            result.isScam = node.path("isScam").asBoolean(false);
            result.category = node.path("category").asText("Unclear");
            result.confidence = node.path("confidence").asInt(50);
            result.explanation = node.path("explanation").asText("No explanation provided.");
            return result;

        } catch (Exception e) {
            System.out.println("Failed to get LLM message analysis: " + e.getMessage());
            return fallback();
        }
    }

    private LlmMessageAnalysis fallback() {
        LlmMessageAnalysis result = new LlmMessageAnalysis();
        result.isScam = false;
        result.category = "Unclear (LLM unavailable)";
        result.confidence = 0;
        result.explanation = "The LLM analysis could not be completed. Relying on rule-based indicators only.";
        return result;
    }

    public static class LlmMessageAnalysis {
        public boolean isScam;
        public String category;
        public int confidence;
        public String explanation;
    }
}