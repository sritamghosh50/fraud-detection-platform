package com.frauddetect.starter.service;

import com.frauddetect.starter.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database operations for TransactionRecord.
 */
public interface TransactionRepository
        extends JpaRepository<TransactionRecord, Long> {

    /**
     * Checks whether a transaction with the given transactionId
     * already exists in the database.
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Returns transactions belonging to the specified user.
     */
    List<TransactionRecord> findByUserId(String userId);

    /**
     * Returns only transactions owned by the specified
     * authenticated account.
     */
    List<TransactionRecord> findByOwnerEmail(String ownerEmail);
}