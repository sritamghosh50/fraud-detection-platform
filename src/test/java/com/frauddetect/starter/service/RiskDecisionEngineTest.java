package com.frauddetect.starter.service;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.MlPrediction;
import com.frauddetect.starter.model.RiskDecision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskDecisionEngineTest {

    private final RiskDecisionEngine engine = new RiskDecisionEngine();

    @Test
    void bothLowShouldResultInLowRisk() {
        FraudCheckResult ruleResult = new FraudCheckResult("TEST001", false, 0, List.of("No suspicious patterns found"));
        MlPrediction mlPrediction = new MlPrediction();
        mlPrediction.setRiskScore(0.05);
        mlPrediction.setFraud(false);

        RiskDecision decision = engine.decide("TEST001", ruleResult, mlPrediction);

        assertEquals("LOW", decision.getRiskLevel());
        assertTrue(decision.getFinalRiskScore() < 30);
    }

    @Test
    void bothHighShouldResultInCritical() {
        FraudCheckResult ruleResult = new FraudCheckResult("TEST002", true, 85, List.of("Large amount", "Foreign country", "Odd hour"));
        MlPrediction mlPrediction = new MlPrediction();
        mlPrediction.setRiskScore(99.99);
        mlPrediction.setFraud(true);

        RiskDecision decision = engine.decide("TEST002", ruleResult, mlPrediction);

        assertEquals("CRITICAL", decision.getRiskLevel());
        assertTrue(decision.isFlaggedForReview());
    }

    @Test
    void mlServiceUnavailableShouldFallBackToRulesOnly() {
        // This is the important "what if a dependency is down" case
        FraudCheckResult ruleResult = new FraudCheckResult("TEST003", true, 65, List.of("Foreign country", "Odd hour"));

        RiskDecision decision = engine.decide("TEST003", ruleResult, null); // ML prediction is null - service was down

        assertEquals(65.0, decision.getFinalRiskScore(), "Should fall back to the rules score exactly when ML is unavailable");
        assertTrue(decision.isFlaggedForReview());
    }

    @Test
    void strongRulesSignalShouldNotBeDilutedByLowMlScore() {
        // This tests the safety floor we specifically designed in Step 66
        FraudCheckResult ruleResult = new FraudCheckResult("TEST004", true, 85, List.of("Very large amount", "Foreign country", "Odd hour"));
        MlPrediction mlPrediction = new MlPrediction();
        mlPrediction.setRiskScore(5.0); // ML disagrees strongly and sees nothing wrong
        mlPrediction.setFraud(false);

        RiskDecision decision = engine.decide("TEST004", ruleResult, mlPrediction);

        assertTrue(decision.getFinalRiskScore() >= 85, "A confident rules score of 85+ should not be diluted below itself by a disagreeing ML score");
    }
}