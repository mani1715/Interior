package com.interior.platform.email.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Production-ready transactional email provider adapter for Resend (https://resend.com).
 * Uses bounded HTTP client, structured payload serialization, error normalization,
 * and secret-safe logging.
 */
public class ResendEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);
    private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    private final EmailProperties emailProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailProvider(EmailProperties emailProperties, ObjectMapper objectMapper) {
        this(emailProperties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public ResendEmailProvider(EmailProperties emailProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.emailProperties = emailProperties != null ? emailProperties : new EmailProperties();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public String getProviderName() {
        return "RESEND";
    }

    @Override
    public boolean isConfigured() {
        String apiKey = emailProperties.getResend().getApiKey();
        String fromAddress = emailProperties.getFromAddress();
        return apiKey != null && !apiKey.isBlank() && !isPlaceholder(apiKey)
                && fromAddress != null && !fromAddress.isBlank() && !isPlaceholder(fromAddress);
    }

    @Override
    public EmailSendResult sendEmail(EmailMessage message) {
        if (!isConfigured()) {
            return EmailSendResult.notConfigured("Resend email provider is not configured or has placeholder credentials.");
        }

        if (message == null) {
            return EmailSendResult.permanentFailure("EMAIL_INVALID_REQUEST", "Email message cannot be null");
        }

        String recipient = message.recipientEmail();
        if (recipient == null || recipient.isBlank()) {
            return EmailSendResult.permanentFailure("EMAIL_INVALID_RECIPIENT", "Recipient email cannot be blank");
        }

        if (CRLF_PATTERN.matcher(recipient).find()) {
            return EmailSendResult.permanentFailure("HEADER_INJECTION_DETECTED", "CRLF detected in recipient email");
        }

        if (!EMAIL_PATTERN.matcher(recipient.trim()).matches()) {
            return EmailSendResult.permanentFailure("EMAIL_INVALID_RECIPIENT", "Malformed email address syntax: " + maskEmail(recipient));
        }

        String subject = message.subject();
        if (subject != null && CRLF_PATTERN.matcher(subject).find()) {
            return EmailSendResult.permanentFailure("HEADER_INJECTION_DETECTED", "CRLF detected in email subject");
        }

        String from = message.fromAddress() != null && !message.fromAddress().isBlank()
                ? message.fromAddress()
                : emailProperties.getFormattedFromAddress();

        if (from == null || from.isBlank()) {
            return EmailSendResult.permanentFailure("EMAIL_INVALID_REQUEST", "Sender from-address is missing");
        }

        if (CRLF_PATTERN.matcher(from).find()) {
            return EmailSendResult.permanentFailure("HEADER_INJECTION_DETECTED", "CRLF detected in sender from address");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", from);
            payload.put("to", List.of(recipient.trim()));
            payload.put("subject", subject != null ? subject : "");

            if (message.textContent() != null) {
                payload.put("text", message.textContent());
            }
            if (message.htmlContent() != null) {
                payload.put("html", message.htmlContent());
            }

            String replyTo = message.replyTo() != null && !message.replyTo().isBlank()
                    ? message.replyTo()
                    : emailProperties.getReplyTo();
            if (replyTo != null && !replyTo.isBlank()) {
                if (CRLF_PATTERN.matcher(replyTo).find()) {
                    return EmailSendResult.permanentFailure("HEADER_INJECTION_DETECTED", "CRLF detected in reply-to address");
                }
                payload.put("reply_to", replyTo.trim());
            }

            if (message.metadata() != null && !message.metadata().isEmpty()) {
                Map<String, String> headers = new HashMap<>();
                for (var entry : message.metadata().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && !CRLF_PATTERN.matcher(entry.getValue()).find()) {
                        headers.put("X-" + entry.getKey(), entry.getValue());
                    }
                }
                if (!headers.isEmpty()) {
                    payload.put("headers", headers);
                }
            }

            String requestBodyJson = objectMapper.writeValueAsString(payload);
            String apiUrl = emailProperties.getResend().getApiUrl();
            if (apiUrl == null || apiUrl.isBlank()) {
                apiUrl = "https://api.resend.com/emails";
            }

            int timeoutSeconds = Math.max(5, emailProperties.getTimeoutSeconds());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + emailProperties.getResend().getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Elegance-Interior-Platform/1.0")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body() != null ? response.body().trim() : "";

            if (statusCode == 200 || statusCode == 201) {
                JsonNode json = objectMapper.readTree(responseBody);
                if (json.has("id") && !json.get("id").asText().isBlank()) {
                    String messageId = json.get("id").asText();
                    log.info("Resend dispatched email successfully: messageId={}, recipient={}", messageId, maskEmail(recipient));
                    return EmailSendResult.success(messageId);
                }
                log.warn("Resend responded with HTTP {} but response missing 'id': {}", statusCode, responseBody);
                return EmailSendResult.permanentFailure("MALFORMED_PROVIDER_OUTPUT", "Resend response missing message id");
            } else if (statusCode == 400) {
                String errorMsg = extractErrorMessage(responseBody, "Bad request to Resend API");
                log.warn("Resend rejected request (HTTP 400): {}", errorMsg);
                return EmailSendResult.permanentFailure("EMAIL_INVALID_REQUEST", errorMsg);
            } else if (statusCode == 401 || statusCode == 403) {
                log.error("Resend authentication failed (HTTP {}). Check app.email.resend.api-key configuration.", statusCode);
                return EmailSendResult.permanentFailure("EMAIL_AUTH_FAILED", "Resend API authentication failed (HTTP " + statusCode + ")");
            } else if (statusCode == 422) {
                String errorMsg = extractErrorMessage(responseBody, "Unprocessable entity / invalid recipient");
                log.warn("Resend rejected recipient (HTTP 422): {}", errorMsg);
                return EmailSendResult.permanentFailure("EMAIL_INVALID_RECIPIENT", errorMsg);
            } else if (statusCode == 429) {
                log.warn("Resend upstream rate limit encountered (HTTP 429) for recipient={}", maskEmail(recipient));
                return EmailSendResult.transientFailure("EMAIL_RATE_LIMITED", "Resend API rate limit exceeded");
            } else if (statusCode >= 500) {
                log.error("Resend upstream server error (HTTP {}): {}", statusCode, responseBody);
                return EmailSendResult.transientFailure("EMAIL_PROVIDER_UNAVAILABLE", "Resend service error: HTTP " + statusCode);
            } else {
                log.warn("Unexpected HTTP status {} from Resend API: {}", statusCode, responseBody);
                return EmailSendResult.transientFailure("EMAIL_UNKNOWN_ERROR", "Unexpected provider response: HTTP " + statusCode);
            }

        } catch (HttpTimeoutException e) {
            log.error("Resend API request timed out for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.transientFailure("EMAIL_TIMEOUT", "Resend API request timed out: " + e.getMessage());
        } catch (IOException e) {
            log.error("Network IO error communicating with Resend for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.transientFailure("EMAIL_PROVIDER_UNAVAILABLE", "Network communication failure: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Resend dispatch interrupted for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.transientFailure("EMAIL_TIMEOUT", "Email dispatch was interrupted");
        } catch (Exception e) {
            log.error("Unexpected error in ResendEmailProvider for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.permanentFailure("EMAIL_UNKNOWN_ERROR", "Unexpected error: " + e.getMessage());
        }
    }

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message") && !root.get("message").asText().isBlank()) {
                return root.get("message").asText();
            }
            if (root.has("error") && !root.get("error").asText().isBlank()) {
                return root.get("error").asText();
            }
        } catch (Exception ignored) {}
        return defaultMsg;
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String trimmed = val.trim().toLowerCase();
        return trimmed.isEmpty()
                || trimmed.equals("placeholder")
                || trimmed.startsWith("your-")
                || trimmed.equals("fake")
                || trimmed.equals("mock")
                || trimmed.equals("none")
                || trimmed.equals("<email_from_address>");
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "unknown";
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return "***" + email.substring(Math.max(0, atIdx));
        return email.charAt(0) + "***" + email.substring(atIdx);
    }
}
