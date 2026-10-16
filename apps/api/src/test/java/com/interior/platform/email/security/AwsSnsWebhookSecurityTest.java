package com.interior.platform.email.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.web.EmailWebhookController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E.1 — AWS SES / SNS Webhook Authenticity & SSRF Security Tests")
class AwsSnsWebhookSecurityTest {

    @Mock
    private CommunicationDeliveryService deliveryService;

    private AwsSnsSignatureValidator validator;
    private EmailProperties emailProperties;
    private ObjectMapper objectMapper;
    private EmailWebhookController controller;

    private static KeyPair testKeyPair;
    private static X509Certificate testCert;
    private static final String VALID_CERT_URL = "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-test.pem";
    private static final String VALID_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:elegance-ses-deliveries";

    @BeforeAll
    static void generateTestKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        testKeyPair = kpg.generateKeyPair();

        testCert = mock(X509Certificate.class);
        when(testCert.getPublicKey()).thenReturn(testKeyPair.getPublic());
    }

    @BeforeEach
    void setUp() {
        validator = new AwsSnsSignatureValidator();
        validator.cacheCertificateForTesting(VALID_CERT_URL, testCert);

        emailProperties = new EmailProperties();
        emailProperties.setProvider("ses");
        emailProperties.getSes().setSnsTopicArn(VALID_TOPIC_ARN);

        objectMapper = new ObjectMapper();

        controller = new EmailWebhookController(
                deliveryService,
                objectMapper,
                emailProperties,
                mock(ResendWebhookSignatureValidator.class),
                validator,
                null
        );
    }

    private String signCanonical(String canonical) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(testKeyPair.getPrivate());
        sig.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    private String buildNotificationJson(String messageId, String innerMessage, String topicArn, String certUrl) throws Exception {
        String timestamp = "2026-10-16T12:00:00.000Z";
        String canonical = "Message\n" + innerMessage + "\n" +
                "MessageId\n" + messageId + "\n" +
                "Timestamp\n" + timestamp + "\n" +
                "TopicArn\n" + topicArn + "\n" +
                "Type\nNotification\n";

        String signature = signCanonical(canonical);

        return objectMapper.writeValueAsString(Map.of(
                "Type", "Notification",
                "MessageId", messageId,
                "TopicArn", topicArn,
                "Message", innerMessage,
                "Timestamp", timestamp,
                "SignatureVersion", "2",
                "Signature", signature,
                "SigningCertURL", certUrl
        ));
    }

    @Test
    @DisplayName("Authenticity: Valid AWS SNS notification verified and delivery reconciled")
    void testValidSnsNotificationAccepted() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_valid_123\"}}";
        String payload = buildNotificationJson("sns_msg_001", sesInnerMsg, VALID_TOPIC_ARN, VALID_CERT_URL);

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deliveryService, times(1)).reconcileProviderEvent(eq("ses_msg_valid_123"), eq(DeliveryStatus.DELIVERED), isNull());
    }

    @Test
    @DisplayName("Security: Missing signature rejected with 401 and NO status mutation")
    void testMissingSignatureRejected() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "Type", "Notification",
                "MessageId", "sns_bad_001",
                "TopicArn", VALID_TOPIC_ARN,
                "Message", "{}",
                "Timestamp", "2026-10-16T12:00:00.000Z"
        ));

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Bad signature rejected with 401 and NO status mutation")
    void testBadSignatureRejected() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_forged\"}}";
        String timestamp = "2026-10-16T12:00:00.000Z";

        String payload = objectMapper.writeValueAsString(Map.of(
                "Type", "Notification",
                "MessageId", "sns_bad_sig",
                "TopicArn", VALID_TOPIC_ARN,
                "Message", sesInnerMsg,
                "Timestamp", timestamp,
                "SignatureVersion", "2",
                "Signature", "invalidBase64Signature==",
                "SigningCertURL", VALID_CERT_URL
        ));

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Modified message content rejected with 401 and NO status mutation")
    void testTamperedMessageRejected() throws Exception {
        String originalMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_orig\"}}";
        String timestamp = "2026-10-16T12:00:00.000Z";
        String canonical = "Message\n" + originalMsg + "\n" +
                "MessageId\nsns_tamper\n" +
                "Timestamp\n" + timestamp + "\n" +
                "TopicArn\n" + VALID_TOPIC_ARN + "\n" +
                "Type\nNotification\n";
        String signature = signCanonical(canonical);

        // Attacker modifies Message field after signing
        String tamperedMsg = "{\"eventType\":\"Bounce\",\"mail\":{\"messageId\":\"ses_msg_orig\"}}";

        String payload = objectMapper.writeValueAsString(Map.of(
                "Type", "Notification",
                "MessageId", "sns_tamper",
                "TopicArn", VALID_TOPIC_ARN,
                "Message", tamperedMsg,
                "Timestamp", timestamp,
                "SignatureVersion", "2",
                "Signature", signature,
                "SigningCertURL", VALID_CERT_URL
        ));

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("SSRF Defense: Non-Amazon SigningCertURL rejected immediately without network call")
    void testUntrustedCertHostRejected() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_ssrf\"}}";
        // Malicious attacker URL
        String evilCertUrl = "https://evil-attacker.com/fake-cert.pem";
        String payload = buildNotificationJson("sns_ssrf_1", sesInnerMsg, VALID_TOPIC_ARN, evilCertUrl);

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("SSRF Defense: Non-HTTPS SigningCertURL rejected immediately")
    void testNonHttpsCertUrlRejected() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_http\"}}";
        String httpCertUrl = "http://sns.us-east-1.amazonaws.com/SimpleNotificationService.pem";
        String payload = buildNotificationJson("sns_http_1", sesInnerMsg, VALID_TOPIC_ARN, httpCertUrl);

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Security: Unexpected TopicArn rejected with 401")
    void testUnexpectedTopicArnRejected() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_rogue_topic\"}}";
        String rogueTopicArn = "arn:aws:sns:us-east-1:999999999999:rogue-topic";
        String payload = buildNotificationJson("sns_rogue_1", sesInnerMsg, rogueTopicArn, VALID_CERT_URL);

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Replay Defense: Duplicate SNS MessageId handled idempotently without re-reconciling")
    void testDuplicateSnsMessageIdIdempotency() throws Exception {
        String sesInnerMsg = "{\"eventType\":\"Delivery\",\"mail\":{\"messageId\":\"ses_msg_dup_123\"}}";
        String payload = buildNotificationJson("sns_msg_dup_unique_99", sesInnerMsg, VALID_TOPIC_ARN, VALID_CERT_URL);

        // First attempt: succeeds and reconciles
        ResponseEntity<Void> response1 = controller.handleEmailWebhook("ses", payload, Map.of());
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        verify(deliveryService, times(1)).reconcileProviderEvent(eq("ses_msg_dup_123"), eq(DeliveryStatus.DELIVERED), isNull());

        // Second attempt: recognized as duplicate, returns 200 without second reconciliation
        ResponseEntity<Void> response2 = controller.handleEmailWebhook("ses", payload, Map.of());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(deliveryService, times(1)).reconcileProviderEvent(any(), any(), any()); // Still exactly 1 time
    }

    @Test
    @DisplayName("Subscription Confirmation: Safe Amazon SubscribeURL accepted")
    void testSafeSubscriptionConfirmation() throws Exception {
        String subscribeUrl = "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=" + VALID_TOPIC_ARN + "&Token=12345";
        String timestamp = "2026-10-16T12:00:00.000Z";
        String canonical = "Message\nSubscribe message\n" +
                "MessageId\nsns_sub_confirm\n" +
                "SubscribeURL\n" + subscribeUrl + "\n" +
                "Timestamp\n" + timestamp + "\n" +
                "Token\n12345\n" +
                "TopicArn\n" + VALID_TOPIC_ARN + "\n" +
                "Type\nSubscriptionConfirmation\n";

        String signature = signCanonical(canonical);

        String payload = objectMapper.writeValueAsString(Map.of(
                "Type", "SubscriptionConfirmation",
                "MessageId", "sns_sub_confirm",
                "TopicArn", VALID_TOPIC_ARN,
                "Message", "Subscribe message",
                "SubscribeURL", subscribeUrl,
                "Timestamp", timestamp,
                "Token", "12345",
                "SignatureVersion", "2",
                "Signature", signature,
                "SigningCertURL", VALID_CERT_URL
        ));

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deliveryService, never()).reconcileProviderEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Subscription Confirmation SSRF: Malicious SubscribeURL rejected with 400")
    void testMaliciousSubscribeUrlRejected() throws Exception {
        // Attacker points SubscribeURL to cloud metadata service
        String evilSubscribeUrl = "http://169.254.169.254/latest/meta-data/";
        String timestamp = "2026-10-16T12:00:00.000Z";
        String canonical = "Message\nSubscribe message\n" +
                "MessageId\nsns_sub_evil\n" +
                "SubscribeURL\n" + evilSubscribeUrl + "\n" +
                "Timestamp\n" + timestamp + "\n" +
                "Token\n12345\n" +
                "TopicArn\n" + VALID_TOPIC_ARN + "\n" +
                "Type\nSubscriptionConfirmation\n";

        String signature = signCanonical(canonical);

        String payload = objectMapper.writeValueAsString(Map.of(
                "Type", "SubscriptionConfirmation",
                "MessageId", "sns_sub_evil",
                "TopicArn", VALID_TOPIC_ARN,
                "Message", "Subscribe message",
                "SubscribeURL", evilSubscribeUrl,
                "Timestamp", timestamp,
                "Token", "12345",
                "SignatureVersion", "2",
                "Signature", signature,
                "SigningCertURL", VALID_CERT_URL
        ));

        ResponseEntity<Void> response = controller.handleEmailWebhook("ses", payload, Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
