package com.frauddetect.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the entry point of the whole app.
 * When you run "mvn spring-boot:run", Java starts here first.
 */
@SpringBootApplication
public class FraudDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionApplication.class, args);
        System.out.println("Fraud Detection API is running on http://localhost:8080");
    }
}