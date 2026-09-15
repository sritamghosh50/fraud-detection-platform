package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Combines deterministic URL analysis with optional LLM analysis.
 *
 * If the LLM is unavailable, the real URL rule score is used directly.
 * This prevents every URL from receiving the same fallback score.
 */
@Service
public class UrlAnalysisService {

    private final UrlRuleService urlRuleService;

    private final UrlLlmService urlLlmService;

    private static final double RULES_WEIGHT = 0.65;

    private static final double LLM_WEIGHT = 0.35;

    private static final double LLM_DAMPING_FACTOR = 0.5;

    public UrlAnalysisService(
            UrlRuleService urlRuleService,
            UrlLlmService urlLlmService) {

        this.urlRuleService = urlRuleService;

        this.urlLlmService = urlLlmService;
    }

    public MessageAnalysisResult analyze(
            String url) {

        MessageRuleService.RuleScanResult ruleResult =
                urlRuleService.scan(url);

        MessageLlmService.LlmMessageAnalysis llmResult =
                urlLlmService.analyze(url);

        boolean llmUnavailable =
                llmResult == null
                        || llmResult.llmUnavailable;

        double finalScore;

        String category;

        String explanation;

        /*
         * ---------------------------------------------------------
         * LLM UNAVAILABLE
         * ---------------------------------------------------------
         *
         * Use the actual URL rule score.
         */
        if (llmUnavailable) {

            finalScore =
                    ruleResult.score;

            category =
                    ruleResult.likelyCategory;

            if (category == null
                    || category.isBlank()) {

                category =
                        "Rule-Based URL Analysis";
            }

            explanation =
                    "AI analysis is currently unavailable. "
                            + "The result is based on FraudGuard's "
                            + "URL security rules and structural indicators.";

        } else {

            /*
             * -----------------------------------------------------
             * NORMAL LLM + RULE ANALYSIS
             * -----------------------------------------------------
             */

            double rawLlmScore;

            if (llmResult.isScam) {

                rawLlmScore =
                        llmResult.confidence;

            } else {

                rawLlmScore =
                        100 - llmResult.confidence;
            }

            double dampedLlmScore =
                    dampenTowardCenter(
                            rawLlmScore,
                            LLM_DAMPING_FACTOR
                    );

            finalScore =
                    (dampedLlmScore * LLM_WEIGHT)
                            + (ruleResult.score * RULES_WEIGHT);

            /*
             * Strong deterministic URL signals should not
             * be completely overridden by the LLM.
             */
            if (ruleResult.score >= 50
                    && finalScore < ruleResult.score) {

                finalScore =
                        ruleResult.score;
            }

            category =
                    !"Unclear".equalsIgnoreCase(
                            llmResult.category
                    )
                            ? llmResult.category
                            : ruleResult.likelyCategory;

            explanation =
                    llmResult.explanation;
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
         * CATEGORY
         * ---------------------------------------------------------
         */

        if (category == null
                || category.isBlank()) {

            category =
                    "Unclear URL";
        }

        /*
         * ---------------------------------------------------------
         * RECOMMENDATION
         * ---------------------------------------------------------
         */

        String recommendation;

        if (isScam) {

            recommendation =
                    "Do not click this link or enter any personal "
                            + "information. Verify the website's "
                            + "official address independently.";

        } else if (roundedScore >= 30) {

            recommendation =
                    "This URL shows some suspicious signals. "
                            + "Proceed with caution and double-check "
                            + "the address before entering sensitive information.";

        } else {

            recommendation =
                    "This URL shows low risk signals, but always "
                            + "double-check the address bar before "
                            + "entering sensitive information.";
        }

        /*
         * ---------------------------------------------------------
         * RESULT
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

    /**
     * Pulls an LLM score toward 50.
     *
     * Example:
     *
     * 95 -> approximately 72.5
     * 5  -> approximately 27.5
     */
    private double dampenTowardCenter(
            double rawScore,
            double factor) {

        double center = 50.0;

        return center
                + (rawScore - center)
                * (1 - factor);
    }
}