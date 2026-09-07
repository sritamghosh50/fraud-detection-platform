package com.frauddetect.starter.service;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.MlPrediction;
import com.frauddetect.starter.model.RiskDecision;
import org.springframework.stereotype.Service;

/**
 * This class combines the rules engine result and the ML model result
 * into ONE final decision. This is the "brain" that reviewers and the
 * rest of the system will actually trust.
 */
@Service
public class RiskDecisionEngine {

    // Weighting: ML model counts more (0.7), rules count less (0.3).
    // This is a tunable starting point, not a fixed law.
    private static final double ML_WEIGHT = 0.7;
    private static final double RULES_WEIGHT = 0.3;

    // Anything at or above this combined score gets flagged for human review
    private static final double REVIEW_THRESHOLD = 60.0;

    public RiskDecision decide(String transactionId, FraudCheckResult ruleResult, MlPrediction mlPrediction) {

        double rulesScore = ruleResult.getRiskScore(); // already 0-100
        double mlScore = (mlPrediction != null) ? mlPrediction.getRiskScore() : 0.0; // 0-100

        double finalScore;
        if (mlPrediction != null) {
            // Normal case: combine both using the weights
            finalScore = (mlScore * ML_WEIGHT) + (rulesScore * RULES_WEIGHT);
        } else {
            // If the ML service was unreachable, fall back to rules only
            // rather than silently under-scoring the transaction
            finalScore = rulesScore;
        }

        // A hard rule violation should never be fully diluted by a low ML score -
        // if rules alone are very confident, respect that floor
        if (rulesScore >= 80 && finalScore < rulesScore) {
            finalScore = rulesScore;
        }

        finalScore = Math.min(100, Math.max(0, finalScore)); // keep it in 0-100 range

        String riskLevel;
        if (finalScore >= 85) {
            riskLevel = "CRITICAL";
        } else if (finalScore >= 60) {
            riskLevel = "HIGH";
        } else if (finalScore >= 30) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        boolean flaggedForReview = finalScore >= REVIEW_THRESHOLD;

        return new RiskDecision(
                transactionId,
                Math.round(finalScore * 100.0) / 100.0, // round to 2 decimal places
                riskLevel,
                flaggedForReview,
                mlScore,
                rulesScore,
                ruleResult.getReasons()
        );
    }
}