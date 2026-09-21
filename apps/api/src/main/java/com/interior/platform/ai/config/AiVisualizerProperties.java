package com.interior.platform.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "interior.ai")
public class AiVisualizerProperties {

    /**
     * AI Provider key ("none", "stability", "replicate", "vertex", "mock_configured")
     */
    private String provider = "none";

    /**
     * Provider API secret/token. NEVER exposed to frontend.
     */
    private String apiKey = "";

    /**
     * Custom endpoint or base URL if required by provider.
     */
    private String endpoint = "";

    /**
     * Model version or identifier.
     */
    private String model = "interior-diffusion-v1";

    /**
     * Timeout for provider requests in seconds.
     */
    private int timeoutSeconds = 60;

    /**
     * Max retries for retryable provider failures (e.g. transient 5xx, timeout).
     */
    private int maxRetries = 2;

    /**
     * Maximum prompt character length.
     */
    private int maxPromptLength = 500;

    /**
     * Daily generation limit per studio.
     */
    private int dailyStudioLimit = 50;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxPromptLength() {
        return maxPromptLength;
    }

    public void setMaxPromptLength(int maxPromptLength) {
        this.maxPromptLength = maxPromptLength;
    }

    public int getDailyStudioLimit() {
        return dailyStudioLimit;
    }

    public void setDailyStudioLimit(int dailyStudioLimit) {
        this.dailyStudioLimit = dailyStudioLimit;
    }
}
