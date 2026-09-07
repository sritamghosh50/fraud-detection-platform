package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Combines the rule-based scan and the LLM analysis into one final,
 * explainable verdict - same philosophy as RiskDecisionEngine, applied
 * to messages instead of transactions.
 */
@Service
public class MessageAnalysisService {

    private final MessageRuleService messageRuleService;
    private final MessageLlmService messageLlmService;

    // Same weighting philosophy as RiskDecisionEngine: LLM carries more
    // weight since it understands context, but rules act as a floor.
    private static final double LLM_WEIGHT = 0.6;
    private static final double RULES_WEIGHT = 0.4;

    public MessageAnalysisService(MessageRuleService messageRuleService, MessageLlmService messageLlmService) {
        this.messageRuleService = messageRuleService;
        this.messageLlmService = messageLlmService;
    }

    public MessageAnalysisResult analyze(String messageText) {
        MessageRuleService.RuleScanResult ruleResult = messageRuleService.scan(messageText);
        MessageLlmService.LlmMessageAnalysis llmResult = messageLlmService.analyze(messageText);

        double llmScore = llmResult.isScam ? llmResult.confidence : (100 - llmResult.confidence);
        // If LLM says isScam=true, confidence IS the risk score.
        // If LLM says isScam=false, confidence is confidence in safety, so risk = 100 - confidence.
        // This keeps both cases pointing toward "higher number = more risky".

        double finalScore = (llmScore * LLM_WEIGHT) + (ruleResult.score * RULES_WEIGHT);

        // If rules alone found strong indicators, don't let a disagreeing LLM fully override it
        if (ruleResult.score >= 70 && finalScore < ruleResult.score) {
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
                ? "Do not click any links, share personal information, or send money. Verify independently through official channels before taking any action."
                : "This message shows low risk signals, but always stay cautious with unexpected messages asking for money or personal information.";

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