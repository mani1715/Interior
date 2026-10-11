package com.interior.platform.email.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.service.CommunicationDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/public/webhooks/email")
@Tag(name = "Email Webhooks", description = "Public webhooks for external email provider event reconciliation")
public class EmailWebhookController {

    private static final Logger log = LoggerFactory.getLogger(EmailWebhookController.class);

    private final CommunicationDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @Value("${app.email.webhook-secret:}")
    private String webhookSecret;

    public EmailWebhookController(
            CommunicationDeliveryService deliveryService,
            ObjectMapper objectMapper
    ) {
        this.deliveryService = deliveryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Reconciles email delivery events (delivered, bounced, complaint) from upstream email providers.
     * Validates cryptographic webhook signature if a webhook secret is configured.
     */
    @PostMapping("/{provider}")
    @Operation(summary = "Handle email provider webhook", description = "Reconciles delivery events (DELIVERED, BOUNCED, COMPLAINT) from external email providers")
    public ResponseEntity<Void> handleEmailWebhook(
            @PathVariable("provider") String provider,
            @RequestBody(required = false) String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        if (rawBody == null || rawBody.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Webhook signature verification if secret is configured
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String signature = headers.get("x-webhook-signature");
            if (signature == null || !verifySignature(rawBody, signature, webhookSecret)) {
                log.warn("Invalid webhook signature received for provider: {}", provider);
                return ResponseEntity.status(401).build();
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String messageId = root.has("messageId") ? root.get("messageId").asText() : null;
            if (messageId == null && root.has("id")) {
                messageId = root.get("id").asText();
            }

            String eventType = root.has("type") ? root.get("type").asText() : "";
            String reason = root.has("reason") ? root.get("reason").asText() : null;

            if (messageId != null && !messageId.isBlank()) {
                DeliveryStatus targetStatus = mapEventType(eventType);
                if (targetStatus != null) {
                    deliveryService.reconcileProviderEvent(messageId, targetStatus, reason);
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to parse email webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private DeliveryStatus mapEventType(String eventType) {
        if (eventType == null) return null;
        String normalized = eventType.toUpperCase();
        if (normalized.contains("DELIVERED") || normalized.contains("DELIVERY")) {
            return DeliveryStatus.DELIVERED;
        } else if (normalized.contains("BOUNCE")) {
            return DeliveryStatus.BOUNCED;
        } else if (normalized.contains("COMPLAINT") || normalized.contains("SPAM")) {
            return DeliveryStatus.COMPLAINT;
        } else if (normalized.contains("FAIL") || normalized.contains("DROPPED")) {
            return DeliveryStatus.FAILED;
        }
        return null;
    }

    private boolean verifySignature(String payload, String signature, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
