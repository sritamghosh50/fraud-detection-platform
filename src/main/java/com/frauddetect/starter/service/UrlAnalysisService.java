package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Combines rule-based URL structure analysis with LLM analysis -
 * same combination philosophy as MessageAnalysisService.
 */
@Service
public class UrlAnalysisService {

    private final UrlRuleService urlRuleService;
    private final UrlLlmService urlLlmService;

    private static final double LLM_WEIGHT = 0.5;
    private static final double RULES_WEIGHT = 0.5;
    // Equal weighting here, unlike messages - URL structure rules are
    // quite reliable on their own, so we trust them as much as the LLM.

    public UrlAnalysisService(UrlRuleService urlRuleService, UrlLlmService urlLlmService) {
        this.urlRuleService = urlRuleService;
        this.urlLlmService = urlLlmService;
    }

    public MessageAnalysisResult analyze(String url) {
        MessageRuleService.RuleScanResult ruleResult = urlRuleService.scan(url);
        MessageLlmService.LlmMessageAnalysis llmResult = urlLlmService.analyze(url);

        double llmScore = llmResult.isScam ? llmResult.confidence : (100 - llmResult.confidence);
        double finalScore = (llmScore * LLM_WEIGHT) + (ruleResult.score * RULES_WEIGHT);

               if (ruleResult.score >= 50 && finalScore < ruleResult.score) {
            finalScore = ruleResult.score;
        }

        finalScore = Math.min(100, Math.max(0, finalScore));

        String riskLevel;
        if (finalScore >= 85) riskLevel = "CRITICAL";
        else if (finalScore >= 60) riskLevel = "HIGH";
        else if (finalScore >= 30) riskLevel = "MEDIUM";
        else riskLevel = "LOW";

        boolean isScam = finalScore >= 60;

        String category = !"Unclear".equals(llmResult.category) ? llmResult.category : ruleResult.likelyCategory;

        String recommendation = isScam
                ? "Do not click this link or enter any personal information. Verify the website's official address independently."
                : "This URL shows low risk signals, but always double-check the address bar before entering sensitive information.";

        MessageAnalysisResult result = new MessageAnalysisResult();
        result.setScam(isScam);
        result.setScamCategory(category);
        result.setRiskScore((int) Math.round(finalScore));
        result.setRiskLevel(riskLevel);
        result.setRuleIndicators(ruleResult.indicators);
        result.setLlmExplanation(llmResult.explanation);
        result.setRecommendation(recommendation);

        return result;
    }
}