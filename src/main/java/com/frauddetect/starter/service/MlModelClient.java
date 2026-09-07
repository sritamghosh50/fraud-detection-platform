package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MlPrediction;
import com.frauddetect.starter.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * This class calls our Python ML model server (running on port 8000)
 * and converts a Java Transaction into a real fraud prediction.
 */
@Service
public class MlModelClient {

    private static final String ML_SERVICE_URL = "http://host.docker.internal:8000/predict-realistic";

    private final RestTemplate restTemplate;

    public MlModelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MlPrediction getPrediction(Transaction transaction) {
        // Build the request body to match what the Python endpoint expects:
        // amount, hourOfDay, countryMismatch
        Map<String, Object> request = new HashMap<>();
        request.put("amount", transaction.getAmount());
        request.put("hourOfDay", transaction.getHourOfDay());

        // Convert country/userHomeCountry into the 0/1 flag the model expects
        int countryMismatch = 0;
        if (transaction.getCountry() != null
                && transaction.getUserHomeCountry() != null
                && !transaction.getCountry().equalsIgnoreCase(transaction.getUserHomeCountry())) {
            countryMismatch = 1;
        }
        request.put("countryMismatch", countryMismatch);

        try {
            MlPrediction prediction = restTemplate.postForObject(ML_SERVICE_URL, request, MlPrediction.class);
            return prediction;
        } catch (Exception e) {
            System.out.println("Failed to call ML service: " + e.getMessage());
            return null; // we'll handle this safely wherever we call it
        }
    }
}