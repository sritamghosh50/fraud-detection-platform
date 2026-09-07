package com.frauddetect.starter.service;

import com.frauddetect.starter.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * This class SENDS a transaction into Kafka, onto a "topic" (like a named channel).
 * Think of it as dropping a letter into a mailbox labeled "transactions".
 */
@Service
public class TransactionProducer {

    private static final String TOPIC = "transactions";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(Transaction transaction) {
        try {
            // Convert the Transaction object into a JSON string, since Kafka sends plain text
            String json = objectMapper.writeValueAsString(transaction);
            kafkaTemplate.send(TOPIC, transaction.getTransactionId(), json);
            System.out.println("Sent to Kafka: " + json);
        } catch (Exception e) {
            System.out.println("Failed to send to Kafka: " + e.getMessage());
        }
    }
}