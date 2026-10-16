package com.interior.platform.email.service;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CommunicationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationDeliveryService.class);
    private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

    private final CommunicationDeliveryRepository deliveryRepository;
    private final TransactionalEmailService transactionalEmailService;

    @Value("${app.email.max-retries:3}")
    private int maxRetries = 3;

    @Value("${app.email.base-retry-delay-seconds:60}")
    private int baseRetryDelaySeconds = 60;

    public CommunicationDeliveryService(
            CommunicationDeliveryRepository deliveryRepository,
            TransactionalEmailService transactionalEmailService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.transactionalEmailService = transactionalEmailService;
    }

    /**
     * Sanitizes email header values to defend against CRLF header injection.
     */
    public static void validateHeaderSafety(String recipient, String subject) {
        if (recipient != null && CRLF_PATTERN.matcher(recipient).find()) {
            throw new IllegalArgumentException("Invalid recipient: CRLF characters detected in email address");
        }
        if (subject != null && CRLF_PATTERN.matcher(subject).find()) {
            throw new IllegalArgumentException("Invalid subject: CRLF characters detected in email subject");
        }
    }

    /**
     * Attempts external transactional email delivery.
     * Guaranteed never to throw an exception that would roll back the caller's business transaction.
     * Truthfully logs delivery outcome (SENT, NOT_CONFIGURED, RETRY_SCHEDULED, FAILED).
     */
    public CommunicationDeliveryRecord attemptEmailDelivery(
            UUID studioId,
            UUID recipientUserId,
            String eventType,
            String recipientEmail,
            String subject,
            String textBody,
            String htmlBody,
            String idempotencyKey
    ) {
        // Enforce CRLF header injection safety check
        try {
            validateHeaderSafety(recipientEmail, subject);
        } catch (IllegalArgumentException e) {
            log.warn("Header injection attack prevented for recipient '{}': {}", recipientEmail, e.getMessage());
            CommunicationDeliveryRecord rejectedRecord = new CommunicationDeliveryRecord(
                    UuidV7.randomUuid(),
                    studioId,
                    recipientUserId,
                    DeliveryChannel.EMAIL,
                    eventType,
                    "REJECTED_HEADER_INJECTION",
                    "REJECTED_HEADER_INJECTION",
                    DeliveryStatus.FAILED,
                    "VALIDATOR",
                    null,
                    1,
                    0,
                    e.getMessage(),
                    null,
                    idempotencyKey,
                    Instant.now(),
                    Instant.now(),
                    null
            );
            try {
                deliveryRepository.save(rejectedRecord);
            } catch (Exception ignored) {}
            return rejectedRecord;
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<CommunicationDeliveryRecord> existing = deliveryRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                CommunicationDeliveryRecord prev = existing.get();
                if (prev.status() == DeliveryStatus.SENT || prev.status() == DeliveryStatus.DELIVERED) {
                    log.debug("Idempotent delivery skip for key: {}", idempotencyKey);
                    return prev;
                }
            }
        }

        UUID deliveryId = UuidV7.randomUuid();
        Instant now = Instant.now();
        String providerName = transactionalEmailService.getProviderName();

        if (!transactionalEmailService.isConfigured()) {
            CommunicationDeliveryRecord disabledRecord = new CommunicationDeliveryRecord(
                    deliveryId,
                    studioId,
                    recipientUserId,
                    DeliveryChannel.EMAIL,
                    eventType,
                    recipientEmail != null ? recipientEmail : "unknown",
                    subject,
                    DeliveryStatus.NOT_CONFIGURED,
                    providerName,
                    null,
                    0,
                    maxRetries,
                    "Transactional email provider is disabled or not configured.",
                    null,
                    idempotencyKey,
                    now,
                    now,
                    null
            );
            try {
                deliveryRepository.save(disabledRecord);
            } catch (Exception e) {
                log.warn("Failed to persist disabled delivery log: {}", e.getMessage());
            }
            return disabledRecord;
        }

        // Provider is configured: dispatch
        try {
            EmailMessage message = new EmailMessage(recipientEmail, subject, textBody, htmlBody, java.util.Map.of("eventType", eventType));
            EmailSendResult result = transactionalEmailService.send(message);

            if (result.success()) {
                CommunicationDeliveryRecord record = new CommunicationDeliveryRecord(
                        deliveryId,
                        studioId,
                        recipientUserId,
                        DeliveryChannel.EMAIL,
                        eventType,
                        recipientEmail != null ? recipientEmail : "unknown",
                        subject,
                        DeliveryStatus.SENT,
                        providerName,
                        result.messageId(),
                        1,
                        maxRetries,
                        null,
                        null,
                        idempotencyKey,
                        now,
                        now,
                        now
                );
                deliveryRepository.save(record);
                return record;
            } else if (result.retryable()) {
                Instant nextRetry = now.plus(baseRetryDelaySeconds, ChronoUnit.SECONDS);
                CommunicationDeliveryRecord retryRecord = new CommunicationDeliveryRecord(
                        deliveryId,
                        studioId,
                        recipientUserId,
                        DeliveryChannel.EMAIL,
                        eventType,
                        recipientEmail != null ? recipientEmail : "unknown",
                        subject,
                        DeliveryStatus.RETRY_SCHEDULED,
                        providerName,
                        null,
                        1,
                        maxRetries,
                        result.errorMessage(),
                        nextRetry,
                        idempotencyKey,
                        now,
                        now,
                        null
                );
                deliveryRepository.save(retryRecord);
                return retryRecord;
            } else {
                CommunicationDeliveryRecord failedRecord = new CommunicationDeliveryRecord(
                        deliveryId,
                        studioId,
                        recipientUserId,
                        DeliveryChannel.EMAIL,
                        eventType,
                        recipientEmail != null ? recipientEmail : "unknown",
                        subject,
                        DeliveryStatus.FAILED,
                        providerName,
                        null,
                        1,
                        maxRetries,
                        result.errorMessage(),
                        null,
                        idempotencyKey,
                        now,
                        now,
                        null
                );
                deliveryRepository.save(failedRecord);
                return failedRecord;
            }
        } catch (Exception e) {
            log.error("Unexpected error during email delivery to {}: {}", recipientEmail, e.getMessage());
            Instant nextRetry = now.plus(baseRetryDelaySeconds, ChronoUnit.SECONDS);
            CommunicationDeliveryRecord failedRecord = new CommunicationDeliveryRecord(
                    deliveryId,
                    studioId,
                    recipientUserId,
                    DeliveryChannel.EMAIL,
                    eventType,
                    recipientEmail != null ? recipientEmail : "unknown",
                    subject,
                    DeliveryStatus.RETRY_SCHEDULED,
                    providerName,
                    null,
                    1,
                    maxRetries,
                    "Dispatch exception: " + e.getMessage(),
                    nextRetry,
                    idempotencyKey,
                    now,
                    now,
                    null
            );
            try {
                deliveryRepository.save(failedRecord);
            } catch (Exception saveErr) {
                log.warn("Failed to persist failed delivery record: {}", saveErr.getMessage());
            }
            return failedRecord;
        }
    }

    /**
     * Executes retry dispatch for an existing delivery record.
     */
    public void processRetry(CommunicationDeliveryRecord record) {
        if (record == null || record.status() != DeliveryStatus.RETRY_SCHEDULED) {
            return;
        }
        int newAttempt = record.attemptCount() + 1;
        Instant now = Instant.now();

        if (!transactionalEmailService.isConfigured()) {
            deliveryRepository.updateStatus(
                    record.id(),
                    DeliveryStatus.NOT_CONFIGURED,
                    null,
                    newAttempt,
                    "Provider is not configured on retry attempt.",
                    null,
                    null
            );
            return;
        }

        try {
            EmailMessage message = new EmailMessage(record.recipient(), record.subjectOrSummary(), record.subjectOrSummary());
            EmailSendResult result = transactionalEmailService.send(message);

            if (result.success()) {
                deliveryRepository.updateStatus(
                        record.id(),
                        DeliveryStatus.SENT,
                        result.messageId(),
                        newAttempt,
                        null,
                        null,
                        now
                );
            } else if (result.retryable() && newAttempt < record.maxAttempts()) {
                // Exponential backoff calculation: base * 2^(newAttempt - 1)
                long delaySeconds = (long) (baseRetryDelaySeconds * Math.pow(2, newAttempt - 1));
                delaySeconds = Math.min(delaySeconds, 86400L); // Cap backoff at 24 hours
                Instant nextRetry = now.plus(delaySeconds, ChronoUnit.SECONDS);

                deliveryRepository.updateStatus(
                        record.id(),
                        DeliveryStatus.RETRY_SCHEDULED,
                        null,
                        newAttempt,
                        result.errorMessage(),
                        nextRetry,
                        null
                );
            } else {
                deliveryRepository.updateStatus(
                        record.id(),
                        DeliveryStatus.FAILED,
                        null,
                        newAttempt,
                        result.errorMessage() != null ? result.errorMessage() : "Exceeded maximum retry attempts",
                        null,
                        null
                );
            }
        } catch (Exception e) {
            if (newAttempt < record.maxAttempts()) {
                long delaySeconds = (long) (baseRetryDelaySeconds * Math.pow(2, newAttempt - 1));
                delaySeconds = Math.min(delaySeconds, 86400L);
                Instant nextRetry = now.plus(delaySeconds, ChronoUnit.SECONDS);

                deliveryRepository.updateStatus(
                        record.id(),
                        DeliveryStatus.RETRY_SCHEDULED,
                        null,
                        newAttempt,
                        "Retry error: " + e.getMessage(),
                        nextRetry,
                        null
                );
            } else {
                deliveryRepository.updateStatus(
                        record.id(),
                        DeliveryStatus.FAILED,
                        null,
                        newAttempt,
                        "Exceeded max retries with error: " + e.getMessage(),
                        null,
                        null
                );
            }
        }
    }

    /**
     * Reconciles external webhook status changes (DELIVERED, BOUNCED, COMPLAINT).
     */
    public boolean reconcileProviderEvent(String providerMessageId, DeliveryStatus targetStatus, String reason) {
        if (providerMessageId == null || providerMessageId.isBlank() || targetStatus == null) {
            return false;
        }

        Optional<CommunicationDeliveryRecord> opt = deliveryRepository.findByProviderMessageId(providerMessageId);
        if (opt.isEmpty()) {
            log.info("No local communication record found for provider message id: {}", providerMessageId);
            return false;
        }

        CommunicationDeliveryRecord record = opt.get();

        // Out-of-order event defense: do not downgrade terminal or advanced states
        if (record.status() == DeliveryStatus.DELIVERED && (targetStatus == DeliveryStatus.SENT || targetStatus == DeliveryStatus.PENDING)) {
            log.info("Ignoring out-of-order event ({}) for already DELIVERED delivery {}", targetStatus, record.id());
            return true;
        }
        if ((record.status() == DeliveryStatus.BOUNCED || record.status() == DeliveryStatus.COMPLAINT)
                && (targetStatus == DeliveryStatus.SENT || targetStatus == DeliveryStatus.DELIVERED || targetStatus == DeliveryStatus.PENDING)) {
            log.info("Ignoring out-of-order event ({}) for already terminal {} delivery {}", targetStatus, record.status(), record.id());
            return true;
        }

        Instant now = Instant.now();
        Instant deliveredAt = (targetStatus == DeliveryStatus.DELIVERED) ? now : record.deliveredAt();

        deliveryRepository.updateStatus(
                record.id(),
                targetStatus,
                record.providerMessageId(),
                record.attemptCount(),
                reason != null ? reason : record.lastError(),
                null,
                deliveredAt
        );

        log.info("Communication delivery {} reconciled to {} via webhook event", record.id(), targetStatus);
        return true;
    }

    public List<CommunicationDeliveryRecord> listStudioDeliveries(UUID studioId, int limit, int offset) {
        int sanitizedLimit = Math.max(1, Math.min(100, limit));
        int sanitizedOffset = Math.max(0, offset);
        return deliveryRepository.findByStudioId(studioId, sanitizedLimit, sanitizedOffset);
    }
}
