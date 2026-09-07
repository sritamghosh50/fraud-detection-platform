package com.frauddetect.starter.model;

import java.util.List;

/**
 * The final combined result of analyzing a suspicious message.
 */
public class MessageAnalysisResult {

    private boolean isScam;
    private String scamCategory;      // e.g. "Job Scam", "Phishing", "Lottery Scam", "Unclear"
    private int riskScore;            // 0-100
    private String riskLevel;         // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> ruleIndicators; // red flags our own rules found
    private String llmExplanation;    // plain-English reasoning from the LLM
    private String recommendation;    // what the user should do

    public MessageAnalysisResult() {
    }

    public boolean isScam() {
        return isScam;
    }

    public void setScam(boolean scam) {
        isScam = scam;
    }

    public String getScamCategory() {
        return scamCategory;
    }

    public void setScamCategory(String scamCategory) {
        this.scamCategory = scamCategory;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getRuleIndicators() {
        return ruleIndicators;
    }

    public void setRuleIndicators(List<String> ruleIndicators) {
        this.ruleIndicators = ruleIndicators;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}