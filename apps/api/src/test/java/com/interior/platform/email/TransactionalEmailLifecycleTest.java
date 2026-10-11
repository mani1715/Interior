package com.interior.platform.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.provider.FakeEmailProvider;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.EmailTemplateService;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.email.web.EmailWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9B — Transactional Email & Communication Lifecycle Architecture Tests")
class TransactionalEmailLifecycleTest {

    @Mock
    private CommunicationDeliveryRepository deliveryRepository;

    private FakeEmailProvider fakeEmailProvider;
    private TransactionalEmailService transactionalEmailService;
    private CommunicationDeliveryService deliveryService;
    private EmailTemplateService emailTemplateService;
    private EmailWebhookController emailWebhookController;
    private ObjectMapper objectMapper;

    private UUID studioId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        fakeEmailProvider = new FakeEmailProvider();
        transactionalEmailService = new TransactionalEmailService(fakeEmailProvider);
        deliveryService = new CommunicationDeliveryService(deliveryRepository, transactionalEmailService);
        emailTemplateService = new EmailTemplateService("https://elegance.design");
        objectMapper = new ObjectMapper();
        emailWebhookController = new EmailWebhookController(deliveryService, objectMapper);

        studioId = UuidV7.randomUuid();
        userId = UuidV7.randomUuid();
    }

    @Test
    @DisplayName("Email Provider: FakeEmailProvider successfully delivers and records generated provider message ID")
    void testFakeEmailProviderSuccess() {
        fakeEmailProvider.setSimulationMode(FakeEmailProvider.SimulationMode.SUCCESS);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "SUBSCRIPTION_ACTIVATED",
                "owner@studio.com",
                "Subscription Activated",
                "Your plan is active",
                "<p>Your plan is active</p>",
                "sub_act_123"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.SENT, record.status());
        assertEquals("FAKE_SIMULATED", record.provider());
        assertNotNull(record.providerMessageId());
        assertTrue(record.providerMessageId().startsWith("sim_"));
        assertEquals(1, fakeEmailProvider.getSentMessages().size());
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Email Provider: Transient failure transitions to RETRY_SCHEDULED with bounded backoff")
    void testFakeEmailProviderTransientFailureSchedulesRetry() {
        fakeEmailProvider.setSimulationMode(FakeEmailProvider.SimulationMode.TRANSIENT_FAILURE);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "PAYMENT_FAILED",
                "owner@studio.com",
                "Payment Failed",
                "Payment failed",
                "<p>Payment failed</p>",
                "pay_fail_123"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.RETRY_SCHEDULED, record.status());
        assertNotNull(record.nextRetryAt());
        assertTrue(record.nextRetryAt().isAfter(Instant.now()));
        assertTrue(record.lastError().contains("Simulated transient upstream rate limit"));
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Email Provider: Permanent failure marks delivery as terminal FAILED immediately")
    void testFakeEmailProviderPermanentFailureMarksFailed() {
        fakeEmailProvider.setSimulationMode(FakeEmailProvider.SimulationMode.PERMANENT_FAILURE);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "NEW_LEAD",
                "invalid@studio.com",
                "New Lead Alert",
                "New lead",
                "<p>New lead</p>",
                "lead_perm_123"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.FAILED, record.status());
        assertNull(record.nextRetryAt());
        assertTrue(record.lastError().contains("permanent failure"));
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Security: CRLF injection in recipient email or subject is rejected with zero exception to caller")
    void testHeaderInjectionDefended() {
        // Attempt CRLF in recipient
        CommunicationDeliveryRecord record1 = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "TEAM_INVITATION",
                "victim@domain.com\r\nBcc: attacker@evil.com",
                "Valid Subject",
                "Text",
                "Html",
                "inj_1"
        );
        assertEquals(DeliveryStatus.FAILED, record1.status());
        assertEquals("VALIDATOR", record1.provider());
        assertTrue(record1.lastError().contains("CRLF characters detected in email address"));

        // Attempt CRLF in subject
        CommunicationDeliveryRecord record2 = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "TEAM_INVITATION",
                "user@domain.com",
                "Subject\nBcc: evil@domain.com",
                "Text",
                "Html",
                "inj_2"
        );
        assertEquals(DeliveryStatus.FAILED, record2.status());
        assertEquals("VALIDATOR", record2.provider());
        assertTrue(record2.lastError().contains("CRLF characters detected in email subject"));
    }

    @Test
    @DisplayName("Webhook Reconciliation: Delivery event updates local record to DELIVERED")
    void testWebhookReconcileDelivered() {
        String msgId = "sim_test_msg_999";
        CommunicationDeliveryRecord existing = new CommunicationDeliveryRecord(
                UuidV7.randomUuid(),
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "SUBSCRIPTION_ACTIVATED",
                "owner@studio.com",
                "Active",
                DeliveryStatus.SENT,
                "FAKE_SIMULATED",
                msgId,
                1,
                3,
                null,
                null,
                "idemp_1",
                Instant.now(),
                Instant.now(),
                null
        );

        when(deliveryRepository.findByProviderMessageId(msgId)).thenReturn(Optional.of(existing));

        String jsonPayload = """
            {
                "messageId": "sim_test_msg_999",
                "type": "email.delivered"
            }
        """;

        ResponseEntity<Void> response = emailWebhookController.handleEmailWebhook("fake", jsonPayload, Map.of());
        assertEquals(200, response.getStatusCode().value());

        verify(deliveryRepository, times(1)).updateStatus(
                eq(existing.id()),
                eq(DeliveryStatus.DELIVERED),
                eq(msgId),
                eq(1),
                isNull(),
                isNull(),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Webhook Reconciliation: Bounce event marks terminal BOUNCED status")
    void testWebhookReconcileBounced() {
        String msgId = "sim_bounce_msg_888";
        CommunicationDeliveryRecord existing = new CommunicationDeliveryRecord(
                UuidV7.randomUuid(),
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "TEAM_INVITATION",
                "bounce@studio.com",
                "Invite",
                DeliveryStatus.SENT,
                "FAKE_SIMULATED",
                msgId,
                1,
                3,
                null,
                null,
                "idemp_2",
                Instant.now(),
                Instant.now(),
                null
        );

        when(deliveryRepository.findByProviderMessageId(msgId)).thenReturn(Optional.of(existing));

        String jsonPayload = """
            {
                "messageId": "sim_bounce_msg_888",
                "type": "email.bounced",
                "reason": "Mailbox does not exist (550)"
            }
        """;

        ResponseEntity<Void> response = emailWebhookController.handleEmailWebhook("fake", jsonPayload, Map.of());
        assertEquals(200, response.getStatusCode().value());

        verify(deliveryRepository, times(1)).updateStatus(
                eq(existing.id()),
                eq(DeliveryStatus.BOUNCED),
                eq(msgId),
                eq(1),
                eq("Mailbox does not exist (550)"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Email Templates: Billing templates properly escape user content and render server grace dates")
    void testBillingEmailTemplates() {
        String xssStudio = "<b>Luxury Spaces</b><script>alert(1)</script>";
        String xssPlan = "Pro <img src=x onerror=alert(1)>";
        String graceDate = "18 Oct 2026";

        EmailTemplateService.RenderedEmail activated = emailTemplateService.renderSubscriptionActivated(xssStudio, xssPlan, "/workspace/billing");
        assertFalse(activated.htmlBody().contains("<script>"));
        assertFalse(activated.htmlBody().contains("<img"));
        assertTrue(activated.htmlBody().contains("https://elegance.design/workspace/billing"));

        EmailTemplateService.RenderedEmail failed = emailTemplateService.renderPaymentFailed(xssStudio, xssPlan, graceDate, "/workspace/billing");
        assertFalse(failed.htmlBody().contains("<script>"));
        assertTrue(failed.htmlBody().contains("18 Oct 2026"));
        assertTrue(failed.textBody().contains("18 Oct 2026"));

        EmailTemplateService.RenderedEmail renewed = emailTemplateService.renderSubscriptionRenewed(xssStudio, xssPlan, "15 Nov 2026", "/workspace/billing");
        assertTrue(renewed.htmlBody().contains("15 Nov 2026"));

        EmailTemplateService.RenderedEmail cancel = emailTemplateService.renderCancelAtPeriodEnd(xssStudio, xssPlan, "31 Oct 2026", "/workspace/billing");
        assertTrue(cancel.htmlBody().contains("31 Oct 2026"));

        EmailTemplateService.RenderedEmail sec = emailTemplateService.renderSecurityAlert("John <script>", "Session Revoked", "Active session ended", "/account");
        assertFalse(sec.htmlBody().contains("<script>"));
        assertTrue(sec.htmlBody().contains("https://elegance.design/account"));
    }
}
