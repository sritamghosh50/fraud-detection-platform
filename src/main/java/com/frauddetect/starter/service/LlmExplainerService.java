package com.frauddetect.starter.service;

import com.frauddetect.starter.model.OllamaRequest;
import com.frauddetect.starter.model.OllamaResponse;
import com.frauddetect.starter.model.RiskDecision;
import com.frauddetect.starter.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LlmExplainerService {

    private final RestTemplate restTemplate;

    @Value("${ollama.url}")
    private String ollamaUrl;

    public LlmExplainerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Generate an explanation for a suspicious transaction.
     * This method is used by TransactionConsumer.
     */
    public String generateExplanation(
            Transaction transaction,
            RiskDecision decision) {

        try {

            OllamaRequest request =
                    new OllamaRequest(
                            "llama3.2",
                            buildTransactionPrompt(
                                    transaction,
                                    decision
                            ),
                            false
                    );

            ResponseEntity<OllamaResponse> response =
                    restTemplate.postForEntity(
                            ollamaUrl,
                            request,
                            OllamaResponse.class
                    );

            if (response.getBody() != null
                    && response.getBody().getResponse() != null
                    && !response.getBody().getResponse().isBlank()) {

                return response.getBody().getResponse();
            }

            return "LLM returned an empty response.";

        } catch (Exception e) {

            System.out.println(
                    "Failed to get LLM transaction explanation: "
                            + e.getMessage()
            );

            return "The LLM analysis could not be completed.";
        }
    }

    /**
     * General-purpose explanation method.
     */
    public String explain(String transactionText) {

        try {

            OllamaRequest request =
                    new OllamaRequest(
                            "llama3.2",
                            buildPrompt(transactionText),
                            false
                    );

            ResponseEntity<OllamaResponse> response =
                    restTemplate.postForEntity(
                            ollamaUrl,
                            request,
                            OllamaResponse.class
                    );

            if (response.getBody() != null
                    && response.getBody().getResponse() != null
                    && !response.getBody().getResponse().isBlank()) {

                return response.getBody().getResponse();
            }

            return "LLM returned an empty response.";

        } catch (Exception e) {

            System.out.println(
                    "Failed to get LLM explanation: "
                            + e.getMessage()
            );

            return "The LLM analysis could not be completed.";
        }
    }

    /**
     * Build the prompt used for transaction fraud explanations.
     */
    private String buildTransactionPrompt(
            Transaction transaction,
            RiskDecision decision) {

        String reasons =
                decision.getRulesReasons() == null
                        ? "No specific rule reasons were provided."
                        : String.join(
                                ", ",
                                decision.getRulesReasons()
                        );

        return """
                You are a fraud detection assistant.

                Analyze this transaction and explain why it was
                flagged for review.

                Transaction ID: %s
                User ID: %s
                Amount: %.2f
                Transaction country: %s
                User home country: %s
                Hour of day: %d

                Final risk score: %.2f
                Risk level: %s
                ML risk score: %.2f
                Rules risk score: %.2f

                Rule-based reasons:
                %s

                Give a short, clear explanation that a human reviewer
                can understand quickly.

                Mention the main suspicious factors and what the
                reviewer should consider.

                Do not invent information that is not provided.
                """.formatted(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getCountry(),
                transaction.getUserHomeCountry(),
                transaction.getHourOfDay(),
                decision.getFinalRiskScore(),
                decision.getRiskLevel(),
                decision.getMlRiskScore(),
                decision.getRulesRiskScore(),
                reasons
        );
    }

    /**
     * Build the prompt used for general text/message explanations.
     */
    private String buildPrompt(String transactionText) {

        return """
                You are a fraud detection assistant.

                Analyze the following transaction/message and explain
                whether it looks suspicious.

                Give a short and simple explanation for a normal user.

                Transaction/message:
                %s
                """.formatted(transactionText);
    }
}