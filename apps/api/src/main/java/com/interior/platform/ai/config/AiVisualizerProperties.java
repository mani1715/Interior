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
     * Global kill switch: when false, rejects all new generation requests immediately.
     */
    private boolean enabled = true;

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
     * Daily generation limit per studio (default: 20 generations per studio per day).
     */
    private int dailyStudioLimit = 20;

    /**
     * Global daily spend budget cap in minor units (e.g. 50000 = $500.00). 0 means unconstrained.
     */
    private long globalDailyBudgetMinor = 0L;

    /**
     * Default per-studio monthly spend budget cap in minor units (0 = unconstrained).
     */
    private long defaultStudioMonthlyBudgetMinor = 0L;

    /**
     * Base estimated cost per generation in minor units (default: 4 = $0.04).
     */
    private long estimatedCostPerGenerationMinor = 4L;

    /**
     * Pricing currency code.
     */
    private String pricingCurrency = "USD";

    /**
     * Pricing table version tag.
     */
    private String pricingVersion = "v1-default";

    /**
     * Allowed models allowlist.
     */
    private java.util.List<String> allowedModels = java.util.List.of("interior-diffusion-v1", "gemini-imagen-3", "dall-e-3", "mock-model");

    /**
     * Whether the active provider supports multi-modal reference image conditioning.
     */
    private boolean supportsReferenceImages = true;

    /**
     * Maximum number of reference images allowed per generation job.
     */
    private int maxReferenceImages = 4;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getGlobalDailyBudgetMinor() {
        return globalDailyBudgetMinor;
    }

    public void setGlobalDailyBudgetMinor(long globalDailyBudgetMinor) {
        this.globalDailyBudgetMinor = globalDailyBudgetMinor;
    }

    public long getDefaultStudioMonthlyBudgetMinor() {
        return defaultStudioMonthlyBudgetMinor;
    }

    public void setDefaultStudioMonthlyBudgetMinor(long defaultStudioMonthlyBudgetMinor) {
        this.defaultStudioMonthlyBudgetMinor = defaultStudioMonthlyBudgetMinor;
    }

    public long getEstimatedCostPerGenerationMinor() {
        return estimatedCostPerGenerationMinor;
    }

    public void setEstimatedCostPerGenerationMinor(long estimatedCostPerGenerationMinor) {
        this.estimatedCostPerGenerationMinor = estimatedCostPerGenerationMinor;
    }

    public String getPricingCurrency() {
        return pricingCurrency;
    }

    public void setPricingCurrency(String pricingCurrency) {
        this.pricingCurrency = pricingCurrency;
    }

    public String getPricingVersion() {
        return pricingVersion;
    }

    public void setPricingVersion(String pricingVersion) {
        this.pricingVersion = pricingVersion;
    }

    public java.util.List<String> getAllowedModels() {
        return allowedModels;
    }

    public void setAllowedModels(java.util.List<String> allowedModels) {
        this.allowedModels = allowedModels;
    }

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

    public boolean isSupportsReferenceImages() {
        return supportsReferenceImages;
    }

    public void setSupportsReferenceImages(boolean supportsReferenceImages) {
        this.supportsReferenceImages = supportsReferenceImages;
    }

    /**
     * Whether the active provider supports precision mask editing / inpainting.
     */
    private boolean supportsMaskEditing = true;

    public int getMaxReferenceImages() {
        return maxReferenceImages;
    }

    public void setMaxReferenceImages(int maxReferenceImages) {
        this.maxReferenceImages = maxReferenceImages;
    }

    public boolean isSupportsMaskEditing() {
        return supportsMaskEditing;
    }

    public void setSupportsMaskEditing(boolean supportsMaskEditing) {
        this.supportsMaskEditing = supportsMaskEditing;
    }
}
