package com.interior.platform.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic configuration properties for commercial billing, subscriptions,
 * payment providers, and webhook authentication.
 * Defaults to "disabled" to ensure zero external calls occur until Phase 15.
 */
@Component
@ConfigurationProperties(prefix = "app.billing")
public class BillingProperties {

    /**
     * Active billing provider: disabled, fake, razorpay, stripe.
     * Defaults to "disabled" to prevent accidental commercial charges.
     */
    private String provider = "disabled";

    /**
     * Grace period in days when a subscription enters PAST_DUE.
     */
    private int gracePeriodDays = 7;

    /**
     * Default platform billing currency (ISO 4217 code).
     */
    private String currency = "INR";

    /**
     * Additional permitted redirect hosts for checkout completion.
     */
    private List<String> allowedRedirectHosts = new ArrayList<>();

    private RazorpayProperties razorpay = new RazorpayProperties();
    private StripeProperties stripe = new StripeProperties();

    public String getProvider() {
        return provider != null ? provider.trim().toLowerCase() : "disabled";
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<String> getAllowedRedirectHosts() {
        return allowedRedirectHosts;
    }

    public void setAllowedRedirectHosts(List<String> allowedRedirectHosts) {
        this.allowedRedirectHosts = allowedRedirectHosts;
    }

    public RazorpayProperties getRazorpay() {
        return razorpay;
    }

    public void setRazorpay(RazorpayProperties razorpay) {
        this.razorpay = razorpay;
    }

    public StripeProperties getStripe() {
        return stripe;
    }

    public void setStripe(StripeProperties stripe) {
        this.stripe = stripe;
    }

    public static class RazorpayProperties {
        private String keyId = "";
        private String keySecret = "";
        private String webhookSecret = "";
        private String apiUrl = "https://api.razorpay.com/v1";

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public boolean isConfigured() {
            return isNonBlankAndNotPlaceholder(keyId)
                    && isNonBlankAndNotPlaceholder(keySecret)
                    && isNonBlankAndNotPlaceholder(webhookSecret);
        }
    }

    public static class StripeProperties {
        private String apiKey = "";
        private String webhookSecret = "";
        private String apiUrl = "https://api.stripe.com/v1";
        private int timestampToleranceSeconds = 300;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public int getTimestampToleranceSeconds() {
            return timestampToleranceSeconds;
        }

        public void setTimestampToleranceSeconds(int timestampToleranceSeconds) {
            this.timestampToleranceSeconds = timestampToleranceSeconds;
        }

        public boolean isConfigured() {
            return isNonBlankAndNotPlaceholder(apiKey)
                    && isNonBlankAndNotPlaceholder(webhookSecret);
        }
    }

    private static boolean isNonBlankAndNotPlaceholder(String val) {
        if (val == null || val.isBlank()) return false;
        String lower = val.trim().toLowerCase();
        return !lower.startsWith("your-") && !lower.contains("placeholder") && !lower.equals("change-me");
    }
}
