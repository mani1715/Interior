package com.interior.platform.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    /**
     * Active transactional email provider: disabled, fake, resend, ses.
     * Defaults to "disabled" to prevent accidental external sending.
     */
    private String provider = "disabled";

    /**
     * Sender email address (e.g. notifications@example.com).
     */
    private String fromAddress = "";

    /**
     * Human-readable sender display name (e.g. "Elégance").
     */
    private String fromDisplayName = "Elégance";

    /**
     * Optional global Reply-To address.
     */
    private String replyTo = "";

    /**
     * Maximum retry attempts on transient delivery failures.
     */
    private int maxRetries = 3;

    /**
     * Base backoff delay in seconds for exponential retry calculation.
     */
    private int baseRetryDelaySeconds = 60;

    /**
     * Socket connect and HTTP request timeout in seconds.
     */
    private int timeoutSeconds = 15;

    /**
     * Secret key for validating incoming webhook signatures.
     */
    private String webhookSecret = "";

    private ResendProperties resend = new ResendProperties();
    private SesProperties ses = new SesProperties();

    public String getProvider() {
        return provider != null ? provider.trim().toLowerCase() : "disabled";
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromDisplayName() {
        return fromDisplayName;
    }

    public void setFromDisplayName(String fromDisplayName) {
        this.fromDisplayName = fromDisplayName;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBaseRetryDelaySeconds() {
        return baseRetryDelaySeconds;
    }

    public void setBaseRetryDelaySeconds(int baseRetryDelaySeconds) {
        this.baseRetryDelaySeconds = baseRetryDelaySeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public ResendProperties getResend() {
        return resend;
    }

    public void setResend(ResendProperties resend) {
        this.resend = resend;
    }

    public SesProperties getSes() {
        return ses;
    }

    public void setSes(SesProperties ses) {
        this.ses = ses;
    }

    public String getFormattedFromAddress() {
        if (fromAddress == null || fromAddress.isBlank()) {
            return "";
        }
        if (fromDisplayName != null && !fromDisplayName.isBlank()) {
            return String.format("%s <%s>", fromDisplayName.trim(), fromAddress.trim());
        }
        return fromAddress.trim();
    }

    public static class ResendProperties {
        private String apiKey = "";
        private String apiUrl = "https://api.resend.com/emails";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
    }

    public static class SesProperties {
        private String region = "us-east-1";
        private String configurationSet = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private String endpoint = "";

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getConfigurationSet() {
            return configurationSet;
        }

        public void setConfigurationSet(String configurationSet) {
            this.configurationSet = configurationSet;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
