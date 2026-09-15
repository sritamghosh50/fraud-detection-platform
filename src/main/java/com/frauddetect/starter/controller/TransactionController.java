package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.Transaction;
import com.frauddetect.starter.model.TransactionRecord;
import com.frauddetect.starter.service.CurrentUserService;
import com.frauddetect.starter.service.FraudRuleService;
import com.frauddetect.starter.service.TransactionProducer;
import com.frauddetect.starter.service.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class TransactionController {

    private final FraudRuleService fraudRuleService;
    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;
    private final CurrentUserService currentUserService;

    public TransactionController(
            FraudRuleService fraudRuleService,
            TransactionRepository transactionRepository,
            TransactionProducer transactionProducer,
            CurrentUserService currentUserService) {

        this.fraudRuleService = fraudRuleService;
        this.transactionRepository = transactionRepository;
        this.transactionProducer = transactionProducer;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/check")
    public FraudCheckResult checkTransaction(
            @RequestBody Transaction transaction) {

        String loggedInUser =
                currentUserService.getCurrentUserEmail();

        FraudCheckResult result =
                fraudRuleService.check(transaction);

        TransactionRecord record =
                new TransactionRecord();

        record.setTransactionId(
                transaction.getTransactionId()
        );

        // Authenticated user controls the ownership.
        record.setUserId(loggedInUser);

        // Store the authenticated user's email as owner.
        record.setOwnerEmail(loggedInUser);

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
                result.isFlaggedAsFraud()
        );

        record.setRiskScore(
                result.getRiskScore()
        );

        record.setCheckedAt(
                LocalDateTime.now()
        );

        transactionRepository.save(record);

        return result;
    }

    @PostMapping("/submit")
    public Map<String, String> submitTransaction(
            @RequestBody Transaction transaction) {

        String loggedInUser =
                currentUserService.getCurrentUserEmail();

        // Never trust userId supplied by the frontend.
        transaction.setUserId(loggedInUser);

        // Store authenticated account ownership.
        transaction.setOwnerEmail(loggedInUser);

        transactionProducer.sendTransaction(transaction);

        return Map.of(
                "status",
                "submitted",
                "transactionId",
                transaction.getTransactionId(),
                "userId",
                loggedInUser,
                "note",
                "Sent to Kafka. Check /api/transactions/history in a moment to see the result."
        );
    }

    @GetMapping("/history")
    public List<TransactionRecord> getHistory() {

        String loggedInUser =
                currentUserService.getCurrentUserEmail();

        // Only return transactions belonging to the
        // currently authenticated account.
        return transactionRepository.findByOwnerEmail(
                loggedInUser
        );
    }
}