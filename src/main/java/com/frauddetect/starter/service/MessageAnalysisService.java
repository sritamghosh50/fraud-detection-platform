package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines rule-based analysis and optional LLM analysis.
 *
 * If the LLM is available:
 *      Rule score + LLM score are combined.
 *
 * If the LLM is unavailable:
 *      We use the real rule-based score only.
 *
 * This prevents an unavailable LLM from incorrectly producing
 * a fixed risk score such as 60 for every message.
 */
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

    public MessageAnalysisResult analyze(String messageText) {

        MessageRuleService.RuleScanResult ruleResult =
                messageRuleService.scan(messageText);

        MessageLlmService.LlmMessageAnalysis llmResult =
                messageLlmService.analyze(messageText);

        /*
         * ---------------------------------------------------------
         * LLM UNAVAILABLE
         * ---------------------------------------------------------
         *
         * Do NOT convert confidence=0 into 100% fraud risk.
         *
         * In production, Ollama may not be available.
         * In that situation the deterministic rules are still valid.
         */
        boolean llmUnavailable =
                llmResult == null
                        || llmResult.llmUnavailable;

        double finalScore;

        String category;

        String explanation;

        if (llmUnavailable) {

            finalScore = ruleResult.score;

            category = ruleResult.likelyCategory;

            if (category == null || category.isBlank()) {
                category = "Rule-Based Analysis";
            }

            explanation =
                    "AI analysis is currently unavailable. "
                            + "The result is based on FraudGuard's structural "
                            + "and keyword-based security rules.";

        } else {

            /*
             * -----------------------------------------------------
             * NORMAL LLM + RULE ANALYSIS
             * -----------------------------------------------------
             */

            double llmScore;

            if (llmResult.isScam) {

                llmScore = llmResult.confidence;

            } else {

                llmScore = 100 - llmResult.confidence;
            }

            finalScore =
                    (llmScore * LLM_WEIGHT)
                            + (ruleResult.score * RULES_WEIGHT);

            /*
             * If rules found strong indicators, don't allow
             * the LLM to completely override them.
             */
            if (ruleResult.score >= 70
                    && finalScore < ruleResult.score) {

                finalScore = ruleResult.score;
            }

            category =
                    !"Unclear".equalsIgnoreCase(
                            llmResult.category
                    )
                            ? llmResult.category
                            : ruleResult.likelyCategory;

            explanation = llmResult.explanation;
        }

        /*
         * ---------------------------------------------------------
         * FINAL SCORE
         * ---------------------------------------------------------
         */

        finalScore =
                Math.min(
                        100,
                        Math.max(
                                0,
                                finalScore
                        )
                );

        int roundedScore =
                (int) Math.round(finalScore);

        /*
         * ---------------------------------------------------------
         * RISK LEVEL
         * ---------------------------------------------------------
         */

        String riskLevel;

        if (roundedScore >= 85) {

            riskLevel = "CRITICAL";

        } else if (roundedScore >= 60) {

            riskLevel = "HIGH";

        } else if (roundedScore >= 30) {

            riskLevel = "MEDIUM";

        } else {

            riskLevel = "LOW";
        }

        boolean isScam =
                roundedScore >= 60;

        /*
         * ---------------------------------------------------------
         * DEFAULT CATEGORY
         * ---------------------------------------------------------
         */

        if (category == null || category.isBlank()) {

            category = "Unclear";
        }

        /*
         * ---------------------------------------------------------
         * RECOMMENDATION
         * ---------------------------------------------------------
         */

        String recommendation;

        if (isScam) {

            recommendation =
                    "Do not click any links, share personal information, "
                            + "or send money. Verify independently through "
                            + "official channels before taking any action.";

        } else if (roundedScore >= 30) {

            recommendation =
                    "This message shows some suspicious signals. "
                            + "Be cautious and verify the sender before "
                            + "sharing information or making payments.";

        } else {

            recommendation =
                    "This message shows low risk signals, but always "
                            + "stay cautious with unexpected messages "
                            + "asking for money or personal information.";
        }

        /*
         * ---------------------------------------------------------
         * BUILD RESULT
         * ---------------------------------------------------------
         */

        MessageAnalysisResult result =
                new MessageAnalysisResult();

        result.setScam(isScam);

        result.setScamCategory(category);

        result.setRiskScore(roundedScore);

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