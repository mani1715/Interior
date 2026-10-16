package com.interior.platform.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.email.web.EmailWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E — Email Disabled Workflow, Webhook Out-of-Order & Replay Defense Tests")
class EmailDisabledWorkflowTest {

    @Mock
    private CommunicationDeliveryRepository deliveryRepository;

    private DisabledEmailProvider disabledEmailProvider;
    private TransactionalEmailService transactionalEmailService;
    private CommunicationDeliveryService deliveryService;
    private EmailWebhookController webhookController;
    private ObjectMapper objectMapper;

    private UUID studioId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        disabledEmailProvider = new DisabledEmailProvider();
        transactionalEmailService = new TransactionalEmailService(disabledEmailProvider);
        deliveryService = new CommunicationDeliveryService(deliveryRepository, transactionalEmailService);
        objectMapper = new ObjectMapper();
        webhookController = new EmailWebhookController(deliveryService, objectMapper);

        studioId = UuidV7.randomUuid();
        userId = UuidV7.randomUuid();
    }

    @Test
    @DisplayName("Disabled Email Flow: Team invite delivery records NOT_CONFIGURED without rolling back or failing")
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
        assertEquals(DeliveryStatus.NOT_CONFIGURED, record.status());
        assertEquals("DISABLED", record.provider());
        assertNull(record.deliveredAt());
        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Disabled Email Flow: Review invite delivery records NOT_CONFIGURED without throwing exception")
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
        assertEquals(DeliveryStatus.NOT_CONFIGURED, record.status());
        assertEquals("DISABLED", record.provider());
        verify(deliveryRepository, times(1)).save(record);
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

        // Simulate delayed/out-of-order SENT event arriving from webhook
        boolean reconciled = deliveryService.reconcileProviderEvent("msg_re_abc", DeliveryStatus.SENT, "Late arriving sent event");

        assertTrue(reconciled);
        // Verify updateStatus was NEVER called to overwrite DELIVERED with SENT
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
    @DisplayName("Webhook Safety: Unknown provider message ID logs safely without updating database")
    void testUnknownProviderMessageId() {
        when(deliveryRepository.findByProviderMessageId("unknown_msg_999")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = webhookController.handleEmailWebhook(
                "resend",
                "{\"type\": \"email.delivered\", \"data\": {\"email_id\": \"unknown_msg_999\"}}",
                Map.of()
        );

        assertEquals(200, response.getStatusCode().value());
        verify(deliveryRepository, never()).updateStatus(any(), any(), any(), anyInt(), any(), any(), any());
    }
}
