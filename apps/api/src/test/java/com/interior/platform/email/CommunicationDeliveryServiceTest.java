package com.interior.platform.email.service;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.provider.EmailProvider;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunicationDeliveryServiceTest {

    @Mock
    private CommunicationDeliveryRepository deliveryRepository;

    @Mock
    private EmailProvider activeEmailProvider;

    private EmailTemplateService emailTemplateService;
    private UUID studioId;
    private UUID recipientUserId;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService("https://elegance.design");
        studioId = UuidV7.randomUuid();
        recipientUserId = UuidV7.randomUuid();
    }

    @Test
    @DisplayName("Email Delivery: Truthfully records NOT_CONFIGURED when disabled provider is active")
    void testDeliveryWithDisabledProvider() {
        DisabledEmailProvider disabledProvider = new DisabledEmailProvider();
        TransactionalEmailService emailService = new TransactionalEmailService(disabledProvider);
        CommunicationDeliveryService deliveryService = new CommunicationDeliveryService(deliveryRepository, emailService);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                recipientUserId,
                "TEAM_INVITATION",
                "designer@example.com",
                "Join Studio",
                "Text content",
                "<p>Html content</p>",
                "invite_key_123"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.NOT_CONFIGURED, record.status());
        assertEquals("DISABLED", record.provider());
        assertNull(record.deliveredAt());
        assertTrue(record.lastError().contains("not configured"));

        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Email Delivery: Truthfully records SENT when provider successfully dispatches")
    void testDeliveryWithActiveProviderSuccess() {
        when(activeEmailProvider.isConfigured()).thenReturn(true);
        when(activeEmailProvider.getProviderName()).thenReturn("SES");
        when(activeEmailProvider.sendEmail(any(EmailMessage.class)))
                .thenReturn(EmailSendResult.success("msg_ses_abc123"));

        TransactionalEmailService emailService = new TransactionalEmailService(activeEmailProvider);
        CommunicationDeliveryService deliveryService = new CommunicationDeliveryService(deliveryRepository, emailService);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                recipientUserId,
                "REVIEW_INVITATION",
                "client@example.com",
                "Review Invitation",
                "Please review",
                "<p>Please review</p>",
                "review_key_456"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.SENT, record.status());
        assertEquals("SES", record.provider());
        assertEquals("msg_ses_abc123", record.providerMessageId());
        assertNotNull(record.deliveredAt());
        assertNull(record.lastError());

        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Email Delivery: Truthfully records FAILED when provider returns failure without throwing")
    void testDeliveryWithActiveProviderFailure() {
        when(activeEmailProvider.isConfigured()).thenReturn(true);
        when(activeEmailProvider.getProviderName()).thenReturn("SES");
        when(activeEmailProvider.sendEmail(any(EmailMessage.class)))
                .thenReturn(EmailSendResult.failure("Rate limit exceeded on upstream provider"));

        TransactionalEmailService emailService = new TransactionalEmailService(activeEmailProvider);
        CommunicationDeliveryService deliveryService = new CommunicationDeliveryService(deliveryRepository, emailService);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                recipientUserId,
                "NEW_LEAD",
                "admin@studio.com",
                "New Lead Alert",
                "New lead",
                "<p>New lead</p>",
                "lead_key_789"
        );

        assertNotNull(record);
        assertEquals(DeliveryStatus.FAILED, record.status());
        assertEquals("SES", record.provider());
        assertEquals("Rate limit exceeded on upstream provider", record.lastError());
        assertNull(record.deliveredAt());

        verify(deliveryRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Email Delivery: Idempotency check prevents duplicate dispatch for existing SENT record")
    void testIdempotentDeliverySkip() {
        CommunicationDeliveryRecord existingSent = new CommunicationDeliveryRecord(
                UuidV7.randomUuid(),
                studioId,
                recipientUserId,
                DeliveryChannel.EMAIL,
                "TEAM_INVITATION",
                "designer@example.com",
                "Join Studio",
                DeliveryStatus.SENT,
                "SES",
                "existing_msg_id",
                1,
                3,
                null,
                null,
                "idempotent_key_999",
                java.time.Instant.now(),
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(deliveryRepository.findByIdempotencyKey("idempotent_key_999"))
                .thenReturn(Optional.of(existingSent));

        TransactionalEmailService emailService = new TransactionalEmailService(activeEmailProvider);
        CommunicationDeliveryService deliveryService = new CommunicationDeliveryService(deliveryRepository, emailService);

        CommunicationDeliveryRecord record = deliveryService.attemptEmailDelivery(
                studioId,
                recipientUserId,
                "TEAM_INVITATION",
                "designer@example.com",
                "Join Studio",
                "Text content",
                "<p>Html content</p>",
                "idempotent_key_999"
        );

        assertSame(existingSent, record);
        verify(activeEmailProvider, never()).sendEmail(any());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email Templates: XSS payloads in studio, client, and project names are escaped")
    void testTemplateEscapingXssSafety() {
        String xssPayload = "<script>alert('pwned')</script>";
        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTeamInvitation(
                xssPayload,
                "<b>Admin</b>",
                "/invite/abc123token"
        );

        assertFalse(rendered.htmlBody().contains("<script>"));
        assertTrue(rendered.htmlBody().contains("&lt;script&gt;alert(&#39;pwned&#39;)&lt;/script&gt;"));
        assertFalse(rendered.htmlBody().contains("<b>Admin</b>"));
        assertTrue(rendered.htmlBody().contains("&lt;b&gt;Admin&lt;/b&gt;"));
        assertTrue(rendered.htmlBody().contains("https://elegance.design/invite/abc123token"));
    }

    @Test
    @DisplayName("Email Templates: Application links strictly use configured canonical URL")
    void testTemplateCanonicalUrlEnforcement() {
        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderReviewInvitation(
                "Aura Design",
                "Mr. Sharma",
                "review/invite/token456"
        );

        assertTrue(rendered.htmlBody().contains("https://elegance.design/review/invite/token456"));
        assertTrue(rendered.textBody().contains("https://elegance.design/review/invite/token456"));
    }
}
