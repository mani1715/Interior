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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommunicationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationDeliveryService.class);

    private final CommunicationDeliveryRepository deliveryRepository;
    private final TransactionalEmailService transactionalEmailService;

    public CommunicationDeliveryService(
            CommunicationDeliveryRepository deliveryRepository,
            TransactionalEmailService transactionalEmailService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.transactionalEmailService = transactionalEmailService;
    }

    /**
     * Attempts external transactional email delivery.
     * Guaranteed never to throw an exception that would roll back the caller's business transaction.
     * Truthfully logs delivery outcome (SENT, NOT_CONFIGURED, FAILED).
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
                    3,
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

            DeliveryStatus status = result.success() ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
            Instant deliveredAt = result.success() ? Instant.now() : null;

            CommunicationDeliveryRecord record = new CommunicationDeliveryRecord(
                    deliveryId,
                    studioId,
                    recipientUserId,
                    DeliveryChannel.EMAIL,
                    eventType,
                    recipientEmail != null ? recipientEmail : "unknown",
                    subject,
                    status,
                    providerName,
                    result.messageId(),
                    1,
                    3,
                    result.errorMessage(),
                    null,
                    idempotencyKey,
                    now,
                    now,
                    deliveredAt
            );

            deliveryRepository.save(record);
            return record;
        } catch (Exception e) {
            log.error("Unexpected error during email delivery to {}: {}", recipientEmail, e.getMessage());
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
                    3,
                    "Dispatch error: " + e.getMessage(),
                    null,
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

    public List<CommunicationDeliveryRecord> listStudioDeliveries(UUID studioId, int limit, int offset) {
        int sanitizedLimit = Math.max(1, Math.min(100, limit));
        int sanitizedOffset = Math.max(0, offset);
        return deliveryRepository.findByStudioId(studioId, sanitizedLimit, sanitizedOffset);
    }
}
