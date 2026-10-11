package com.interior.platform.operations.service;

import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.media.security.scanner.MalwareScanner;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.operations.domain.*;
import com.interior.platform.operations.dto.*;
import com.interior.platform.operations.repository.PlatformOperationsRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.mfa.MfaService;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PlatformOperationsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformOperationsService.class);

    private final PlatformOperationsRepository operationsRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final MfaService mfaService;
    private final StorageService storageService;
    private final TransactionalEmailService transactionalEmailService;
    private final AiImageProvider aiImageProvider;
    private final MalwareScanner malwareScanner;

    public PlatformOperationsService(
            PlatformOperationsRepository operationsRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            JdbcTemplate jdbcTemplate,
            @Autowired(required = false) MfaService mfaService,
            @Autowired(required = false) StorageService storageService,
            @Autowired(required = false) TransactionalEmailService transactionalEmailService,
            @Autowired(required = false) AiImageProvider aiImageProvider,
            @Autowired(required = false) MalwareScanner malwareScanner
    ) {
        this.operationsRepository = operationsRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.mfaService = mfaService;
        this.storageService = storageService;
        this.transactionalEmailService = transactionalEmailService;
        this.aiImageProvider = aiImageProvider;
        this.malwareScanner = malwareScanner;
    }

    public PlatformOperationalStateRecord getOperationalState() {
        return operationsRepository.getOperationalState().orElseGet(() ->
                new PlatformOperationalStateRecord("GLOBAL", "NORMAL", null, true, true, true, null, Instant.now())
        );
    }

    public boolean isMaintenanceReadOnly() {
        return "READ_ONLY".equalsIgnoreCase(getOperationalState().maintenanceMode());
    }

    public boolean isUploadsEnabled() {
        return getOperationalState().uploadsEnabled();
    }

    public boolean isAiEnabled() {
        return getOperationalState().aiEnabled();
    }

    public boolean isRegistrationsEnabled() {
        return getOperationalState().registrationsEnabled();
    }

    @Transactional
    public PlatformOperationalStateRecord updateOperationalState(
            ActorContext actor,
            SessionRecord session,
            UpdateOperationalStateRequest req,
            String ip,
            String ua
    ) {
        authorizationService.requirePlatformRole(actor, "SUPER_ADMIN");

        if (mfaService != null) {
            mfaService.requireFreshStepUp(actor, session);
        }

        operationsRepository.updateOperationalState(
                req.maintenanceMode(),
                req.maintenanceMessage(),
                req.uploadsEnabled(),
                req.aiEnabled(),
                req.registrationsEnabled(),
                actor.userId()
        );

        auditService.record(
                actor.userId(),
                null,
                "ADMIN_OPERATIONAL_STATE_UPDATED",
                "PLATFORM_OPERATIONAL_STATE",
                "GLOBAL",
                Map.of(
                        "maintenanceMode", String.valueOf(req.maintenanceMode()),
                        "uploadsEnabled", String.valueOf(req.uploadsEnabled()),
                        "aiEnabled", String.valueOf(req.aiEnabled()),
                        "registrationsEnabled", String.valueOf(req.registrationsEnabled())
                ),
                ip,
                ua
        );

        return getOperationalState();
    }

    public ProviderMatrixResponse getProviderMatrix(ActorContext actor) {
        authorizationService.requirePlatformRole(actor, "ADMIN");

        Map<String, ProviderMatrixResponse.ProviderStatusDetail> matrix = new LinkedHashMap<>();

        // 1. Database
        ProviderOperationalStatus dbStatus;
        String dbDetails;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbStatus = ProviderOperationalStatus.AVAILABLE;
            dbDetails = "Connected and answering queries";
        } catch (Exception e) {
            dbStatus = ProviderOperationalStatus.UNAVAILABLE;
            dbDetails = "Connection check failed: " + e.getMessage();
        }
        matrix.put("database", new ProviderMatrixResponse.ProviderStatusDetail(dbStatus, "PostgreSQL", dbDetails));

        // 2. Storage
        ProviderOperationalStatus storageStatus = ProviderOperationalStatus.NOT_CONFIGURED;
        String storageName = "LOCAL_DISK";
        String storageDetails = "Development local disk storage adapter active";
        if (storageService != null) {
            storageStatus = ProviderOperationalStatus.AVAILABLE;
            storageName = storageService.getClass().getSimpleName();
        }
        matrix.put("storage", new ProviderMatrixResponse.ProviderStatusDetail(storageStatus, storageName, storageDetails));

        // 3. Email
        ProviderOperationalStatus emailStatus = ProviderOperationalStatus.NOT_CONFIGURED;
        String emailProvider = "NONE";
        String emailDetails = "Transactional email provider credentials not configured";
        if (transactionalEmailService != null && transactionalEmailService.isConfigured()) {
            emailStatus = ProviderOperationalStatus.AVAILABLE;
            emailProvider = transactionalEmailService.getProviderName();
            emailDetails = "Configured and active";
        }
        matrix.put("email", new ProviderMatrixResponse.ProviderStatusDetail(emailStatus, emailProvider, emailDetails));

        // 4. AI Image Visualizer
        ProviderOperationalStatus aiStatus = ProviderOperationalStatus.NOT_CONFIGURED;
        String aiName = "NONE";
        String aiDetails = "AI generation provider not configured";
        if (aiImageProvider != null) {
            aiName = aiImageProvider.getClass().getSimpleName();
            if (aiImageProvider.isConfigured()) {
                aiStatus = isAiEnabled() ? ProviderOperationalStatus.AVAILABLE : ProviderOperationalStatus.DISABLED;
                aiDetails = isAiEnabled() ? "Configured and active" : "Globally disabled by platform administration";
            }
        }
        matrix.put("ai", new ProviderMatrixResponse.ProviderStatusDetail(aiStatus, aiName, aiDetails));

        // 5. Malware Scanner
        ProviderOperationalStatus scannerStatus = ProviderOperationalStatus.NOT_CONFIGURED;
        String scannerName = "NONE";
        String scannerDetails = "Malware scanner not configured";
        if (malwareScanner != null) {
            scannerName = malwareScanner.getProviderId();
            if (malwareScanner.isConfigured()) {
                scannerStatus = ProviderOperationalStatus.AVAILABLE;
                scannerDetails = "Configured and operational";
            }
        }
        matrix.put("malwareScanner", new ProviderMatrixResponse.ProviderStatusDetail(scannerStatus, scannerName, scannerDetails));

        // 6. Commercial Payment Gateway
        matrix.put("payment", new ProviderMatrixResponse.ProviderStatusDetail(
                ProviderOperationalStatus.NOT_CONFIGURED,
                "Stripe",
                "Live payment keys pending Phase 11 production provisioning"
        ));

        return new ProviderMatrixResponse(matrix, Instant.now());
    }

    public List<OperationalJobRunRecord> listJobRuns(ActorContext actor, int limit, int offset, String jobKey) {
        authorizationService.requirePlatformRole(actor, "ADMIN");
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);
        return operationsRepository.listJobRuns(boundedLimit, boundedOffset, jobKey);
    }
}
