package com.frauddetect.starter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaWarmupService {

    @Value("${ollama.url}")
    private String ollamaUrl;

    private static final String MODEL_NAME = "llama3.2";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaWarmupService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOllama() {

        Thread warmupThread = new Thread(() -> {

            try {

                System.out.println("========================================");
                System.out.println("FraudGuard Ollama warm-up started...");
                System.out.println("Loading model: " + MODEL_NAME);
                System.out.println("========================================");

                Map<String, Object> request = new HashMap<>();

                request.put("model", MODEL_NAME);
                request.put(
                        "prompt",
                        "Reply with only the word READY."
                );
                request.put("stream", false);
                request.put("keep_alive", "30m");

                ResponseEntity<String> response =
                        restTemplate.postForEntity(
                                ollamaUrl,
                                request,
                                String.class
                        );

                if (response.getStatusCode().is2xxSuccessful()) {

                    System.out.println("========================================");
                    System.out.println("Ollama model is warmed up successfully.");
                    System.out.println("FraudGuard is ready for fast AI analysis.");
                    System.out.println("========================================");

                } else {

                    System.out.println(
                            "Ollama warm-up returned HTTP status: "
                                    + response.getStatusCode()
                    );
                }

            } catch (Exception e) {

                System.out.println("========================================");
                System.out.println(
                        "Ollama warm-up failed: "
                                + e.getMessage()
                );
                System.out.println(
                        "FraudGuard will continue normally."
                );
                System.out.println("========================================");
            }

        });

        warmupThread.setName("ollama-warmup-thread");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }
}