package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class MessageAnalysisService {

    private final MessageRuleService messageRuleService;
    private final MessageLlmService messageLlmService;

    private static final double LLM_WEIGHT = 0.6;
    private static final double RULES_WEIGHT = 0.4;

    public MessageAnalysisService(
            MessageRuleService messageRuleService,
            MessageLlmService messageLlmService) {

        this.messageRuleService = messageRuleService;
        this.messageLlmService = messageLlmService;
    }

    /*
     * Normal message analysis.
     *
     * Uses rules + LLM when LLM is available.
     */
    public MessageAnalysisResult analyze(String messageText) {

        MessageRuleService.RuleScanResult ruleResult =
                messageRuleService.scan(messageText);

        MessageLlmService.LlmMessageAnalysis llmResult =
                messageLlmService.analyze(messageText);

        /*
         * If LLM is unavailable, use rules only.
         */
        if (llmResult.llmUnavailable) {
            return buildRuleOnlyResult(ruleResult);
        }

        double llmScore =
                llmResult.isScam
                        ? llmResult.confidence
                        : (100 - llmResult.confidence);

        double finalScore =
                (llmScore * LLM_WEIGHT)
                        + (ruleResult.score * RULES_WEIGHT);

        if (ruleResult.score >= 70
                && finalScore < ruleResult.score) {

            finalScore = ruleResult.score;
        }

        return buildResult(
                finalScore,
                llmResult.category,
                ruleResult,
                llmResult.explanation
        );
    }

    /*
     * Image analysis uses ONLY deterministic rules.
     *
     * This is intentionally fast.
     * It does not wait for Ollama.
     */
    public MessageAnalysisResult analyzeImageText(
            String extractedText) {

        MessageRuleService.RuleScanResult ruleResult =
                messageRuleService.scan(extractedText);

        return buildRuleOnlyResult(ruleResult);
    }

    private MessageAnalysisResult buildRuleOnlyResult(
            MessageRuleService.RuleScanResult ruleResult) {

        String category =
                ruleResult.likelyCategory;

        if (category == null
                || category.isBlank()
                || category.equalsIgnoreCase("Unclear")) {

            category = "Rule-Based Analysis";
        }

        String explanation;

        if (ruleResult.score == 0) {

            explanation =
                    "No strong scam indicators were detected "
                            + "by FraudGuard's security rules.";

        } else {

            explanation =
                    "The result is based on FraudGuard's "
                            + "deterministic scam and phishing rules.";
        }

        return buildResult(
                ruleResult.score,
                category,
                ruleResult,
                explanation
        );
    }

    private MessageAnalysisResult buildResult(
            double score,
            String category,
            MessageRuleService.RuleScanResult ruleResult,
            String explanation) {

        int finalScore =
                (int) Math.round(
                        Math.min(
                                100,
                                Math.max(
                                        0,
                                        score
                                )
                        )
                );

        String riskLevel;

        if (finalScore >= 85) {

            riskLevel = "CRITICAL";

        } else if (finalScore >= 60) {

            riskLevel = "HIGH";

        } else if (finalScore >= 30) {

            riskLevel = "MEDIUM";

        } else {

            riskLevel = "LOW";
        }

        boolean isScam =
                finalScore >= 60;

        if (category == null
                || category.isBlank()) {

            category = "Unclear";
        }

        String recommendation;

        if (isScam) {

            recommendation =
                    "Do not click links, share personal "
                            + "information, or send money. "
                            + "Verify independently through official channels.";

        } else if (finalScore >= 30) {

            recommendation =
                    "Proceed carefully and verify the sender "
                            + "before sharing information or making payments.";

        } else {

            recommendation =
                    "No strong scam indicators were detected. "
                            + "Continue to stay cautious with unexpected messages.";
        }

        MessageAnalysisResult result =
                new MessageAnalysisResult();

        result.setScam(isScam);

        result.setScamCategory(category);

        result.setRiskScore(finalScore);

        result.setRiskLevel(riskLevel);

        result.setRuleIndicators(
                ruleResult.indicators
        );

        result.setLlmExplanation(
                explanation
        );

        result.setRecommendation(
                recommendation
        );

        return result;
    }
}