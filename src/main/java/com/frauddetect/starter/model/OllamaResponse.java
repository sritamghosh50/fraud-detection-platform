package com.frauddetect.starter.model;

/**
 * Matches the JSON shape Ollama's /api/generate endpoint returns.
 * We only care about the "response" field - the generated text.
 * Jackson will ignore the other fields (model, done, context, etc.) automatically.
 */
public class OllamaResponse {

    private String response;

    public OllamaResponse() {
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}