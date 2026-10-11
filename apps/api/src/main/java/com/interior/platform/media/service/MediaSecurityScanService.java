package com.interior.platform.media.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.security.scanner.MalwareScanner;
import com.interior.platform.media.security.scanner.ScanMetadata;
import com.interior.platform.media.security.scanner.ScanResult;
import com.interior.platform.security.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaSecurityScanService {

    private static final Logger log = LoggerFactory.getLogger(MediaSecurityScanService.class);

    private final MediaRepository mediaRepository;
    private final MalwareScanner malwareScanner;
    private final MediaSecurityProperties securityProperties;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public MediaSecurityScanService(
            MediaRepository mediaRepository,
            MalwareScanner malwareScanner,
            MediaSecurityProperties securityProperties,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.mediaRepository = mediaRepository;
        this.malwareScanner = malwareScanner;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes malware scanning and verifies compliance with commit-gate policy.
     * Throws an exception if the payload fails inspection or if required scanning fails closed.
     */
    @Transactional
    public ScanResult scanAndEnforce(UploadIntentRecord intent, byte[] rawBytes, UUID actorUserId) {
        String provider = malwareScanner.getProviderId();
        boolean isConfigured = malwareScanner.isConfigured();
        boolean requireClean = securityProperties.isRequireCleanScan();

        log.info("Processing media scan for intent {} (studio={}, provider={}, requireClean={})",
                intent.id(), intent.studioId(), provider, requireClean);

        // If scanning is disabled and NOT required, mark NOT_CONFIGURED or NOT_REQUIRED and pass
        if (!isConfigured && !requireClean) {
            log.info("Malware scanning not configured and not required for intent {}. Bypassing commit gate.", intent.id());
            mediaRepository.updateUploadIntentScanStatus(intent.id(), ScanStatus.NOT_CONFIGURED, provider, null);
            return ScanResult.unavailable(provider, "Malware scanner not configured, requireCleanScan=false");
        }

        // If required but scanner is not configured, fail-closed
        if (!isConfigured) {
            mediaRepository.updateUploadIntentScanStatus(intent.id(), ScanStatus.NOT_CONFIGURED, provider, null);
            auditService.record(
                    actorUserId,
                    intent.studioId(),
                    "MEDIA_SCAN_FAIL_CLOSED",
                    "UPLOAD_INTENT",
                    intent.id().toString(),
                    Map.of("reason", "MALWARE_SCANNER_NOT_CONFIGURED"),
                    null,
                    null
            );
            throw new IllegalStateException("MALWARE_SCANNER_NOT_CONFIGURED: Production upload security requires a configured malware scanner.");
        }

        // Execute scan
        ScanMetadata metadata = new ScanMetadata(
                intent.id(),
                intent.studioId(),
                intent.projectId(),
                intent.expectedContentType(),
                rawBytes.length
        );

        ScanResult result;
        try {
            result = malwareScanner.scan(intent.quarantineKey(), rawBytes, metadata);
        } catch (Exception e) {
            log.error("Scanner exception during inspection for intent {}: {}", intent.id(), e.getMessage(), e);
            result = ScanResult.error(provider, e.getMessage(), 0);
        }

        // Record scan ledger audit
        recordScanLedger(intent, result);

        // Handle outcomes
        if (result.outcome() == ScanOutcome.CLEAN) {
            mediaRepository.updateUploadIntentScanStatus(intent.id(), ScanStatus.CLEAN, result.provider(), null);
            return result;
        }

        if (result.outcome() == ScanOutcome.INFECTED) {
            String threat = result.threatClassification() != null ? result.threatClassification() : "GENERIC_MALWARE_DETECTED";
            mediaRepository.updateUploadIntentScanStatus(intent.id(), ScanStatus.INFECTED, result.provider(), threat);

            // Mark intent failed to release quota and prevent retry of malicious payload
            mediaRepository.updateUploadIntentStatus(intent.id(), UploadIntentStatus.FAILED);

            auditService.record(
                    actorUserId,
                    intent.studioId(),
                    "MEDIA_MALWARE_DETECTED",
                    "UPLOAD_INTENT",
                    intent.id().toString(),
                    Map.of(
                            "threat", threat,
                            "provider", result.provider(),
                            "quarantineKey", intent.quarantineKey()
                    ),
                    null,
                    null
            );

            log.warn("MALWARE DETECTED in intent {}: threat={}", intent.id(), threat);
            throw new BadRequestException("MALWARE_DETECTED: The uploaded file was flagged as malicious (" + threat + ") and rejected.");
        }

        // Handle errors or unavailable
        mediaRepository.updateUploadIntentScanStatus(intent.id(), ScanStatus.SCAN_FAILED, result.provider(), null);

        auditService.record(
                actorUserId,
                intent.studioId(),
                "MEDIA_SCAN_FAILED",
                "UPLOAD_INTENT",
                intent.id().toString(),
                Map.of(
                        "outcome", result.outcome().name(),
                        "error", result.errorMessage() != null ? result.errorMessage() : "unknown"
                ),
                null,
                null
        );

        if (requireClean) {
            throw new IllegalStateException("UPLOAD_SECURITY_SCAN_FAILED: Unable to verify upload safety with security scanner. (" + result.errorMessage() + ")");
        }

        return result;
    }

    private void recordScanLedger(UploadIntentRecord intent, ScanResult result) {
        try {
            String metadataJson = objectMapper.writeValueAsString(result.details() != null ? result.details() : Map.of());
            MediaSecurityScanRecord scanRecord = new MediaSecurityScanRecord(
                    UuidV7.randomUuid(),
                    intent.id(),
                    intent.studioId(),
                    intent.quarantineKey(),
                    result.provider(),
                    result.engineVersion(),
                    result.outcome(),
                    result.threatClassification(),
                    result.durationMs(),
                    Instant.now(),
                    result.errorMessage(),
                    metadataJson
            );
            mediaRepository.recordMediaSecurityScan(scanRecord);
        } catch (Exception e) {
            log.error("Failed to persist security scan audit record for intent {}: {}", intent.id(), e.getMessage());
        }
    }
}
