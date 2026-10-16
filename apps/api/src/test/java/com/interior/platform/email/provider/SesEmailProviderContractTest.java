package com.interior.platform.email.provider;

import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E — AWS SES Email Provider Contract & Security Tests")
class SesEmailProviderContractTest {

    @Mock
    private SesV2Client sesClient;

    private EmailProperties emailProperties;
    private SesEmailProvider provider;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setProvider("ses");
        emailProperties.setFromAddress("notifications@interior.design");
        emailProperties.setFromDisplayName("Elégance");
        emailProperties.getSes().setRegion("us-east-1");

        provider = new SesEmailProvider(emailProperties, sesClient);
    }

    @Test
    @DisplayName("SES Contract: Successful send returns AWS message ID")
    void testSesSuccess() {
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("0100018f-abcd-1234-ses-msg")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        EmailMessage message = new EmailMessage(
                "client@example.com",
                "Review Invitation",
                "Please review your project",
                "<p>Please review your project</p>",
                java.util.Map.of("leadId", "lead_123")
        );

        EmailSendResult result = provider.sendEmail(message);

        assertTrue(result.success());
        assertEquals("0100018f-abcd-1234-ses-msg", result.messageId());
        assertEquals("SENT", result.status());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("SES Contract: TooManyRequestsException maps to transient EMAIL_RATE_LIMITED failure")
    void testSesRateLimited() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(TooManyRequestsException.builder().message("Daily quota exceeded").build());

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_RATE_LIMITED", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("SES Contract: MailFromDomainNotVerifiedException maps to permanent EMAIL_AUTH_FAILED failure")
    void testSesDomainNotVerified() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(MailFromDomainNotVerifiedException.builder().message("Domain not verified").build());

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_AUTH_FAILED", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("SES Contract: BadRequestException maps to permanent EMAIL_INVALID_REQUEST failure")
    void testSesBadRequest() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(BadRequestException.builder().message("Invalid parameters").build());

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_INVALID_REQUEST", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("SES Contract: 5xx SesV2Exception maps to transient EMAIL_PROVIDER_UNAVAILABLE failure")
    void testSesServerUnavailable() {
        SesV2Exception serverError = (SesV2Exception) SesV2Exception.builder()
                .statusCode(503)
                .message("Service Unavailable in Region")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(serverError);

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_PROVIDER_UNAVAILABLE", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("SES Contract: SdkClientException / Network Timeout maps to transient EMAIL_TIMEOUT failure")
    void testSesTimeout() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SdkClientException.create("Read timed out"));

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_TIMEOUT", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("SES Security: CRLF injection in recipient email is rejected fail-closed")
    void testSesCrlfRecipientInjection() {
        EmailMessage message = new EmailMessage("client@example.com\r\nBcc: evil@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("HEADER_INJECTION_DETECTED", result.errorCode());
        assertFalse(result.retryable());
        verifyNoInteractions(sesClient);
    }

    @Test
    @DisplayName("SES Provider: Unconfigured provider truthfully returns notConfigured without calling SDK")
    void testSesUnconfigured() {
        emailProperties.setFromAddress("");
        assertFalse(provider.isConfigured());

        EmailMessage message = new EmailMessage("client@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("NOT_CONFIGURED", result.errorCode());
        verifyNoInteractions(sesClient);
    }
}
