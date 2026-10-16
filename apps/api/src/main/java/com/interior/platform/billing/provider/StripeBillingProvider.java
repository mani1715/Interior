package com.interior.platform.billing.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.config.BillingProperties;
import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.designers.domain.StudioDetailRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe candidate payment provider adapter.
 * Handles Stripe Checkout Session contracts, timestamped signature verification (t=, v1=),
 * replay attack tolerance enforcement, and event schema transformation.
 * Live credentials intentionally deferred to Phase 15.
 */
public class StripeBillingProvider implements BillingProvider {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final BillingProperties properties;
    private final ObjectMapper objectMapper;

    public StripeBillingProvider(BillingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }

    @Override
    public boolean isConfigured() {
        return properties != null && properties.getStripe().isConfigured();
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(
            StudioDetailRecord studio,
            BillingPlanRecord plan,
            String successUrl,
            String cancelUrl,
            String idempotencyKey
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe billing provider is not configured. Live activation deferred to Phase 15.");
        }

        String sessionId = "cs_live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String checkoutUrl = "https://checkout.stripe.com/c/pay/" + sessionId;

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
        if (!isConfigured()) {
            log.info("Stripe not configured; recorded local cancellation for subscription: {}", subscription.id());
            return;
        }
        log.info("Stripe cancelSubscription requested for external subId: {}, atPeriodEnd: {}",
                subscription.providerSubscriptionId(), atPeriodEnd);
    }

    @Override
    public WebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        String secret = properties != null ? properties.getStripe().getWebhookSecret() : "";
        if (secret == null || secret.isBlank()) {
            throw new SecurityException("Stripe webhook secret is not configured on the server");
        }

        String signatureHeader = findHeader(headers, "stripe-signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SecurityException("Missing Stripe signature header: Stripe-Signature");
        }

        int toleranceSeconds = properties != null ? properties.getStripe().getTimestampToleranceSeconds() : 300;
        verifyStripeSignature(rawPayload, signatureHeader.trim(), secret, toleranceSeconds);

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.hasNonNull("id") ? root.get("id").asText() : null;
            String eventType = root.hasNonNull("type") ? root.get("type").asText() : "payment_intent.succeeded";

            String subId = null;
            String payId = null;
            Long amountMinor = null;
            String status = "PROCESSED";

            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.has("object")) {
                JsonNode obj = dataNode.get("object");

                if (obj.hasNonNull("id")) {
                    payId = obj.get("id").asText();
                }
                if (obj.hasNonNull("payment_intent")) {
                    payId = obj.get("payment_intent").asText();
                }
                if (obj.hasNonNull("subscription")) {
                    subId = obj.get("subscription").asText();
                } else if (eventType.startsWith("customer.subscription") && obj.hasNonNull("id")) {
                    subId = obj.get("id").asText();
                }

                if (obj.hasNonNull("amount_total")) {
                    amountMinor = obj.get("amount_total").asLong();
                } else if (obj.hasNonNull("amount_paid")) {
                    amountMinor = obj.get("amount_paid").asLong();
                } else if (obj.hasNonNull("amount")) {
                    amountMinor = obj.get("amount").asLong();
                }

                if (obj.hasNonNull("status")) {
                    status = obj.get("status").asText();
                }
            }

            if (eventId == null || eventId.isBlank()) {
                eventId = "evt_stripe_" + (payId != null ? payId : UUID.randomUUID().toString().substring(0, 8));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawData = objectMapper.convertValue(root, Map.class);

            Instant timestamp = root.hasNonNull("created")
                    ? Instant.ofEpochSecond(root.get("created").asLong())
                    : Instant.now();

            return new WebhookEvent(
                    eventId,
                    eventType,
                    subId,
                    payId,
                    amountMinor,
                    status,
                    timestamp,
                    rawData
            );
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Stripe webhook payload: " + e.getMessage(), e);
        }
    }

    private void verifyStripeSignature(String payload, String header, String secret, int toleranceSeconds) {
        String[] pairs = header.split(",");
        long timestamp = -1;
        List<String> signatures = new ArrayList<>();

        for (String pair : pairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String val = parts[1].trim();
                if ("t".equals(key)) {
                    try {
                        timestamp = Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        throw new SecurityException("Invalid timestamp in Stripe-Signature header: " + val);
                    }
                } else if ("v1".equals(key)) {
                    signatures.add(val);
                }
            }
        }

        if (timestamp < 0) {
            throw new SecurityException("Missing timestamp (t=) in Stripe-Signature header");
        }
        if (signatures.isEmpty()) {
            throw new SecurityException("Missing signature (v1=) in Stripe-Signature header");
        }

        long nowSec = Instant.now().getEpochSecond();
        if (toleranceSeconds > 0 && Math.abs(nowSec - timestamp) > toleranceSeconds) {
            throw new SecurityException("Stripe webhook timestamp outside tolerance window (" + toleranceSeconds + "s)");
        }

        String signedPayload = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] computedHash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computedHash);

            boolean matchFound = false;
            for (String sig : signatures) {
                if (MessageDigest.isEqual(
                        computedHex.getBytes(StandardCharsets.UTF_8),
                        sig.getBytes(StandardCharsets.UTF_8)
                )) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                throw new SecurityException("Cryptographic signature mismatch for Stripe webhook");
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Failed to calculate HMAC-SHA256 signature for Stripe webhook: " + e.getMessage(), e);
        }
    }

    private String findHeader(Map<String, String> headers, String targetName) {
        if (headers == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (targetName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
