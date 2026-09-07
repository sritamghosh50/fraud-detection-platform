package com.frauddetect.starter.service;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds our fraud rules. It's simple on purpose -
 * this is Day 1. Later we'll add a real ML model here too.
 *
 * Each rule adds "risk points" if it's suspicious. If the total
 * is high enough, we flag the transaction.
 */
@Service
public class FraudRuleService {

    private static final int FLAG_THRESHOLD = 50; // if riskScore >= 50, we flag it

    private final VelocityCheckService velocityCheckService;

    @Autowired
    public FraudRuleService(VelocityCheckService velocityCheckService) {
        this.velocityCheckService = velocityCheckService;
    }

    public FraudCheckResult check(Transaction transaction) {
        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        // Rule 1: very large amount is suspicious
        if (transaction.getAmount() > 100000) {
            riskScore += 40;
            reasons.add("Transaction amount is unusually large (over 100,000)");
        } else if (transaction.getAmount() > 50000) {
            riskScore += 20;
            reasons.add("Transaction amount is higher than typical (over 50,000)");
        }

        // Rule 2: transaction happening in a different country than usual
        if (transaction.getCountry() != null
                && transaction.getUserHomeCountry() != null
                && !transaction.getCountry().equalsIgnoreCase(transaction.getUserHomeCountry())) {

            riskScore += 30;

            reasons.add("Transaction country (" + transaction.getCountry()
                    + ") is different from user's home country ("
                    + transaction.getUserHomeCountry() + ")");
        }

        // Rule 3: transaction happening late at night (odd hours are more suspicious)
        if (transaction.getHourOfDay() >= 1 && transaction.getHourOfDay() <= 4) {
            riskScore += 15;
            reasons.add("Transaction happened between 1 AM and 4 AM, an unusual time");
        }

        // Rule 4: too many transactions from the same user too quickly
        // Redis is used to track the transaction velocity.
        if (transaction.getUserId() != null) {

            boolean tooFast =
                    velocityCheckService.isVelocityExceeded(
                            transaction.getUserId()
                    );

            if (tooFast) {
                riskScore += 35;

                reasons.add(
                        "User has made more than 3 transactions within the last minute (velocity check)"
                );
            }
        }

        boolean flagged = riskScore >= FLAG_THRESHOLD;

        if (reasons.isEmpty()) {
            reasons.add("No suspicious patterns found");
        }

        return new FraudCheckResult(
                transaction.getTransactionId(),
                flagged,
                Math.min(riskScore, 100), // cap the score at 100
                reasons
        );
    }
}