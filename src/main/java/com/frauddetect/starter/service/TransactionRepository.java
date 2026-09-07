package com.frauddetect.starter.service;

import com.frauddetect.starter.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database operations for TransactionRecord.
 */
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    /**
     * Checks whether a transaction with the given transactionId
     * already exists in the database.
     */
    boolean existsByTransactionId(String transactionId);
}