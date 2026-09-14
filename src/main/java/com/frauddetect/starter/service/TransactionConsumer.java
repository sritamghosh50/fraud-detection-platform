package com.frauddetect.starter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetect.starter.model.Alert;
import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.MlPrediction;
import com.frauddetect.starter.model.RiskDecision;
import com.frauddetect.starter.model.Transaction;
import com.frauddetect.starter.model.TransactionRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TransactionConsumer {

    private final FraudRuleService fraudRuleService;
    private final MlModelClient mlModelClient;
    private final RiskDecisionEngine riskDecisionEngine;
    private final LlmExplainerService llmExplainerService;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionConsumer(
            FraudRuleService fraudRuleService,
            MlModelClient mlModelClient,
            RiskDecisionEngine riskDecisionEngine,
            LlmExplainerService llmExplainerService,
            TransactionRepository transactionRepository,
            AlertRepository alertRepository) {

        this.fraudRuleService = fraudRuleService;
        this.mlModelClient = mlModelClient;
        this.riskDecisionEngine = riskDecisionEngine;
        this.llmExplainerService = llmExplainerService;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    @KafkaListener(
            topics = "transactions",
            groupId = "fraud-detection-group"
    )
    public void consume(String message) {

        try {

            System.out.println("Received from Kafka: " + message);

            Transaction transaction =
                    objectMapper.readValue(
                            message,
                            Transaction.class
                    );

            if (transactionRepository.existsByTransactionId(
                    transaction.getTransactionId())) {

                System.out.println(
                        "Duplicate transaction ignored: "
                                + transaction.getTransactionId()
                );

                return;
            }

            // 1. Java rules
            FraudCheckResult ruleResult =
                    fraudRuleService.check(transaction);

            // 2. Python ML
            MlPrediction mlPrediction =
                    mlModelClient.getPrediction(transaction);

            // 3. Combine rules + ML
            RiskDecision decision =
                    riskDecisionEngine.decide(
                            transaction.getTransactionId(),
                            ruleResult,
                            mlPrediction
                    );

            // 4. LLM explanation
            String explanation;

            if (decision.isFlaggedForReview()) {

                System.out.println(
                        "Generating LLM explanation..."
                );

                explanation =
                        llmExplainerService.generateExplanation(
                                transaction,
                                decision
                        );

            } else {

                explanation =
                        "No explanation needed - transaction is low risk.";
            }

            // 5. Save transaction record
            TransactionRecord record =
                    new TransactionRecord();

            record.setTransactionId(
                    transaction.getTransactionId()
            );

            record.setUserId(
                    transaction.getUserId()
            );

            record.setAmount(
                    transaction.getAmount()
            );

            record.setCountry(
                    transaction.getCountry()
            );

            record.setUserHomeCountry(
                    transaction.getUserHomeCountry()
            );

            record.setHourOfDay(
                    transaction.getHourOfDay()
            );

            record.setFlaggedAsFraud(
                    ruleResult.isFlaggedAsFraud()
            );

            record.setRiskScore(
                    ruleResult.getRiskScore()
            );

            record.setCheckedAt(
                    LocalDateTime.now()
            );

            if (mlPrediction != null) {

                record.setMlRiskScore(
                        mlPrediction.getRiskScore()
                );

                record.setMlFlaggedAsFraud(
                        mlPrediction.isFraud()
                );
            }

            record.setFinalRiskScore(
                    decision.getFinalRiskScore()
            );

            record.setRiskLevel(
                    decision.getRiskLevel()
            );

            record.setFlaggedForReview(
                    decision.isFlaggedForReview()
            );

            record.setLlmExplanation(
                    explanation
            );

            transactionRepository.save(record);

            // 6. Create alert
            if (decision.isFlaggedForReview()) {

                Alert alert = new Alert();

                alert.setTransactionId(
                        transaction.getTransactionId()
                );

                alert.setFinalRiskScore(
                        decision.getFinalRiskScore()
                );

                alert.setRiskLevel(
                        decision.getRiskLevel()
                );

                alert.setLlmExplanation(
                        explanation
                );

                alert.setReviewStatus(
                        "PENDING"
                );

                alert.setCreatedAt(
                        LocalDateTime.now()
                );

                alertRepository.save(alert);

                System.out.println(
                        "Alert created for transaction "
                                + transaction.getTransactionId()
                );
            }

            System.out.println(
                    "Processed transaction "
                            + transaction.getTransactionId()
                            + " | User: "
                            + transaction.getUserId()
                            + " | Level: "
                            + decision.getRiskLevel()
                            + " | Final Score: "
                            + decision.getFinalRiskScore()
                            + " | Needs Review: "
                            + decision.isFlaggedForReview()
            );

            System.out.println(
                    "LLM Explanation: "
                            + explanation
            );

        } catch (Exception e) {

            System.out.println(
                    "Failed to process Kafka message: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
