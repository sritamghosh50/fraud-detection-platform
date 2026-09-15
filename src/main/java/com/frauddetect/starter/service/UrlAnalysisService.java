package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class UrlAnalysisService {

    private final UrlRuleService urlRuleService;

    public UrlAnalysisService(
            UrlRuleService urlRuleService) {

        this.urlRuleService =
                urlRuleService;
    }

    public MessageAnalysisResult analyze(
            String url) {

        MessageRuleService.RuleScanResult scan =
                urlRuleService.scan(url);

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
                    "No strong suspicious URL indicators were detected.";

        } else {

            explanation =
                    "FraudGuard checked the URL structure, "
                            + "domain, protocol, keywords, and other "
                            + "security indicators.";
        }

        String recommendation;

        if (score >= 60) {

            recommendation =
                    "Do not open this link or enter sensitive "
                            + "information. Verify the official website address.";

        } else if (score >= 30) {

            recommendation =
                    "Proceed carefully and verify the website "
                            + "before entering personal information.";

        } else {

            recommendation =
                    "No strong URL red flags were detected. "
                            + "Still verify the address before entering sensitive information.";
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