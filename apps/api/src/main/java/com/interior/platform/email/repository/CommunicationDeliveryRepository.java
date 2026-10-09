package com.interior.platform.email.repository;

import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationDeliveryRepository {
    void save(CommunicationDeliveryRecord record);
    Optional<CommunicationDeliveryRecord> findById(UUID id);
    Optional<CommunicationDeliveryRecord> findByIdempotencyKey(String idempotencyKey);
    List<CommunicationDeliveryRecord> findByStudioId(UUID studioId, int limit, int offset);
    void updateStatus(UUID id, DeliveryStatus status, String providerMessageId, int attemptCount, String lastError, Instant nextRetryAt, Instant deliveredAt);
}
