package com.frauddetect.starter.model;

import java.util.List;

/**
 * This is the FINAL combined verdict - it merges the rules engine result
 * and the ML model result into one decision, with a risk level a human
 * reviewer can act on quickly.
 */
public class RiskDecision {

    private String transactionId;
    private double finalRiskScore;   // 0-100, combined score
    private String riskLevel;        // LOW, MEDIUM, HIGH, CRITICAL
    private boolean flaggedForReview;
    private double mlRiskScore;
    private double rulesRiskScore;
    private List<String> rulesReasons;

    public RiskDecision() {
    }

    public RiskDecision(String transactionId, double finalRiskScore, String riskLevel,
                         boolean flaggedForReview, double mlRiskScore, double rulesRiskScore,
                         List<String> rulesReasons) {
        this.transactionId = transactionId;
        this.finalRiskScore = finalRiskScore;
        this.riskLevel = riskLevel;
        this.flaggedForReview = flaggedForReview;
        this.mlRiskScore = mlRiskScore;
        this.rulesRiskScore = rulesRiskScore;
        this.rulesReasons = rulesReasons;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getFinalRiskScore() {
        return finalRiskScore;
    }

    public void setFinalRiskScore(double finalRiskScore) {
        this.finalRiskScore = finalRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isFlaggedForReview() {
        return flaggedForReview;
    }

    public void setFlaggedForReview(boolean flaggedForReview) {
        this.flaggedForReview = flaggedForReview;
    }

    public double getMlRiskScore() {
        return mlRiskScore;
    }

    public void setMlRiskScore(double mlRiskScore) {
        this.mlRiskScore = mlRiskScore;
    }

    public double getRulesRiskScore() {
        return rulesRiskScore;
    }

    public void setRulesRiskScore(double rulesRiskScore) {
        this.rulesRiskScore = rulesRiskScore;
    }

    public List<String> getRulesReasons() {
        return rulesReasons;
    }

    public void setRulesReasons(List<String> rulesReasons) {
        this.rulesReasons = rulesReasons;
    }
}