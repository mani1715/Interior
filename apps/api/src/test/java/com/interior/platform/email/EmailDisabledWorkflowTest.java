package com.interior.platform.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.security.AwsSnsSignatureValidator;
import com.interior.platform.email.security.ResendWebhookSignatureValidator;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.email.web.EmailWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E.1 — Email Disabled Source-Truth & Delivery Semantics Tests")
class EmailDisabledWorkflowTest {

    @Mock
    private CommunicationDeliveryRepository deliveryRepository;

    private DisabledEmailProvider disabledEmailProvider;
    private TransactionalEmailService transactionalEmailService;
    private CommunicationDeliveryService deliveryService;
    private EmailProperties emailProperties;
    private ResendWebhookSignatureValidator resendValidator;
    private AwsSnsSignatureValidator awsSnsValidator;
    private EmailWebhookController webhookController;
    private ObjectMapper objectMapper;

    private UUID studioId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        disabledEmailProvider = new DisabledEmailProvider();
        transactionalEmailService = new TransactionalEmailService(disabledEmailProvider);
        deliveryService = new CommunicationDeliveryService(deliveryRepository, transactionalEmailService);
        emailProperties = new EmailProperties();
        emailProperties.setProvider("disabled");
        emailProperties.setWebhookSecret("test-secret-whsec-123");

        resendValidator = mock(ResendWebhookSignatureValidator.class);
        awsSnsValidator = mock(AwsSnsSignatureValidator.class);
        objectMapper = new ObjectMapper();

        webhookController = new EmailWebhookController(
                deliveryService,
                objectMapper,
                emailProperties,
                resendValidator,
                awsSnsValidator,
                null
        );

        studioId = UuidV7.randomUuid();
        userId = UuidV7.randomUuid();
    }

    @Test
    @DisplayName("Disabled Email Flow: Team invite delivery records NOT_CONFIGURED with attempt=0, null providerMsgId, and null deliveredAt")
    void testTeamInviteWhenEmailDisabled() {
        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "TEAM_INVITATION",
                "colleague@studio.com",
                "Join Studio",
                "Text content",
                "<p>Html content</p>",
                "team_invite:123"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.NOT_CONFIGURED, record.status(), "Status MUST be NOT_CONFIGURED when disabled");
        assertEquals("DISABLED", record.provider());
        assertNull(record.providerMessageId(), "Disabled provider must NOT create a fake external message ID");
        assertEquals(0, record.attemptCount(), "Attempt count must be 0 (no network call occurred)");
        assertNull(record.nextRetryAt(), "Disabled provider must NEVER schedule a retry");
        assertNull(record.deliveredAt(), "Disabled provider must NEVER set deliveredAt");
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Disabled Email Flow: Review invite delivery records NOT_CONFIGURED without throwing exception or fake send")
    void testReviewInviteWhenEmailDisabled() {
        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                userId,
                "REVIEW_INVITATION",
                "client@home.com",
                "Share Your Experience",
                "Please review",
                "<p>Please review</p>",
                "review_invite:456"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.NOT_CONFIGURED, record.status(), "Status MUST be NOT_CONFIGURED");
        assertEquals("DISABLED", record.provider());
        assertNull(record.providerMessageId(), "providerMessageId must be null");
        assertEquals(0, record.attemptCount(), "attemptCount must be 0");
        assertNull(record.nextRetryAt(), "nextRetryAt must be null");
        assertNull(record.deliveredAt(), "deliveredAt must be null");
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Disabled Email Flow: processRetry on disabled provider sets NOT_CONFIGURED and does not schedule further retry")
    void testProcessRetryWithDisabledProvider() {
        UUID deliveryId = UuidV7.randomUuid();
        CommunicationDeliveryRecord retryRecord = new CommunicationDeliveryRecord(
                deliveryId,
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "TEAM_INVITATION",
                "user@example.com",
                "Subject",
                DeliveryStatus.RETRY_SCHEDULED,
                "DISABLED",
                null,
                1,
                3,
                "Initial failure",
                Instant.now().minusSeconds(10),
                "key_retry",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(10),
                null
        );

        deliveryService.processRetry(retryRecord);

        // Verify it updates status to NOT_CONFIGURED and nextRetryAt is null
        verify(deliveryRepository, times(1)).updateStatus(
                eq(deliveryId),
                eq(DeliveryStatus.NOT_CONFIGURED),
                isNull(),
                eq(2),
                contains("not configured"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Reconcile Guard: Webhook reconciliation cannot mutate a NOT_CONFIGURED delivery record")
    void testReconcileCannotMutateNotConfiguredRecord() {
        UUID deliveryId = UuidV7.randomUuid();
        CommunicationDeliveryRecord notConfiguredRecord = new CommunicationDeliveryRecord(
                deliveryId,
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "TEAM_INVITATION",
                "user@example.com",
                "Subject",
                DeliveryStatus.NOT_CONFIGURED,
                "DISABLED",
                "fake_or_stale_msg_id",
                0,
                3,
                "Provider not configured",
                null,
                "key_not_conf",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60),
                null
        );

        when(deliveryRepository.findByProviderMessageId("fake_or_stale_msg_id")).thenReturn(Optional.of(notConfiguredRecord));

        boolean reconciled = deliveryService.reconcileProviderEvent("fake_or_stale_msg_id", DeliveryStatus.DELIVERED, "Delivered event");

        assertFalse(reconciled, "Reconciliation must fail and reject mutating a NOT_CONFIGURED record");
        verify(deliveryRepository, never()).updateStatus(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("Out-of-Order Defense: Delayed SENT or PENDING event cannot downgrade an already DELIVERED message")
    void testOutOfOrderEventDoesNotDowngradeDelivered() {
        UUID deliveryId = UuidV7.randomUuid();
        Instant deliveredTime = Instant.now().minusSeconds(100);

        CommunicationDeliveryRecord existingRecord = new CommunicationDeliveryRecord(
                deliveryId,
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "TEAM_INVITATION",
                "colleague@studio.com",
                "Subject",
                DeliveryStatus.DELIVERED,
                "RESEND",
                "msg_re_abc",
                1,
                3,
                null,
                null,
                "key_123",
                deliveredTime.minusSeconds(10),
                deliveredTime,
                deliveredTime
        );

        when(deliveryRepository.findByProviderMessageId("msg_re_abc")).thenReturn(Optional.of(existingRecord));

        boolean reconciled = deliveryService.reconcileProviderEvent("msg_re_abc", DeliveryStatus.SENT, "Late arriving sent event");

        assertTrue(reconciled);
        verify(deliveryRepository, never()).updateStatus(any(), eq(DeliveryStatus.SENT), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("Out-of-Order Defense: Delayed SENT or DELIVERED event cannot overwrite a BOUNCED record")
    void testOutOfOrderEventDoesNotDowngradeBounced() {
        UUID deliveryId = UuidV7.randomUuid();
        Instant bounceTime = Instant.now().minusSeconds(50);

        CommunicationDeliveryRecord existingRecord = new CommunicationDeliveryRecord(
                deliveryId,
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "REVIEW_INVITATION",
                "bad@nonexistent.domain",
                "Subject",
                DeliveryStatus.BOUNCED,
                "SES",
                "msg_ses_xyz",
                1,
                3,
                "Hard bounce: mailbox does not exist",
                null,
                "key_456",
                bounceTime.minusSeconds(20),
                bounceTime,
                null
        );

        when(deliveryRepository.findByProviderMessageId("msg_ses_xyz")).thenReturn(Optional.of(existingRecord));

        boolean reconciled = deliveryService.reconcileProviderEvent("msg_ses_xyz", DeliveryStatus.DELIVERED, "Late delivered");

        assertTrue(reconciled);
        verify(deliveryRepository, never()).updateStatus(any(), eq(DeliveryStatus.DELIVERED), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("Out-of-Order Defense: COMPLAINT record cannot be downgraded by delayed DELIVERED or SENT")
    void testOutOfOrderComplaintCannotBeDowngraded() {
        UUID deliveryId = UuidV7.randomUuid();
        Instant complaintTime = Instant.now().minusSeconds(30);

        CommunicationDeliveryRecord existingRecord = new CommunicationDeliveryRecord(
                deliveryId,
                studioId,
                userId,
                DeliveryChannel.EMAIL,
                "NEW_LEAD_ALERT",
                "spam@report.domain",
                "Subject",
                DeliveryStatus.COMPLAINT,
                "RESEND",
                "msg_re_complaint",
                1,
                3,
                "User marked as spam",
                null,
                "key_complaint",
                complaintTime.minusSeconds(10),
                complaintTime,
                null
        );

        when(deliveryRepository.findByProviderMessageId("msg_re_complaint")).thenReturn(Optional.of(existingRecord));

        boolean reconciled = deliveryService.reconcileProviderEvent("msg_re_complaint", DeliveryStatus.DELIVERED, "Late delivered");

        assertTrue(reconciled);
        verify(deliveryRepository, never()).updateStatus(any(), eq(DeliveryStatus.DELIVERED), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("Webhook Safety: Unknown provider message ID logs safely without updating database")
    void testUnknownProviderMessageId() {
        when(resendValidator.isValid(anyString(), anyMap(), anyString())).thenReturn(true);
        when(deliveryRepository.findByProviderMessageId("unknown_msg_999")).thenReturn(Optional.empty());

        webhookController.handleEmailWebhook(
                "resend",
                "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"unknown_msg_999\"}}",
                java.util.Map.of("svix-id", "test_id", "svix-timestamp", "12345", "svix-signature", "v1,valid")
        );

        verify(deliveryRepository, never()).updateStatus(any(), any(), any(), anyInt(), any(), any(), any());
    }
}
