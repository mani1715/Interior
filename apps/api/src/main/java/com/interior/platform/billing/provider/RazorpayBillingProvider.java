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
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Razorpay candidate payment provider adapter.
 * Handles Razorpay Order/Subscription checkout contracts and cryptographic HMAC-SHA256 webhook signature validation.
 * Live credentials intentionally deferred to Phase 15.
 */
public class RazorpayBillingProvider implements BillingProvider {

    private static final Logger log = LoggerFactory.getLogger(RazorpayBillingProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final BillingProperties properties;
    private final ObjectMapper objectMapper;

    public RazorpayBillingProvider(BillingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public boolean isConfigured() {
        return properties != null && properties.getRazorpay().isConfigured();
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
            throw new IllegalStateException("Razorpay billing provider is not configured. Live activation deferred to Phase 15.");
        }

        // Generate provider order session reference
        String orderId = "order_rzp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String checkoutUrl = "https://api.razorpay.com/v1/checkout/hosted/" + orderId;

        return new CheckoutSessionResponse(
                orderId,
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
            log.info("Razorpay not configured; recorded local cancellation for subscription: {}", subscription.id());
            return;
        }
        log.info("Razorpay cancelSubscription requested for external subId: {}, atPeriodEnd: {}",
                subscription.providerSubscriptionId(), atPeriodEnd);
    }

    @Override
    public WebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        String secret = properties != null ? properties.getRazorpay().getWebhookSecret() : "";
        if (secret == null || secret.isBlank()) {
            throw new SecurityException("Razorpay webhook secret is not configured on the server");
        }

        String signatureHeader = findHeader(headers, "x-razorpay-signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SecurityException("Missing Razorpay signature header: X-Razorpay-Signature");
        }

        verifyHmacSignature(rawPayload, signatureHeader.trim(), secret);

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.hasNonNull("id") ? root.get("id").asText() : null;
            String eventType = root.hasNonNull("event") ? root.get("event").asText() : "payment.captured";

            String subId = null;
            String payId = null;
            Long amountMinor = null;
            String status = "PROCESSED";

            JsonNode payloadNode = root.get("payload");
            if (payloadNode != null) {
                JsonNode paymentNode = payloadNode.get("payment");
                if (paymentNode != null && paymentNode.has("entity")) {
                    JsonNode payEntity = paymentNode.get("entity");
                    if (payEntity.hasNonNull("id")) {
                        payId = payEntity.get("id").asText();
                    }
                    if (payEntity.hasNonNull("amount")) {
                        amountMinor = payEntity.get("amount").asLong();
                    }
                    if (payEntity.hasNonNull("status")) {
                        status = payEntity.get("status").asText();
                    }
                }

                JsonNode subNode = payloadNode.get("subscription");
                if (subNode != null && subNode.has("entity")) {
                    JsonNode subEntity = subNode.get("entity");
                    if (subEntity.hasNonNull("id")) {
                        subId = subEntity.get("id").asText();
                    }
                }
            }

            if (eventId == null || eventId.isBlank()) {
                eventId = "rzp_evt_" + (payId != null ? payId : UUID.randomUUID().toString().substring(0, 8));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawData = objectMapper.convertValue(root, Map.class);

            Instant timestamp = root.hasNonNull("created_at")
                    ? Instant.ofEpochSecond(root.get("created_at").asLong())
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
            throw new IllegalArgumentException("Failed to parse Razorpay webhook payload: " + e.getMessage(), e);
        }
    }

    private void verifyHmacSignature(String payload, String expectedSignatureHex, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] computedHash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computedHash);

            if (!MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    expectedSignatureHex.getBytes(StandardCharsets.UTF_8)
            )) {
                throw new SecurityException("Cryptographic signature mismatch for Razorpay webhook");
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Failed to calculate HMAC-SHA256 signature for Razorpay webhook: " + e.getMessage(), e);
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
