package com.interior.platform.email.service;

import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class CommunicationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommunicationRetryScheduler.class);

    private final CommunicationDeliveryRepository deliveryRepository;
    private final CommunicationDeliveryService communicationDeliveryService;

    @Value("${app.email.retry-scheduler.enabled:true}")
    private boolean schedulerEnabled = true;

    @Value("${app.email.retry-scheduler.batch-size:25}")
    private int batchSize = 25;

    public CommunicationRetryScheduler(
            CommunicationDeliveryRepository deliveryRepository,
            CommunicationDeliveryService communicationDeliveryService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.communicationDeliveryService = communicationDeliveryService;
    }

    /**
     * Periodically polls for RETRY_SCHEDULED records that have passed their next_retry_at time.
     * Uses database-level row locking (FOR UPDATE SKIP LOCKED) to guarantee multi-instance concurrency safety.
     */
    @Scheduled(fixedDelayString = "${app.email.retry-scheduler.fixed-delay-ms:60000}")
    public void processPendingRetries() {
        if (!schedulerEnabled) {
            return;
        }

        String jobId = "job-comm-retry-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        org.slf4j.MDC.put("requestId", jobId);
        org.slf4j.MDC.put("correlationId", jobId);

        try {
            List<CommunicationDeliveryRecord> batch = deliveryRepository.claimNextRetryBatch(batchSize, Instant.now());
            if (!batch.isEmpty()) {
                log.info("Claimed {} communication delivery records for retry dispatch", batch.size());
            }

            for (CommunicationDeliveryRecord record : batch) {
                try {
                    communicationDeliveryService.processRetry(record);
                } catch (Exception e) {
                    log.error("Failed to process retry for record {}: {}", record.id(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error executing communication retry batch: {}", e.getMessage());
        } finally {
            org.slf4j.MDC.clear();
        }
    }
}
