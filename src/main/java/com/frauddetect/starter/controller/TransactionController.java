package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.Transaction;
import com.frauddetect.starter.model.TransactionRecord;
import com.frauddetect.starter.service.FraudRuleService;
import com.frauddetect.starter.service.TransactionProducer;
import com.frauddetect.starter.service.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * This is the "front door" of our app.
 * - POST /check   -> old way: checks immediately and returns the result (still works)
 * - POST /submit  -> new way: sends to Kafka, gets processed automatically in the background
 * - GET  /history -> see everything that's been checked and saved
 */
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final FraudRuleService fraudRuleService;
    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;

    public TransactionController(FraudRuleService fraudRuleService,
                                  TransactionRepository transactionRepository,
                                  TransactionProducer transactionProducer) {
        this.fraudRuleService = fraudRuleService;
        this.transactionRepository = transactionRepository;
        this.transactionProducer = transactionProducer;
    }

    @PostMapping("/check")
    public FraudCheckResult checkTransaction(@RequestBody Transaction transaction) {
        FraudCheckResult result = fraudRuleService.check(transaction);

        TransactionRecord record = new TransactionRecord();
        record.setTransactionId(transaction.getTransactionId());
        record.setUserId(transaction.getUserId());
        record.setAmount(transaction.getAmount());
        record.setCountry(transaction.getCountry());
        record.setUserHomeCountry(transaction.getUserHomeCountry());
        record.setHourOfDay(transaction.getHourOfDay());
        record.setFlaggedAsFraud(result.isFlaggedAsFraud());
        record.setRiskScore(result.getRiskScore());
        record.setCheckedAt(LocalDateTime.now());
        transactionRepository.save(record);

        return result;
    }

    @PostMapping("/submit")
    public Map<String, String> submitTransaction(@RequestBody Transaction transaction) {
        transactionProducer.sendTransaction(transaction);
        return Map.of(
                "status", "submitted",
                "transactionId", transaction.getTransactionId(),
                "note", "Sent to Kafka. Check /api/transactions/history in a moment to see the result."
        );
    }

    @GetMapping("/history")
    public List<TransactionRecord> getHistory() {
        return transactionRepository.findAll();
    }
}