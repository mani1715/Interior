package com.interior.platform.email.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 12E — Resend Email Provider Contract & Security Tests")
class ResendEmailProviderContractTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private EmailProperties emailProperties;
    private ObjectMapper objectMapper;
    private ResendEmailProvider provider;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setProvider("resend");
        emailProperties.setFromAddress("notifications@interior.design");
        emailProperties.setFromDisplayName("Elégance");
        emailProperties.getResend().setApiKey("re_test_key_1234567890");
        emailProperties.getResend().setApiUrl("https://api.resend.com/emails");

        objectMapper = new ObjectMapper();
        provider = new ResendEmailProvider(emailProperties, objectMapper, httpClient);
    }

    @Test
    @DisplayName("Resend Contract: Successful send (HTTP 200/201) returns message ID")
    void testResendSuccess() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\": \"re_msg_abc123xyz\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage(
                "designer@example.com",
                "Welcome to Elégance",
                "Hello designer",
                "<p>Hello designer</p>",
                java.util.Map.of("category", "INVITATION")
        );

        EmailSendResult result = provider.sendEmail(message);

        assertTrue(result.success());
        assertEquals("re_msg_abc123xyz", result.messageId());
        assertEquals("SENT", result.status());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: HTTP 400 Bad Request maps to permanent EMAIL_INVALID_REQUEST failure")
    void testResendBadRequest() throws Exception {
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"message\": \"Invalid from address\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_INVALID_REQUEST", result.errorCode());
        assertFalse(result.retryable());
        assertTrue(result.errorMessage().contains("Invalid from address"));
    }

    @Test
    @DisplayName("Resend Contract: HTTP 401/403 Auth Error maps to permanent EMAIL_AUTH_FAILED failure")
    void testResendAuthError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"message\": \"Invalid API key\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_AUTH_FAILED", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: HTTP 422 Unprocessable Recipient maps to EMAIL_INVALID_RECIPIENT")
    void testResendUnprocessableRecipient() throws Exception {
        when(httpResponse.statusCode()).thenReturn(422);
        when(httpResponse.body()).thenReturn("{\"message\": \"Domain has no MX records\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_INVALID_RECIPIENT", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: HTTP 429 Rate Limit maps to transient EMAIL_RATE_LIMITED failure")
    void testResendRateLimited() throws Exception {
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("{\"message\": \"Too many requests\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_RATE_LIMITED", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: HTTP 500/503 Upstream Error maps to transient EMAIL_PROVIDER_UNAVAILABLE failure")
    void testResendUpstreamError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpResponse.body()).thenReturn("Service Unavailable");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_PROVIDER_UNAVAILABLE", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: Socket/Request Timeout maps to transient EMAIL_TIMEOUT failure")
    void testResendTimeout() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new HttpTimeoutException("request timed out"));

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("EMAIL_TIMEOUT", result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    @DisplayName("Resend Contract: Malformed 200 OK missing 'id' fails closed with MALFORMED_PROVIDER_OUTPUT")
    void testResendMalformedPayload() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"status\": \"ok\"}");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("MALFORMED_PROVIDER_OUTPUT", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Security: CRLF injection in recipient email is rejected fail-closed")
    void testResendCrlfRecipientInjection() {
        EmailMessage message = new EmailMessage("designer@example.com\r\nBcc: victim@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("HEADER_INJECTION_DETECTED", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Security: CRLF injection in subject is rejected fail-closed")
    void testResendCrlfSubjectInjection() {
        EmailMessage message = new EmailMessage("designer@example.com", "Subject\r\nCc: attacker@example.com", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("HEADER_INJECTION_DETECTED", result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    @DisplayName("Resend Provider: Unconfigured provider truthfully returns notConfigured without network call")
    void testResendUnconfigured() {
        emailProperties.getResend().setApiKey("");
        assertFalse(provider.isConfigured());

        EmailMessage message = new EmailMessage("designer@example.com", "Subject", "Body");
        EmailSendResult result = provider.sendEmail(message);

        assertFalse(result.success());
        assertEquals("NOT_CONFIGURED", result.errorCode());
        verifyNoInteractions(httpClient);
    }
}
