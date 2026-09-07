package com.frauddetect.starter.model;

import java.util.List;

/**
 * This is what we send back to whoever asked us to check a transaction.
 */
public class FraudCheckResult {

    private String transactionId;
    private boolean flaggedAsFraud;
    private int riskScore;          // 0 to 100, higher = more suspicious
    private List<String> reasons;   // plain-English reasons why it was flagged

    public FraudCheckResult() {
    }

    public FraudCheckResult(String transactionId, boolean flaggedAsFraud, int riskScore, List<String> reasons) {
        this.transactionId = transactionId;
        this.flaggedAsFraud = flaggedAsFraud;
        this.riskScore = riskScore;
        this.reasons = reasons;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isFlaggedAsFraud() {
        return flaggedAsFraud;
    }

    public void setFlaggedAsFraud(boolean flaggedAsFraud) {
        this.flaggedAsFraud = flaggedAsFraud;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}