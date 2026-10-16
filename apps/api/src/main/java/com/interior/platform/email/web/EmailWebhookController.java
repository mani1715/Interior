package com.interior.platform.email.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.security.AwsSnsSignatureValidator;
import com.interior.platform.email.security.ResendWebhookSignatureValidator;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.security.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/public/webhooks/email")
@Tag(name = "Email Webhooks", description = "Public webhooks for external email provider event reconciliation")
public class EmailWebhookController {

    private static final Logger log = LoggerFactory.getLogger(EmailWebhookController.class);

    private final CommunicationDeliveryService deliveryService;
    private final ObjectMapper objectMapper;
    private final EmailProperties emailProperties;
    private final ResendWebhookSignatureValidator resendValidator;
    private final AwsSnsSignatureValidator awsSnsValidator;
    private final RateLimiterService rateLimiterService;

    public EmailWebhookController(
            CommunicationDeliveryService deliveryService,
            ObjectMapper objectMapper
    ) {
        this(deliveryService, objectMapper, new EmailProperties(), new ResendWebhookSignatureValidator(), new AwsSnsSignatureValidator(), null);
    }

    @Autowired
    public EmailWebhookController(
            CommunicationDeliveryService deliveryService,
            ObjectMapper objectMapper,
            EmailProperties emailProperties,
            ResendWebhookSignatureValidator resendValidator,
            AwsSnsSignatureValidator awsSnsValidator,
            @Autowired(required = false) RateLimiterService rateLimiterService
    ) {
        this.deliveryService = deliveryService;
        this.objectMapper = objectMapper;
        this.emailProperties = emailProperties;
        this.resendValidator = resendValidator;
        this.awsSnsValidator = awsSnsValidator;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Reconciles email delivery events (delivered, bounced, complaint) from upstream email providers.
     * Enforces strict cryptographic signature authentication before any delivery record is mutated.
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

        String prov = provider != null ? provider.trim().toLowerCase() : "";

        // Rate limiting: allow legitimate burst notifications while mitigating DoS floods
        if (rateLimiterService != null) {
            try {
                rateLimiterService.acquire("email_webhook:" + prov, 120, Duration.ofMinutes(1));
            } catch (RateLimitExceededException e) {
                log.warn("Rate limit exceeded on email webhook endpoint for provider: {}", prov);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
        }

        if ("resend".equals(prov)) {
            return handleResendWebhook(rawBody, headers);
        } else if ("ses".equals(prov) || "sns".equals(prov)) {
            return handleSesSnsWebhook(rawBody);
        } else {
            log.warn("Email webhook received for unsupported or disabled provider: {}", prov);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private ResponseEntity<Void> handleResendWebhook(String rawBody, Map<String, String> headers) {
        String secret = emailProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Resend webhook rejected: webhook signing secret is not configured on server");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean verified = resendValidator.isValid(rawBody, headers, secret);
        if (!verified) {
            log.warn("Resend webhook rejected: cryptographic signature verification failed or replay detected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Authenticity confirmed: process payload safely
        try {
            JsonNode root = objectMapper.readTree(rawBody);

            String messageId = null;
            if (root.has("data")) {
                JsonNode data = root.get("data");
                if (data.has("email_id")) {
                    messageId = data.get("email_id").asText();
                } else if (data.has("id")) {
                    messageId = data.get("id").asText();
                }
            } else if (root.has("id")) {
                messageId = root.get("id").asText();
            }

            String eventType = root.has("type") ? root.get("type").asText() : "";
            DeliveryStatus targetStatus = mapEventType(eventType);

            String reason = null;
            if (root.has("data") && root.get("data").has("reason")) {
                reason = root.get("data").get("reason").asText();
            } else if (root.has("reason")) {
                reason = root.get("reason").asText();
            }

            if (messageId != null && !messageId.isBlank() && targetStatus != null) {
                deliveryService.reconcileProviderEvent(messageId, targetStatus, reason);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to parse authenticated Resend webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<Void> handleSesSnsWebhook(String rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.error("Failed to parse AWS SES/SNS webhook JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        String expectedTopicArn = emailProperties.getSes().getSnsTopicArn();
        boolean verified = awsSnsValidator.isValid(root, expectedTopicArn);
        if (!verified) {
            log.warn("AWS SES/SNS webhook rejected: signature verification failed or SSRF protection triggered");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Deduplication to prevent replay attacks
        String snsMessageId = root.has("MessageId") ? root.get("MessageId").asText() : null;
        if (awsSnsValidator.isDuplicateMessage(snsMessageId)) {
            log.info("Ignoring duplicate AWS SNS messageId: {}", snsMessageId);
            return ResponseEntity.ok().build();
        }

        String type = root.has("Type") ? root.get("Type").asText() : "";

        if ("SubscriptionConfirmation".equals(type)) {
            String subscribeUrl = root.has("SubscribeURL") ? root.get("SubscribeURL").asText() : null;
            if (awsSnsValidator.isSafeSubscribeUrl(subscribeUrl)) {
                log.info("SNS SubscriptionConfirmation received and verified for topic: {}", root.path("TopicArn").asText());
                return ResponseEntity.ok().build();
            } else {
                log.warn("SNS SubscriptionConfirmation rejected: unsafe SubscribeURL: {}", subscribeUrl);
                return ResponseEntity.badRequest().build();
            }
        }

        if ("Notification".equals(type)) {
            try {
                String innerMessageStr = root.has("Message") ? root.get("Message").asText() : null;
                if (innerMessageStr == null || innerMessageStr.isBlank()) {
                    return ResponseEntity.ok().build();
                }

                JsonNode sesMsg = objectMapper.readTree(innerMessageStr);

                String messageId = null;
                if (sesMsg.has("mail") && sesMsg.get("mail").has("messageId")) {
                    messageId = sesMsg.get("mail").get("messageId").asText();
                } else if (sesMsg.has("messageId")) {
                    messageId = sesMsg.get("messageId").asText();
                }

                String eventType = "";
                if (sesMsg.has("eventType")) {
                    eventType = sesMsg.get("eventType").asText();
                } else if (sesMsg.has("notificationType")) {
                    eventType = sesMsg.get("notificationType").asText();
                }

                String reason = null;
                if (sesMsg.has("bounce")) {
                    JsonNode bounce = sesMsg.get("bounce");
                    reason = "Bounce: " + (bounce.has("bounceType") ? bounce.get("bounceType").asText() : "Unknown");
                } else if (sesMsg.has("complaint")) {
                    JsonNode complaint = sesMsg.get("complaint");
                    reason = "Complaint: " + (complaint.has("complaintFeedbackType") ? complaint.get("complaintFeedbackType").asText() : "Unknown");
                }

                DeliveryStatus targetStatus = mapEventType(eventType);
                if (messageId != null && !messageId.isBlank() && targetStatus != null) {
                    deliveryService.reconcileProviderEvent(messageId, targetStatus, reason);
                }

                return ResponseEntity.ok().build();
            } catch (Exception e) {
                log.error("Failed to parse authenticated SES notification message: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok().build();
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
        } else if (normalized.contains("SENT")) {
            return DeliveryStatus.SENT;
        }
        return null;
    }
}
