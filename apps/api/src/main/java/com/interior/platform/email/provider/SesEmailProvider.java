package com.interior.platform.email.provider;

import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;
import software.amazon.awssdk.services.sesv2.model.*;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Production-ready transactional email provider adapter for Amazon Simple Email Service (SES v2).
 * Supports AWS Default Credential Provider Chain (IAM roles, ECS task roles, EKS IRSA)
 * as well as optional explicit static credentials.
 */
public class SesEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SesEmailProvider.class);
    private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    private final EmailProperties emailProperties;
    private final SesV2Client sesClient;

    public SesEmailProvider(EmailProperties emailProperties) {
        this(emailProperties, buildDefaultClient(emailProperties));
    }

    public SesEmailProvider(EmailProperties emailProperties, SesV2Client sesClient) {
        this.emailProperties = emailProperties != null ? emailProperties : new EmailProperties();
        this.sesClient = sesClient;
    }

    @Override
    public String getProviderName() {
        return "SES";
    }

    @Override
    public boolean isConfigured() {
        String fromAddress = emailProperties.getFromAddress();
        if (fromAddress == null || fromAddress.isBlank() || isPlaceholder(fromAddress)) {
            return false;
        }
        String region = emailProperties.getSes().getRegion();
        return region != null && !region.isBlank() && !isPlaceholder(region);
    }

    @Override
    public EmailSendResult sendEmail(EmailMessage message) {
        if (!isConfigured()) {
            return EmailSendResult.notConfigured("AWS SES email provider is not configured or missing valid from-address/region.");
        }

        if (sesClient == null) {
            return EmailSendResult.permanentFailure("EMAIL_PROVIDER_UNAVAILABLE", "SesV2Client is not initialized");
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
            Body.Builder bodyBuilder = Body.builder();
            if (message.textContent() != null && !message.textContent().isBlank()) {
                bodyBuilder.text(Content.builder().data(message.textContent()).charset("UTF-8").build());
            }
            if (message.htmlContent() != null && !message.htmlContent().isBlank()) {
                bodyBuilder.html(Content.builder().data(message.htmlContent()).charset("UTF-8").build());
            }

            Message sesMessage = Message.builder()
                    .subject(Content.builder().data(subject != null ? subject : "").charset("UTF-8").build())
                    .body(bodyBuilder.build())
                    .build();

            Destination destination = Destination.builder()
                    .toAddresses(recipient.trim())
                    .build();

            EmailContent emailContent = EmailContent.builder()
                    .simple(sesMessage)
                    .build();

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .fromEmailAddress(from)
                    .destination(destination)
                    .content(emailContent);

            String replyTo = message.replyTo() != null && !message.replyTo().isBlank()
                    ? message.replyTo()
                    : emailProperties.getReplyTo();
            if (replyTo != null && !replyTo.isBlank()) {
                if (CRLF_PATTERN.matcher(replyTo).find()) {
                    return EmailSendResult.permanentFailure("HEADER_INJECTION_DETECTED", "CRLF detected in reply-to address");
                }
                requestBuilder.replyToAddresses(replyTo.trim());
            }

            String configSet = emailProperties.getSes().getConfigurationSet();
            if (configSet != null && !configSet.isBlank() && !isPlaceholder(configSet)) {
                requestBuilder.configurationSetName(configSet.trim());
            }

            SendEmailResponse response = sesClient.sendEmail(requestBuilder.build());
            String messageId = response.messageId();

            log.info("AWS SES dispatched email successfully: messageId={}, recipient={}", messageId, maskEmail(recipient));
            return EmailSendResult.success(messageId);

        } catch (TooManyRequestsException | LimitExceededException e) {
            log.warn("AWS SES rate limit or sending quota exceeded for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.transientFailure("EMAIL_RATE_LIMITED", "AWS SES sending quota or rate limit exceeded: " + e.getMessage());
        } catch (MailFromDomainNotVerifiedException | AccountSuspendedException e) {
            log.error("AWS SES authorization or domain verification failure for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.permanentFailure("EMAIL_AUTH_FAILED", "AWS SES authorization failure: " + e.getMessage());
        } catch (BadRequestException | NotFoundException e) {
            log.warn("AWS SES rejected invalid request for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.permanentFailure("EMAIL_INVALID_REQUEST", "AWS SES invalid request: " + e.getMessage());
        } catch (SesV2Exception e) {
            int statusCode = e.statusCode();
            log.error("AWS SES service exception (HTTP {}) for recipient={}: {}", statusCode, maskEmail(recipient), e.getMessage());
            if (statusCode >= 500) {
                return EmailSendResult.transientFailure("EMAIL_PROVIDER_UNAVAILABLE", "AWS SES service error: " + e.getMessage());
            } else {
                return EmailSendResult.permanentFailure("EMAIL_REJECTED", "AWS SES rejected email: " + e.getMessage());
            }
        } catch (SdkClientException e) {
            log.error("AWS SDK client communication failure for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.transientFailure("EMAIL_TIMEOUT", "AWS SES client timeout/connectivity error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in SesEmailProvider for recipient={}: {}", maskEmail(recipient), e.getMessage());
            return EmailSendResult.permanentFailure("EMAIL_UNKNOWN_ERROR", "Unexpected SES error: " + e.getMessage());
        }
    }

    private static SesV2Client buildDefaultClient(EmailProperties properties) {
        if (properties == null) return null;
        try {
            SesV2ClientBuilder builder = SesV2Client.builder();
            String region = properties.getSes().getRegion();
            if (region != null && !region.isBlank()) {
                builder.region(Region.of(region.trim()));
            } else {
                builder.region(Region.US_EAST_1);
            }

            String accessKey = properties.getSes().getAccessKeyId();
            String secretKey = properties.getSes().getSecretAccessKey();
            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }

            String endpoint = properties.getSes().getEndpoint();
            if (endpoint != null && !endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint.trim()));
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Could not initialize default SesV2Client: {}", e.getMessage());
            return null;
        }
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
