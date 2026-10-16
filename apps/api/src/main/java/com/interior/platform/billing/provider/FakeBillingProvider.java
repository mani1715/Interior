package com.interior.platform.billing.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.designers.domain.StudioDetailRecord;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic in-memory / local test provider for commercial billing tests.
 * Forbidden in production by ProductionConfigurationValidator.
 */
public class FakeBillingProvider implements BillingProvider {

    private final ObjectMapper objectMapper;

    public FakeBillingProvider() {
        this(new ObjectMapper());
    }

    public FakeBillingProvider(ObjectMapper objectMapper) {
        assertNotProduction();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private void assertNotProduction() {
        String sysProp = System.getProperty("spring.profiles.active");
        if (sysProp != null && sysProp.contains("production")) {
            throw new IllegalStateException("CRITICAL SECURITY FAULT: FakeBillingProvider cannot be instantiated in production!");
        }
        String envProp = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envProp != null && envProp.contains("production")) {
            throw new IllegalStateException("CRITICAL SECURITY FAULT: FakeBillingProvider cannot be instantiated in production!");
        }
    }

    @Override
    public String getProviderName() {
        return "FAKE";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(
            StudioDetailRecord studio,
            BillingPlanRecord plan,
            String successUrl,
            String cancelUrl,
            String idempotencyKey
    ) {
        String sessionId = "fake_sess_" + UUID.randomUUID().toString().substring(0, 8);
        String checkoutUrl = "https://checkout.fake-billing.local/pay/" + sessionId;
        return new CheckoutSessionResponse(
                sessionId,
                checkoutUrl,
                getProviderName(),
                plan.code(),
                plan.priceMinor(),
                plan.currency()
        );
    }

    @Override
    public void cancelSubscription(StudioSubscriptionRecord subscription, boolean atPeriodEnd) {
        // Record simulated external cancellation
    }

    @Override
    public WebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        // Case-insensitive header lookup for signature
        String sig = null;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("x-fake-signature".equalsIgnoreCase(entry.getKey())) {
                    sig = entry.getValue();
                    break;
                }
            }
        }

        if (sig == null || "invalid-sig".equalsIgnoreCase(sig)) {
            throw new SecurityException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.has("id") ? root.get("id").asText() : "ev_" + UUID.randomUUID().toString().substring(0, 8);
            String eventType = root.has("event") ? root.get("event").asText() : "payment.succeeded";
            String subId = root.has("subscription_id") ? root.get("subscription_id").asText() : null;
            String payId = root.has("payment_id") ? root.get("payment_id").asText() : null;
            Long amount = root.has("amount") ? root.get("amount").asLong() : null;

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(root, Map.class);

            return new WebhookEvent(
                    eventId,
                    eventType,
                    subId,
                    payId,
                    amount,
                    "PROCESSED",
                    Instant.now(),
                    map
            );
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse fake webhook: " + e.getMessage(), e);
        }
    }
}
