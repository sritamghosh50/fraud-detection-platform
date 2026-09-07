package com.frauddetect.starter.service;

import com.frauddetect.starter.model.OllamaRequest;
import com.frauddetect.starter.model.OllamaResponse;
import com.frauddetect.starter.model.RiskDecision;
import com.frauddetect.starter.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Generates a plain-English explanation for an already calculated
 * fraud-risk decision.
 *
 * IMPORTANT:
 * The LLM only explains the decision.
 * It does NOT make the fraud decision.
 */
@Service
public class LlmExplainerService {

    private static final String DEFAULT_OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL_NAME =
            "llama3.2";

    private final RestTemplate restTemplate;
    private final String ollamaUrl;

    public LlmExplainerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;

        String configuredUrl = System.getenv("OLLAMA_URL");

        if (configuredUrl == null || configuredUrl.isBlank()) {
            this.ollamaUrl = DEFAULT_OLLAMA_URL;
        } else {
            this.ollamaUrl = configuredUrl + "/api/generate";
        }

        System.out.println("Ollama URL: " + this.ollamaUrl);
    }

    public String generateExplanation(
            Transaction transaction,
            RiskDecision decision) {

        String prompt = buildPrompt(transaction, decision);

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

            if (response != null
                    && response.getResponse() != null
                    && !response.getResponse().isBlank()) {

                return response.getResponse().trim();

            } else {

                return "Explanation unavailable (empty response from LLM).";
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to call Ollama at "
                            + ollamaUrl
                            + ": "
                            + e.getMessage()
            );

            return "Explanation unavailable (LLM service could not be reached).";
        }
    }

    private String buildPrompt(
            Transaction transaction,
            RiskDecision decision) {

        double mlScore = decision.getMlRiskScore();
        double rulesScore = decision.getRulesRiskScore();
        double finalScore = decision.getFinalRiskScore();

        return String.format(

                """
                You are a fraud detection explanation assistant.

                Your job is ONLY to explain an already calculated risk decision.
                Do NOT change the decision.
                Do NOT invent facts.
                Do NOT make up thresholds.

                IMPORTANT SCORE INTERPRETATION:
                - All risk scores range from 0 to 100.
                - 0 means very low risk.
                - 100 means extremely high risk.
                - Therefore, 99.99 is extremely HIGH risk, NOT low risk.
                - A higher score always means higher risk.

                Exact transaction information:
                Amount: %.2f
                Transaction country: %s
                User home country: %s
                Hour of day: %d

                Exact calculated results:
                Rules engine risk score: %.2f out of 100
                ML model risk score: %.2f out of 100
                Final combined risk score: %.2f out of 100
                Final risk level: %s

                Write exactly 2 or 3 short sentences in simple English.

                Explain why the transaction received the given risk level.
                If the ML score is high, describe it as high risk.
                If the rules score is high, describe it as high risk.
                If the final score is high, describe the overall risk as high.
                You may mention the country difference or unusual hour only
                because those exact values were provided.

                Never say that a score close to 100 is low or very low.
                Never invent a reason that is not supported by the values above.
                """,

                transaction.getAmount(),
                transaction.getCountry(),
                transaction.getUserHomeCountry(),
                transaction.getHourOfDay(),
                rulesScore,
                mlScore,
                finalScore,
                decision.getRiskLevel()
        );
    }
}