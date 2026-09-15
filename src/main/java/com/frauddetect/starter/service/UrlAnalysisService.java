package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MessageAnalysisResult;
import org.springframework.stereotype.Service;

/**
 * Combines rule-based URL structure analysis with LLM analysis.
 *
 * IMPORTANT DESIGN NOTE: small local LLMs tend to be overconfident - they
 * rarely say "I'm 55% sure," they say "I'm 95% sure" or "I'm 5% sure," even
 * when a URL is only mildly suspicious. If we trust that confidence number
 * directly, the final score gets dragged to the extremes almost every time,
 * which is exactly the "only ever 10 or 90" problem this class used to have.
 *
 * The fix: the deterministic rules score (now more finely graduated - see
 * UrlRuleService) does most of the real work of placing a URL somewhere
 * sensible on the 0-100 scale. The LLM's opinion is folded in with a lower
 * weight, AND its raw confidence is "damped" toward the middle before use,
 * so it nudges the result rather than yanking it to an extreme.
 */
@Service
public class UrlAnalysisService {

    private final UrlRuleService urlRuleService;
    private final UrlLlmService urlLlmService;

    // Rules now carry more of the weight, since they're graduated and
    // explainable. The LLM acts as a secondary opinion, not the deciding vote.
    private static final double RULES_WEIGHT = 0.65;
    private static final double LLM_WEIGHT = 0.35;

    // How much we pull the LLM's raw confidence toward the center (50) before
    // using it. 0.0 = no damping (use raw confidence as-is). 1.0 = fully
    // flattened to 50 (LLM has zero effect). 0.5 is a reasonable middle ground.
    private static final double LLM_DAMPING_FACTOR = 0.5;

    public UrlAnalysisService(UrlRuleService urlRuleService, UrlLlmService urlLlmService) {
        this.urlRuleService = urlRuleService;
        this.urlLlmService = urlLlmService;
    }

    public MessageAnalysisResult analyze(String url) {
        MessageRuleService.RuleScanResult ruleResult = urlRuleService.scan(url);
        MessageLlmService.LlmMessageAnalysis llmResult = urlLlmService.analyze(url);

        double rawLlmScore = llmResult.isScam ? llmResult.confidence : (100 - llmResult.confidence);
        double dampedLlmScore = dampenTowardCenter(rawLlmScore, LLM_DAMPING_FACTOR);

        double finalScore = (dampedLlmScore * LLM_WEIGHT) + (ruleResult.score * RULES_WEIGHT);

        // If rules alone found strong indicators, don't let the LLM's opinion
        // pull the score down below what the rules already established.
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
                : (finalScore >= 30
                    ? "This URL shows some mild risk signals. Proceed with caution and double-check the address bar before entering sensitive information."
                    : "This URL shows low risk signals, but always double-check the address bar before entering sensitive information.");

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

    /**
     * Pulls a 0-100 score toward 50 by the given factor, without discarding it
     * entirely. factor=0.5 means a raw score of 95 becomes ~72.5, and a raw
     * score of 5 becomes ~27.5 - still meaningfully different, just not
     * swinging all the way to the extreme on the LLM's word alone.
     */
    private double dampenTowardCenter(double rawScore, double factor) {
        double center = 50.0;
        return center + (rawScore - center) * (1 - factor);
    }
}