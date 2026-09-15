package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class MessageAnalysisService {

    private final MessageRuleService messageRuleService;

    public MessageAnalysisService(
            MessageRuleService messageRuleService) {

        this.messageRuleService =
                messageRuleService;
    }

    public MessageAnalysisResult analyze(
            String messageText) {

        MessageRuleService.RuleScanResult scan =
                messageRuleService.scan(
                        messageText
                );

        return buildResult(
                scan
        );
    }

    /*
     * Image OCR also uses the same fast rule engine.
     */
    public MessageAnalysisResult analyzeImageText(
            String extractedText) {

        MessageRuleService.RuleScanResult scan =
                messageRuleService.scan(
                        extractedText
                );

        return buildResult(
                scan
        );
    }

    private MessageAnalysisResult buildResult(
            MessageRuleService.RuleScanResult scan) {

        int score =
                Math.min(
                        100,
                        Math.max(
                                0,
                                scan.score
                        )
                );

        String riskLevel;

        if (score >= 85) {

            riskLevel = "CRITICAL";

        } else if (score >= 60) {

            riskLevel = "HIGH";

        } else if (score >= 30) {

            riskLevel = "MEDIUM";

        } else {

            riskLevel = "LOW";
        }

        boolean scam =
                score >= 60;

        String explanation;

        if (scan.indicators.isEmpty()) {

            explanation =
                    "No strong scam indicators were detected "
                            + "by FraudGuard's message security rules.";

        } else {

            explanation =
                    "FraudGuard detected " +
                            scan.indicators.size() +
                            " suspicious indicator(s) "
                            + "using its message security rules.";
        }

        String recommendation;

        if (score >= 60) {

            recommendation =
                    "Do not click links, share OTPs or passwords, "
                            + "or send money. Verify the sender independently.";

        } else if (score >= 30) {

            recommendation =
                    "Be careful with this message and verify "
                            + "the sender before taking action.";

        } else {

            recommendation =
                    "No strong scam indicators were detected. "
                            + "Continue to stay cautious online.";
        }

        MessageAnalysisResult result =
                new MessageAnalysisResult();

        result.setScam(
                scam
        );

        result.setScamCategory(
                scan.likelyCategory
        );

        result.setRiskScore(
                score
        );

        result.setRiskLevel(
                riskLevel
        );

        result.setRuleIndicators(
                scan.indicators
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