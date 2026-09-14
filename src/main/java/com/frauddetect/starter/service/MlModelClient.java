package com.frauddetect.starter.service;

import com.frauddetect.starter.model.MlPrediction;
import com.frauddetect.starter.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls the FraudGuard Python ML service on Render
 * and converts a Java Transaction into a fraud prediction.
 */
@Service
public class MlModelClient {

    private static final String ML_SERVICE_URL =
            "https://fraudguard-ml-rksa.onrender.com/predict-realistic";

    private final RestTemplate restTemplate;

    public MlModelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MlPrediction getPrediction(Transaction transaction) {

        Map<String, Object> request = new HashMap<>();

        request.put("amount", transaction.getAmount());
        request.put("hourOfDay", transaction.getHourOfDay());

        int countryMismatch = 0;

        if (transaction.getCountry() != null
                && transaction.getUserHomeCountry() != null
                && !transaction.getCountry()
                        .equalsIgnoreCase(transaction.getUserHomeCountry())) {

            countryMismatch = 1;
        }

        request.put("countryMismatch", countryMismatch);

        try {

            MlPrediction prediction = restTemplate.postForObject(
                    ML_SERVICE_URL,
                    request,
                    MlPrediction.class
            );

            return prediction;

        } catch (Exception e) {

            System.out.println(
                    "Failed to call ML service: " + e.getMessage()
            );

            return null;
        }
    }
}
