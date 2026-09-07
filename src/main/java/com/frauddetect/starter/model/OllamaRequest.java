package com.frauddetect.starter.model;

/**
 * Matches the JSON shape Ollama's /api/generate endpoint expects.
 */
public class OllamaRequest {

    private String model;
    private String prompt;
    private boolean stream;
    private OllamaOptions options;

    public OllamaRequest() {
    }

    public OllamaRequest(String model, String prompt, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        // Lower temperature = less randomness = more consistent scores
        // across repeated calls on the same input.
        this.options = new OllamaOptions(0.1, 42);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public OllamaOptions getOptions() {
        return options;
    }

    public void setOptions(OllamaOptions options) {
        this.options = options;
    }

    /**
     * Extra generation settings. "temperature" controls randomness
     * (0 = most consistent, 1 = most varied). "seed" makes the random
     * number generator start from a fixed point, improving repeatability.
     */
    public static class OllamaOptions {
        private double temperature;
        private int seed;

        public OllamaOptions() {
        }

        public OllamaOptions(double temperature, int seed) {
            this.temperature = temperature;
            this.seed = seed;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getSeed() {
            return seed;
        }

        public void setSeed(int seed) {
            this.seed = seed;
        }
    }
}