package com.interior.platform.email.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.web.EmailWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E.1 — Resend Webhook Authenticity & Security Tests")
class ResendWebhookSecurityTest {

    @Mock
    private CommunicationDeliveryRepository deliveryRepository;

    @Mock
    private CommunicationDeliveryService deliveryService;

    private ResendWebhookSignatureValidator validator;
    private EmailProperties emailProperties;
    private ObjectMapper objectMapper;
    private EmailWebhookController controller;

    private static final String TEST_SECRET = "whsec_dGVzdC1zZWNyZXQtMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="; // Base64 test key
    private static final String RAW_KEY = "test-secret-12345678901234567890123456789012";

    @BeforeEach
    void setUp() {
        validator = new ResendWebhookSignatureValidator();
        emailProperties = new EmailProperties();
        emailProperties.setWebhookSecret(TEST_SECRET);
        objectMapper = new ObjectMapper();

        controller = new EmailWebhookController(
                deliveryService,
                objectMapper,
                emailProperties,
                validator,
                mock(AwsSnsSignatureValidator.class),
                null
        );
    }

    private String computeSignature(String msgId, String timestamp, String payload, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        String toSign = msgId + "." + timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Authenticity: Valid Resend signature processes event and mutates status")
    void testValidSignatureAccepted() throws Exception {
        String msgId = "msg_test_valid_001";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"" + msgId + "\"}}";
        String sig = computeSignature("msg_123", timestamp, payload, RAW_KEY);

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", timestamp,
                "svix-signature", "v1," + sig
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, headers);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deliveryService, times(1)).reconcileProviderEvent(eq(msgId), eq(DeliveryStatus.DELIVERED), isNull());
    }

    @Test
    @DisplayName("Security: Missing signature headers rejected with 401 and NO status mutation")
    void testMissingSignatureRejected() {
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_hacked\"}}";

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Invalid signature rejected with 401 and NO status mutation")
    void testInvalidSignatureRejected() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_hacked\"}}";

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", timestamp,
                "svix-signature", "v1,invalidBase64SignatureHere=="
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, headers);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Tampered payload rejected with 401 and NO status mutation")
    void testTamperedPayloadRejected() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String originalPayload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_orig\"}}";
        String sig = computeSignature("msg_123", timestamp, originalPayload, RAW_KEY);

        String tamperedPayload = "{\"type\": \"email.bounced\", \"data\": {\"email_id\": \"msg_orig\"}}";

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", timestamp,
                "svix-signature", "v1," + sig
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", tamperedPayload, headers);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Replay Defense: Expired timestamp (>300 seconds) rejected with 401 and NO status mutation")
    void testStaleTimestampReplayRejected() throws Exception {
        String oldTimestamp = String.valueOf(Instant.now().minusSeconds(360).getEpochSecond()); // 6 minutes old
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_replay\"}}";
        String sig = computeSignature("msg_123", oldTimestamp, payload, RAW_KEY);

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", oldTimestamp,
                "svix-signature", "v1," + sig
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, headers);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Wrong secret rejected with 401 and NO status mutation")
    void testWrongSecretRejected() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_wrong_secret\"}}";
        String sigWithWrongKey = computeSignature("msg_123", timestamp, payload, "completely-wrong-key-000000000000");

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", timestamp,
                "svix-signature", "v1," + sigWithWrongKey
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, headers);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Unconfigured Server Secret: Webhook rejected with 401 when app.email.webhook-secret is empty")
    void testUnconfiguredWebhookSecretRejected() throws Exception {
        emailProperties.setWebhookSecret(""); // Unconfigured

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String payload = "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"msg_test\"}}";

        Map<String, String> headers = Map.of(
                "svix-id", "msg_123",
                "svix-timestamp", timestamp,
                "svix-signature", "v1,somesig"
        );

        ResponseEntity<Void> response = controller.handleEmailWebhook("resend", payload, headers);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }
}
