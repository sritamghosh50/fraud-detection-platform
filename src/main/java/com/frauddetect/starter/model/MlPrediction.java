package com.frauddetect.starter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class matches the JSON shape returned by our Python model server.
 *
 * Python returns:
 * {
 *   "isFraud": true,
 *   "riskScore": 99.99,
 *   "fraudProbability": 0.9999
 * }
 */
public class MlPrediction {

    @JsonProperty("isFraud")
    private boolean isFraud;

    private double riskScore;

    private double fraudProbability;

    public MlPrediction() {
    }

    // ============================================================
    // IS FRAUD
    // ============================================================

    public boolean isFraud() {
        return isFraud;
    }

    public void setFraud(boolean fraud) {
        isFraud = fraud;
    }

    // ============================================================
    // RISK SCORE
    // ============================================================

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    // ============================================================
    // FRAUD PROBABILITY
    // ============================================================

    public double getFraudProbability() {
        return fraudProbability;
    }

    public void setFraudProbability(double fraudProbability) {
        this.fraudProbability = fraudProbability;
    }
}