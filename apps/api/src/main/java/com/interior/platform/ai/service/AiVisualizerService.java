package com.interior.platform.ai.service;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobReferenceRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.AiReferenceMetadataRecord;
import com.interior.platform.ai.domain.AiUsageEventRecord;
import com.interior.platform.ai.domain.ReferencePurpose;
import com.interior.platform.ai.dto.*;
import com.interior.platform.ai.provider.AiGenerationReference;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.provider.ProviderGenerationResponse;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiVisualizerService {

    private static final Logger log = LoggerFactory.getLogger(AiVisualizerService.class);

    private final AiJobRepository aiJobRepository;
    private final AiReferenceRepository aiReferenceRepository;
    private final AiImageProvider aiImageProvider;
    private final AiVisualizerProperties properties;
    private final MediaRepository mediaRepository;
    private final ProjectRepository projectRepository;
    private final StudioRepository studioRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AiVisualizerService(
            AiJobRepository aiJobRepository,
            AiReferenceRepository aiReferenceRepository,
            AiImageProvider aiImageProvider,
            AiVisualizerProperties properties,
            MediaRepository mediaRepository,
            ProjectRepository projectRepository,
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            RateLimiterService rateLimiterService,
            StorageService storageService,
            ImageProcessingService imageProcessingService
    ) {
        this.aiJobRepository = aiJobRepository;
        this.aiReferenceRepository = aiReferenceRepository;
        this.aiImageProvider = aiImageProvider;
        this.properties = properties;
        this.mediaRepository = mediaRepository;
        this.projectRepository = projectRepository;
        this.studioRepository = studioRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public AiStudioStatusResponse getStudioStatus(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        boolean isConfigured = aiImageProvider.isConfigured();
        int dailyQuota = properties.getDailyStudioLimit();
        int usedToday = aiJobRepository.countTodayUsage(context.studioId());
        int remaining = Math.max(0, dailyQuota - usedToday);

        return new AiStudioStatusResponse(
                isConfigured,
                aiImageProvider.getProviderKey(),
                dailyQuota,
                usedToday,
                remaining,
                aiImageProvider.supportsReferenceImages(),
                aiImageProvider.getMaxReferenceImages()
        );
    }

    @Transactional
    public AiJobDetailResponse createGenerationJob(ActorContext actor, UUID requestedStudioId, CreateAiJobRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        // 1. Truthful provider check: Mock-free, fail-fast if unconfigured
        if (!aiImageProvider.isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }

        // 2. Short-term rate limit protection (10 submissions per minute per studio)
        rateLimiterService.acquire("ai_generate:" + studioId, 10, Duration.ofMinutes(1));

        // 3. Daily quota check
        int usedToday = aiJobRepository.countTodayUsage(studioId);
        if (usedToday >= properties.getDailyStudioLimit()) {
            throw new RateLimitExceededException("Daily AI concept generation quota reached (" + properties.getDailyStudioLimit() + " per day). Please try again tomorrow.");
        }

        // 4. Idempotency deduplication
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            String trimmedKey = request.idempotencyKey().trim();
            Optional<AiJobRecord> existing = aiJobRepository.findByIdempotencyKey(studioId, trimmedKey);
            if (existing.isPresent()) {
                return toJobDetail(existing.get(), studioId);
            }
        }

        // 5. Project ownership verification
        StudioProjectRecord project = projectRepository.findProjectById(studioId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        // 6. Input media verification
        MediaAssetRecord inputMedia = mediaRepository.findMediaAsset(request.inputMediaId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Input media asset not found in studio"));

        if (inputMedia.deletedAt() != null) {
            throw new ResourceNotFoundException("Input media asset has been deleted");
        }
        if (inputMedia.processingStatus() != MediaProcessingStatus.READY) {
            throw new BadRequestException("Input media asset is not ready for processing (status: " + inputMedia.processingStatus() + ")");
        }
        if (inputMedia.mediaType() == MediaType.CLIENT_PRIVATE) {
            throw new BadRequestException("Client confidential media assets cannot be processed by AI.");
        }
        if (!inputMedia.projectId().equals(project.id())) {
            throw new BadRequestException("Input media does not belong to the specified project.");
        }

        // 7. Reference images validation
        List<AiJobReferenceInput> refInputs = request.references() != null ? request.references() : List.of();
        if (!refInputs.isEmpty()) {
            if (!aiImageProvider.supportsReferenceImages()) {
                throw new BadRequestException("Configured AI provider does not support reference images");
            }
            if (refInputs.size() > properties.getMaxReferenceImages()) {
                throw new BadRequestException("Maximum of " + properties.getMaxReferenceImages() + " reference images allowed.");
            }
            for (AiJobReferenceInput ref : refInputs) {
                MediaAssetRecord refMedia = mediaRepository.findMediaAsset(ref.mediaId(), studioId)
                        .orElseThrow(() -> new ResourceNotFoundException("Reference media asset not found in studio: " + ref.mediaId()));
                if (refMedia.deletedAt() != null) {
                    throw new BadRequestException("Reference media asset has been deleted: " + ref.mediaId());
                }
                if (refMedia.processingStatus() != MediaProcessingStatus.READY) {
                    throw new BadRequestException("Reference media asset is not ready for processing (status: " + refMedia.processingStatus() + ")");
                }
                if (refMedia.mediaType() != MediaType.REFERENCE) {
                    throw new BadRequestException("Attached media must have media type REFERENCE: " + ref.mediaId());
                }
            }
        }

        // 8. Sanitize and validate prompt
        String prompt = request.prompt().trim();
        if (prompt.length() > properties.getMaxPromptLength()) {
            throw new BadRequestException("Prompt exceeds maximum allowed length of " + properties.getMaxPromptLength() + " characters.");
        }

        // 9. Create durable Job record (UUIDv7)
        UUID jobId = UuidV7.randomUuid();
        Instant now = Instant.now();
        String idempotencyKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey().trim()
                : null;
        boolean preserveStructure = request.preserveStructure() != null ? request.preserveStructure() : true;

        AiJobRecord job = new AiJobRecord(
                jobId,
                studioId,
                project.id(),
                inputMedia.id(),
                null,
                aiImageProvider.getProviderKey(),
                null,
                prompt,
                null,
                AiJobStatus.QUEUED,
                null,
                null,
                0,
                idempotencyKey,
                actor.userId(),
                now,
                null,
                null,
                null,
                null,
                0L,
                preserveStructure
        );

        aiJobRepository.createJob(job);

        // 10. Persist immutable reference snapshots
        if (!refInputs.isEmpty()) {
            List<AiJobReferenceRecord> snapshotList = new ArrayList<>();
            int order = 0;
            for (AiJobReferenceInput r : refInputs) {
                snapshotList.add(new AiJobReferenceRecord(
                        UuidV7.randomUuid(),
                        jobId,
                        studioId,
                        r.mediaId(),
                        r.purpose(),
                        r.label() != null ? r.label().trim() : null,
                        r.instruction() != null ? r.instruction().trim() : null,
                        r.displayOrder() > 0 ? r.displayOrder() : order++,
                        now
                ));
            }
            aiReferenceRepository.createJobReferences(snapshotList);
        }

        auditService.record(
                actor.userId(),
                studioId,
                "AI_GENERATION_SUBMITTED",
                "AI_JOB",
                jobId.toString(),
                Map.of(
                        "projectId", project.id(),
                        "inputMediaId", inputMedia.id(),
                        "preserveStructure", preserveStructure,
                        "referenceCount", refInputs.size()
                ),
                null,
                null
        );

        // 11. Dispatch async job execution
        executor.submit(() -> executeJobInternal(jobId));

        return toJobDetail(job, studioId);
    }

    public AiJobDetailResponse getJobDetail(ActorContext actor, UUID requestedStudioId, UUID jobId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiJobRecord job = aiJobRepository.findById(context.studioId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Job not found in studio"));

        return toJobDetail(job, context.studioId());
    }

    @Transactional
    public AiJobDetailResponse cancelJob(ActorContext actor, UUID requestedStudioId, UUID jobId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiJobRecord job = aiJobRepository.findById(context.studioId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Job not found in studio"));

        if (job.status() == AiJobStatus.SUCCEEDED || job.status() == AiJobStatus.FAILED || job.status() == AiJobStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel job in terminal state: " + job.status());
        }

        if (job.providerJobId() != null) {
            aiImageProvider.cancel(job.providerJobId());
        }

        Instant now = Instant.now();
        aiJobRepository.updateStatus(
                jobId,
                AiJobStatus.CANCELLED,
                null,
                null,
                now,
                "CANCELLED_BY_USER",
                "Job was cancelled by user.",
                null,
                null,
                job.version()
        );

        aiJobRepository.recordUsageEvent(new AiUsageEventRecord(
                UuidV7.randomUuid(),
                context.studioId(),
                jobId,
                "GENERATION_CANCELLED",
                aiImageProvider.getProviderKey(),
                0,
                now
        ));

        auditService.record(
                actor.userId(),
                context.studioId(),
                "AI_GENERATION_CANCELLED",
                "AI_JOB",
                jobId.toString(),
                Map.of("status", "CANCELLED"),
                null,
                null
        );

        AiJobRecord updated = aiJobRepository.findById(context.studioId(), jobId).orElse(job);
        return toJobDetail(updated, context.studioId());
    }

    public AiJobListResponse listJobs(ActorContext actor, UUID requestedStudioId, UUID projectId, int limit, int offset) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        int clampedLimit = Math.max(1, Math.min(limit, 100));
        int clampedOffset = Math.max(0, offset);

        List<AiJobRecord> jobs = aiJobRepository.findByStudio(context.studioId(), projectId, clampedLimit, clampedOffset);
        int total = aiJobRepository.countByStudio(context.studioId(), projectId);

        List<AiJobDetailResponse> items = jobs.stream()
                .map(j -> toJobDetail(j, context.studioId()))
                .toList();

        return new AiJobListResponse(items, total, clampedLimit, clampedOffset);
    }

    // ============================================================================
    // REFERENCE LIBRARY METHODS
    // ============================================================================

    @Transactional
    public ReferenceDetailResponse createReference(ActorContext actor, UUID requestedStudioId, CreateReferenceRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        MediaAssetRecord mediaAsset = mediaRepository.findMediaAsset(request.mediaId(), studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found in studio: " + request.mediaId()));

        if (mediaAsset.deletedAt() != null) {
            throw new BadRequestException("Media asset has been deleted: " + request.mediaId());
        }
        if (mediaAsset.processingStatus() != MediaProcessingStatus.READY) {
            throw new BadRequestException("Media asset is not ready for use as reference (status: " + mediaAsset.processingStatus() + ")");
        }
        if (mediaAsset.mediaType() != MediaType.REFERENCE) {
            throw new BadRequestException("Media asset must have media type REFERENCE: " + request.mediaId());
        }

        if (request.projectId() != null) {
            projectRepository.findProjectById(studioId, request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio: " + request.projectId()));
        }

        Optional<AiReferenceMetadataRecord> existing = aiReferenceRepository.findByMediaId(studioId, request.mediaId());
        if (existing.isPresent()) {
            AiReferenceMetadataRecord ex = existing.get();
            if (ex.archivedAt() == null) {
                throw new BadRequestException("Reference metadata already exists for this media asset.");
            }
            // Reactivate/update previously archived reference
            AiReferenceMetadataRecord reactivated = new AiReferenceMetadataRecord(
                    ex.id(),
                    ex.mediaId(),
                    studioId,
                    request.projectId(),
                    request.purpose(),
                    request.label() != null ? request.label().trim() : null,
                    request.defaultInstruction() != null ? request.defaultInstruction().trim() : null,
                    ex.createdAt(),
                    Instant.now(),
                    null
            );
            aiReferenceRepository.updateReference(reactivated);
            return toReferenceDetail(reactivated, studioId);
        }

        Instant now = Instant.now();
        AiReferenceMetadataRecord record = new AiReferenceMetadataRecord(
                UuidV7.randomUuid(),
                request.mediaId(),
                studioId,
                request.projectId(),
                request.purpose(),
                request.label() != null ? request.label().trim() : null,
                request.defaultInstruction() != null ? request.defaultInstruction().trim() : null,
                now,
                now,
                null
        );

        aiReferenceRepository.createReference(record);

        auditService.record(
                actor.userId(),
                studioId,
                "AI_REFERENCE_CREATED",
                "AI_REFERENCE",
                record.id().toString(),
                Map.of("mediaId", record.mediaId(), "purpose", record.purpose().name()),
                null,
                null
        );

        return toReferenceDetail(record, studioId);
    }

    public List<ReferenceDetailResponse> listReferences(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            ReferencePurpose purpose,
            boolean includeArchived
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        List<AiReferenceMetadataRecord> list = aiReferenceRepository.findByStudio(studioId, projectId, purpose, includeArchived);
        return list.stream().map(r -> toReferenceDetail(r, studioId)).toList();
    }

    public ReferenceDetailResponse getReference(ActorContext actor, UUID requestedStudioId, UUID referenceId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        AiReferenceMetadataRecord record = aiReferenceRepository.findById(studioId, referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found in studio"));

        return toReferenceDetail(record, studioId);
    }

    @Transactional
    public ReferenceDetailResponse updateReference(
            ActorContext actor,
            UUID requestedStudioId,
            UUID referenceId,
            UpdateReferenceRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        AiReferenceMetadataRecord existing = aiReferenceRepository.findById(studioId, referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found in studio"));

        if (request.projectId() != null) {
            projectRepository.findProjectById(studioId, request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio: " + request.projectId()));
        }

        ReferencePurpose newPurpose = request.purpose() != null ? request.purpose() : existing.purpose();
        String newLabel = request.label() != null ? request.label().trim() : existing.label();
        String newInstruction = request.defaultInstruction() != null ? request.defaultInstruction().trim() : existing.defaultInstruction();
        UUID newProjectId = request.projectId() != null ? request.projectId() : existing.projectId();

        AiReferenceMetadataRecord updated = new AiReferenceMetadataRecord(
                existing.id(),
                existing.mediaId(),
                existing.studioId(),
                newProjectId,
                newPurpose,
                newLabel,
                newInstruction,
                existing.createdAt(),
                Instant.now(),
                existing.archivedAt()
        );

        aiReferenceRepository.updateReference(updated);

        auditService.record(
                actor.userId(),
                studioId,
                "AI_REFERENCE_UPDATED",
                "AI_REFERENCE",
                referenceId.toString(),
                Map.of("purpose", updated.purpose().name()),
                null,
                null
        );

        return toReferenceDetail(updated, studioId);
    }

    @Transactional
    public void archiveReference(ActorContext actor, UUID requestedStudioId, UUID referenceId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        aiReferenceRepository.findById(studioId, referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found in studio"));

        aiReferenceRepository.archiveReference(studioId, referenceId, Instant.now());

        auditService.record(
                actor.userId(),
                studioId,
                "AI_REFERENCE_ARCHIVED",
                "AI_REFERENCE",
                referenceId.toString(),
                Map.of("archived", true),
                null,
                null
        );
    }

    // ============================================================================
    // EXECUTION PIPELINE
    // ============================================================================

    public void executeJobInternal(UUID jobId) {
        Optional<AiJobRecord> jobOpt = aiJobRepository.findByIdGlobal(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }

        AiJobRecord job = jobOpt.get();
        if (job.status() != AiJobStatus.QUEUED) {
            return;
        }

        Instant startedAt = Instant.now();
        long currentVersion = job.version();
        aiJobRepository.updateStatus(jobId, AiJobStatus.PROCESSING, startedAt, null, null, null, null, null, null, currentVersion);
        currentVersion++;

        // Load input media asset
        Optional<MediaAssetRecord> inputMediaOpt = mediaRepository.findMediaAsset(job.inputMediaId(), job.studioId());
        if (inputMediaOpt.isEmpty()) {
            aiJobRepository.updateStatus(jobId, AiJobStatus.FAILED, null, null, Instant.now(), "INPUT_NOT_FOUND", "Input media asset was not found.", null, null, currentVersion);
            return;
        }

        MediaAssetRecord inputMedia = inputMediaOpt.get();
        byte[] inputBytes;
        try {
            inputBytes = storageService.load(inputMedia.originalStorageKey());
            if (inputBytes == null || inputBytes.length == 0) {
                throw new IllegalStateException("Input image content is empty");
            }
        } catch (Exception e) {
            log.error("Failed to load input media content for AI job {}: {}", jobId, e.getMessage());
            aiJobRepository.updateStatus(jobId, AiJobStatus.FAILED, null, null, Instant.now(), "INPUT_LOAD_FAILED", "Failed to load input image data.", null, null, currentVersion);
            return;
        }

        // Load snapshot references for this job
        List<AiJobReferenceRecord> jobRefs = aiReferenceRepository.findReferencesByJobId(jobId);
        List<AiGenerationReference> generationRefs = new ArrayList<>();
        for (AiJobReferenceRecord ref : jobRefs) {
            try {
                Optional<MediaAssetRecord> refAssetOpt = mediaRepository.findMediaAsset(ref.mediaId(), job.studioId());
                if (refAssetOpt.isPresent()) {
                    MediaAssetRecord refAsset = refAssetOpt.get();
                    byte[] refBytes = storageService.load(refAsset.originalStorageKey());
                    if (refBytes != null && refBytes.length > 0) {
                        generationRefs.add(new AiGenerationReference(
                                ref.mediaId(),
                                ref.purposeSnapshot(),
                                ref.labelSnapshot(),
                                ref.instructionSnapshot(),
                                refBytes,
                                refAsset.contentType()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load reference image {} for job {}: {}", ref.mediaId(), jobId, e.getMessage());
            }
        }

        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        ProviderGenerationResponse providerResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            aiJobRepository.incrementAttemptCount(jobId);
            try {
                providerResponse = aiImageProvider.submitGeneration(job, inputBytes, inputMedia.contentType(), generationRefs);
                if (providerResponse.isSuccess() || !providerResponse.isRetryable()) {
                    break;
                }
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(Math.min(500L * attempt, 2000L));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("AI generation attempt {} failed for job {}: {}", attempt, jobId, e.getMessage());
                providerResponse = ProviderGenerationResponse.failure("PROVIDER_CALL_ERROR", "Provider call failed: " + e.getMessage());
            }
        }

        if (providerResponse == null || !providerResponse.isSuccess()) {
            String errCode = providerResponse != null ? providerResponse.errorCode() : "PROVIDER_ERROR";
            String errMsg = providerResponse != null ? providerResponse.errorMessage() : "Generation failed.";
            aiJobRepository.updateStatus(jobId, AiJobStatus.FAILED, null, null, Instant.now(), errCode, errMsg, null, null, currentVersion);
            aiJobRepository.recordUsageEvent(new AiUsageEventRecord(UuidV7.randomUuid(), job.studioId(), jobId, "GENERATION_FAILED", aiImageProvider.getProviderKey(), 0, Instant.now()));
            auditService.record(job.createdBy(), job.studioId(), "AI_GENERATION_FAILED", "AI_JOB", jobId.toString(), Map.of("errorCode", errCode), null, null);
            return;
        }

        // Ingest generated image into Media Engine
        try {
            byte[] outputBytes = providerResponse.imageBytes();
            ImageProcessingService.ImageDimensions dims = imageProcessingService.validateAndGetDimensions(outputBytes);

            UUID outputMediaId = UuidV7.randomUuid();
            String originalStorageKey = storageService.generateCanonicalOriginalKey(
                    job.studioId(),
                    job.projectId(),
                    outputMediaId,
                    "image/jpeg"
            );
            storageService.store(originalStorageKey, outputBytes, "image/jpeg");

            Instant now = Instant.now();
            MediaAssetRecord outputAsset = new MediaAssetRecord(
                    outputMediaId,
                    job.studioId(),
                    job.projectId(),
                    MediaType.AI_CONCEPT,
                    MediaVisibility.PRIVATE,
                    MediaProcessingStatus.PROCESSING,
                    originalStorageKey,
                    "image/jpeg",
                    outputBytes.length,
                    dims.width(),
                    dims.height(),
                    0,
                    false,
                    "AI Concept Visualization: " + job.prompt(),
                    "AI Concept Visualization",
                    true,
                    job.createdBy(),
                    now,
                    now,
                    null
            );
            mediaRepository.createMediaAsset(outputAsset);

            // Generate private preview derivative with mandatory AI Concept disclosure badge (no public derivatives)
            StudioWatermarkSettingsRecord watermarkSettings = getOrCreateWatermarkSettings(job.studioId());
            ImageProcessingService.ProcessedDerivative pd = imageProcessingService.createDerivative(
                    outputBytes,
                    DerivativeVariant.MEDIUM,
                    watermarkSettings,
                    true,
                    MediaType.AI_CONCEPT
            );
            String previewKey = "studio/" + job.studioId() + "/previews/" + outputMediaId + ".jpg";
            storageService.store(previewKey, pd.content(), "image/jpeg");

            // Mark asset ready
            MediaAssetRecord readyAsset = new MediaAssetRecord(
                    outputAsset.id(),
                    outputAsset.studioId(),
                    outputAsset.projectId(),
                    outputAsset.mediaType(),
                    outputAsset.visibility(),
                    MediaProcessingStatus.READY,
                    outputAsset.originalStorageKey(),
                    outputAsset.contentType(),
                    outputAsset.fileSize(),
                    outputAsset.width(),
                    outputAsset.height(),
                    outputAsset.sortOrder(),
                    outputAsset.isCover(),
                    outputAsset.altText(),
                    outputAsset.caption(),
                    outputAsset.watermarkEnabled(),
                    outputAsset.createdBy(),
                    outputAsset.createdAt(),
                    now,
                    null
            );
            mediaRepository.updateMediaAsset(readyAsset);

            // Mark job SUCCEEDED
            aiJobRepository.updateStatus(
                    jobId,
                    AiJobStatus.SUCCEEDED,
                    null,
                    now,
                    null,
                    null,
                    null,
                    outputMediaId,
                    providerResponse.metadataJson(),
                    currentVersion
            );

            // Record usage & audit
            aiJobRepository.recordUsageEvent(new AiUsageEventRecord(
                    UuidV7.randomUuid(),
                    job.studioId(),
                    jobId,
                    "GENERATION_SUCCESS",
                    aiImageProvider.getProviderKey(),
                    1,
                    now
            ));

            auditService.record(
                    job.createdBy(),
                    job.studioId(),
                    "AI_GENERATION_SUCCEEDED",
                    "AI_JOB",
                    jobId.toString(),
                    Map.of("outputMediaId", outputMediaId),
                    null,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to ingest generated AI image for job {}: {}", jobId, e.getMessage(), e);
            aiJobRepository.updateStatus(jobId, AiJobStatus.FAILED, null, null, Instant.now(), "INGESTION_ERROR", "Failed to store generated visualization.", null, null, currentVersion);
            aiJobRepository.recordUsageEvent(new AiUsageEventRecord(UuidV7.randomUuid(), job.studioId(), jobId, "GENERATION_FAILED", aiImageProvider.getProviderKey(), 0, Instant.now()));
            auditService.record(job.createdBy(), job.studioId(), "AI_GENERATION_FAILED", "AI_JOB", jobId.toString(), Map.of("error", e.getMessage()), null, null);
        }
    }

    private AiJobDetailResponse toJobDetail(AiJobRecord job, UUID studioId) {
        String inputPreviewUrl = resolvePreviewUrl(job.inputMediaId(), studioId);
        String outputPreviewUrl = job.outputMediaId() != null ? resolvePreviewUrl(job.outputMediaId(), studioId) : null;

        List<AiJobReferenceRecord> jobRefs = aiReferenceRepository.findReferencesByJobIdAndStudio(studioId, job.id());
        List<AiJobReferenceResponse> refResponses = jobRefs.stream().map(r -> new AiJobReferenceResponse(
                r.id(),
                r.mediaId(),
                resolvePreviewUrl(r.mediaId(), studioId),
                r.purposeSnapshot(),
                r.purposeSnapshot().getDisplayName(),
                r.labelSnapshot(),
                r.instructionSnapshot(),
                r.displayOrder(),
                r.createdAt()
        )).toList();

        return new AiJobDetailResponse(
                job.id(),
                job.studioId(),
                job.projectId(),
                job.inputMediaId(),
                inputPreviewUrl,
                job.outputMediaId(),
                outputPreviewUrl,
                job.providerKey(),
                job.prompt(),
                job.status(),
                job.errorCode(),
                job.errorMessageSafe(),
                job.createdAt(),
                job.startedAt(),
                job.completedAt(),
                job.failedAt(),
                job.preserveStructure(),
                refResponses
        );
    }

    private ReferenceDetailResponse toReferenceDetail(AiReferenceMetadataRecord record, UUID studioId) {
        String previewUrl = resolvePreviewUrl(record.mediaId(), studioId);
        return new ReferenceDetailResponse(
                record.id(),
                record.mediaId(),
                record.studioId(),
                record.projectId(),
                record.purpose(),
                record.purpose().getDisplayName(),
                record.label(),
                record.defaultInstruction(),
                previewUrl,
                record.createdAt(),
                record.updatedAt(),
                record.archivedAt()
        );
    }

    private String resolvePreviewUrl(UUID mediaId, UUID studioId) {
        if (mediaId == null) return null;
        Optional<MediaAssetRecord> assetOpt = mediaRepository.findMediaAsset(mediaId, studioId);
        if (assetOpt.isEmpty()) {
            return null;
        }
        MediaAssetRecord asset = assetOpt.get();

        // Safe private preview for all PRIVATE assets (including AI concepts and references)
        if (asset.visibility() == MediaVisibility.PRIVATE) {
            return "/api/v1/media/" + mediaId + "/preview";
        }

        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(mediaId, studioId);
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.MEDIUM) return d.publicUrl();
        }
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.LARGE) return d.publicUrl();
        }
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.THUMBNAIL) return d.publicUrl();
        }
        return "/api/v1/media/" + mediaId + "/preview";
    }

    private StudioWatermarkSettingsRecord getOrCreateWatermarkSettings(UUID studioId) {
        return mediaRepository.findWatermarkSettings(studioId).orElseGet(() -> {
            String fallback = resolveStudioName(studioId);
            return StudioWatermarkSettingsRecord.defaultForStudio(studioId, fallback);
        });
    }

    private String resolveStudioName(UUID studioId) {
        return studioRepository.findStudioById(studioId)
                .map(StudioDetailRecord::name)
                .orElse("Interior Studio");
    }

    private UserRecord validateActiveUser(UUID userId) {
        UserRecord user = securityRepository.findUserById(userId)
                .orElseThrow(() -> new AccessDeniedException("User account not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Account is suspended or inactive");
        }
        return user;
    }

    private void validateProfessionalRole(ActorContext actor) {
        if (!actor.hasRole("DESIGNER") && !actor.hasRole("DESIGNER_TEAM") &&
            !actor.hasRole("SUPER_ADMIN") && !actor.hasRole("ADMIN")) {
            throw new AccessDeniedException("Professional onboarding required to access AI Visualizer");
        }
    }

    private ResolvedStudioContext resolveStudioContext(ActorContext actor, UUID requestedStudioId) {
        List<StudioMemberRecord> memberships = securityRepository.getStudioMemberships(actor.userId());
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("No studio membership found for account");
        }

        if (requestedStudioId != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(requestedStudioId))
                    .findFirst();
            if (match.isEmpty()) {
                throw new AccessDeniedException("Access denied: you are not a member of the requested studio");
            }
            return new ResolvedStudioContext(match.get().studioId(), match.get().role());
        }

        if (actor.activeStudioId() != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(actor.activeStudioId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedStudioContext(match.get().studioId(), match.get().role());
            }
        }

        StudioMemberRecord primary = memberships.get(0);
        return new ResolvedStudioContext(primary.studioId(), primary.role());
    }

    private record ResolvedStudioContext(UUID studioId, String role) {}
}
