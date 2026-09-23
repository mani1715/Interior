package com.interior.platform.ai.service;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.*;
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

import com.interior.platform.ai.domain.ClientReviewDecisionType;
import com.interior.platform.ai.domain.ClientReviewStatus;
import com.interior.platform.ai.domain.CommentAuthorType;
import com.interior.platform.ai.domain.VariationStrategy;
import com.interior.platform.ai.repository.AiClientReviewRepository;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AiVisualizerService {

    private static final Logger log = LoggerFactory.getLogger(AiVisualizerService.class);

    private final AiJobRepository aiJobRepository;
    private final AiReferenceRepository aiReferenceRepository;
    private final AiClientReviewRepository aiClientReviewRepository;
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

    @org.springframework.beans.factory.annotation.Autowired
    public AiVisualizerService(
            AiJobRepository aiJobRepository,
            AiReferenceRepository aiReferenceRepository,
            AiClientReviewRepository aiClientReviewRepository,
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
        this.aiClientReviewRepository = aiClientReviewRepository;
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
        this(
                aiJobRepository,
                aiReferenceRepository,
                null,
                aiImageProvider,
                properties,
                mediaRepository,
                projectRepository,
                studioRepository,
                securityRepository,
                authorizationService,
                auditService,
                rateLimiterService,
                storageService,
                imageProcessingService
        );
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
                aiImageProvider.getMaxReferenceImages(),
                aiImageProvider.supportsMaskEditing()
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

        // 9. Precision editing mode validation
        EditingMode mode = request.editingMode() != null ? request.editingMode() : EditingMode.FULL_IMAGE;
        String maskStorageKey = request.maskStorageKey() != null ? request.maskStorageKey().trim() : null;

        if (mode == EditingMode.PRECISION_MASK) {
            if (!aiImageProvider.supportsMaskEditing()) {
                throw new BadRequestException("Configured AI provider does not support precision mask editing.");
            }
            if (maskStorageKey == null || maskStorageKey.isBlank()) {
                throw new BadRequestException("maskStorageKey is required for precision mask editing mode.");
            }
            String expectedPrefix = "studio/" + studioId + "/masks/";
            if (!maskStorageKey.startsWith(expectedPrefix) || maskStorageKey.contains("..") || !maskStorageKey.matches("^studio/[a-f0-9\\-]+/masks/[a-f0-9\\-]+\\.png$")) {
                throw new BadRequestException("Invalid maskStorageKey. Mask must belong to the current studio.");
            }
        } else {
            maskStorageKey = null;
        }

        // 10. Create durable Job record (UUIDv7)
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
                preserveStructure,
                mode,
                maskStorageKey
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

        // Load precision mask bytes if present
        byte[] maskBytes = null;
        String maskContentType = null;
        if (job.editingMode() == EditingMode.PRECISION_MASK && job.maskStorageKey() != null) {
            try {
                maskBytes = storageService.load(job.maskStorageKey());
                if (maskBytes == null || maskBytes.length == 0) {
                    throw new IllegalStateException("Mask image content is empty");
                }
                maskContentType = "image/png";
            } catch (Exception e) {
                log.error("Failed to load mask content for AI job {}: {}", jobId, e.getMessage());
                aiJobRepository.updateStatus(jobId, AiJobStatus.FAILED, null, null, Instant.now(), "MASK_LOAD_FAILED", "Failed to load mask image data.", null, null, currentVersion);
                return;
            }
        }

        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        ProviderGenerationResponse providerResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            aiJobRepository.incrementAttemptCount(jobId);
            try {
                if (maskBytes != null) {
                    providerResponse = aiImageProvider.submitGeneration(job, inputBytes, inputMedia.contentType(), maskBytes, maskContentType, generationRefs);
                } else {
                    providerResponse = aiImageProvider.submitGeneration(job, inputBytes, inputMedia.contentType(), generationRefs);
                }
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

        String maskPreviewUrl = (job.editingMode() == EditingMode.PRECISION_MASK && job.maskStorageKey() != null)
                ? "/api/ai/jobs/" + job.id() + "/mask"
                : null;

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
                refResponses,
                job.editingMode() != null ? job.editingMode() : EditingMode.FULL_IMAGE,
                maskPreviewUrl,
                job.parentJobId(),
                job.rootJobId(),
                job.isShortlisted(),
                job.isStudioSelected(),
                job.conceptLabel()
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

    // ============================================================================
    // PRECISION MASK EDITING OPERATIONS
    // ============================================================================

    public UploadMaskResponse uploadMask(ActorContext actor, UUID requestedStudioId, UUID inputMediaId, org.springframework.web.multipart.MultipartFile file) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        if (inputMediaId == null) {
            throw new BadRequestException("inputMediaId is required");
        }

        MediaAssetRecord inputMedia = mediaRepository.findMediaAsset(inputMediaId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Input media asset not found in studio"));

        if (inputMedia.deletedAt() != null) {
            throw new ResourceNotFoundException("Input media asset has been deleted");
        }
        if (inputMedia.processingStatus() != MediaProcessingStatus.READY) {
            throw new BadRequestException("Input media asset is not ready for processing (status: " + inputMedia.processingStatus() + ")");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Mask file cannot be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Mask file exceeds maximum allowed size of 10MB");
        }

        byte[] maskBytes;
        try {
            maskBytes = file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("Failed to read mask file bytes: " + e.getMessage());
        }

        java.awt.image.BufferedImage img;
        try {
            img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(maskBytes));
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse mask image: " + e.getMessage());
        }

        if (img == null) {
            throw new BadRequestException("Invalid mask image format. Must be a valid PNG image.");
        }

        // Dimension validation: if inputMedia has dimensions, check that mask dimensions match
        if (inputMedia.width() > 0 && inputMedia.height() > 0) {
            if (img.getWidth() != inputMedia.width() || img.getHeight() != inputMedia.height()) {
                throw new BadRequestException(
                        "Mask dimensions (" + img.getWidth() + "x" + img.getHeight() +
                        ") must match input media dimensions (" + inputMedia.width() + "x" + inputMedia.height() + ")"
                );
            }
        }

        // Coverage and emptiness validation
        int selectedPixels = 0;
        int totalPixels = img.getWidth() * img.getHeight();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                if (a > 20 && (r > 20 || g > 20 || b > 20)) {
                    selectedPixels++;
                }
            }
        }

        if (selectedPixels == 0) {
            throw new BadRequestException("The mask is empty. Please select an area to edit.");
        }

        double coverageRatio = (double) selectedPixels / totalPixels;
        if (coverageRatio > 0.98) {
            throw new BadRequestException("The selected area covers almost the entire image (>98%). Please use Full Concept mode instead.");
        }

        UUID maskId = UuidV7.randomUuid();
        String maskStorageKey = "studio/" + studioId + "/masks/" + maskId + ".png";
        storageService.store(maskStorageKey, maskBytes, "image/png");

        return new UploadMaskResponse(maskId, maskStorageKey, img.getWidth(), img.getHeight(), coverageRatio);
    }

    public byte[] getJobMask(ActorContext actor, UUID requestedStudioId, UUID jobId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiJobRecord job = aiJobRepository.findById(context.studioId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI Job not found"));

        if (job.editingMode() != EditingMode.PRECISION_MASK || job.maskStorageKey() == null) {
            throw new ResourceNotFoundException("No mask associated with this job");
        }

        String expectedPrefix = "studio/" + context.studioId() + "/masks/";
        if (!job.maskStorageKey().startsWith(expectedPrefix)) {
            throw new AccessDeniedException("Access to this mask is denied");
        }

        byte[] maskBytes = storageService.load(job.maskStorageKey());
        if (maskBytes == null || maskBytes.length == 0) {
            throw new ResourceNotFoundException("Mask file not found in storage");
        }
        return maskBytes;
    }

    // =========================================================================
    // Phase 24: AI Variations, History Lineage & Shortlisting
    // =========================================================================

    public AiJobDetailResponse createVariation(ActorContext actor, UUID requestedStudioId, UUID parentJobId, CreateVariationRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        if (!aiImageProvider.isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }

        rateLimiterService.acquire("ai_generate:" + studioId, 10, Duration.ofMinutes(1));
        int usedToday = aiJobRepository.countTodayUsage(studioId);
        if (usedToday >= properties.getDailyStudioLimit()) {
            throw new RateLimitExceededException("Daily AI concept generation quota reached (" + properties.getDailyStudioLimit() + " per day). Please try again tomorrow.");
        }

        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<AiJobRecord> existing = aiJobRepository.findByIdempotencyKey(studioId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return toJobDetail(existing.get(), studioId);
            }
        }

        AiJobRecord parent = aiJobRepository.findById(studioId, parentJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent generation job not found"));

        if (parent.status() != AiJobStatus.SUCCEEDED) {
            throw new BadRequestException("Cannot create a variation from a job that has not succeeded");
        }

        UUID rootJobId = parent.rootJobId() != null ? parent.rootJobId() : parent.id();

        VariationStrategy strategy = request.resolvedStrategy();
        UUID inputMediaId;
        if (strategy == VariationStrategy.EVOLVE_CONCEPT) {
            if (parent.outputMediaId() == null) {
                throw new BadRequestException("Cannot evolve concept: parent job does not have an output image");
            }
            inputMediaId = parent.outputMediaId();
        } else {
            inputMediaId = parent.inputMediaId();
        }

        String prompt = (request.prompt() != null && !request.prompt().isBlank())
                ? request.prompt().trim()
                : parent.prompt();
        if (prompt.length() > properties.getMaxPromptLength()) {
            throw new BadRequestException("Prompt exceeds maximum allowed length of " + properties.getMaxPromptLength() + " characters.");
        }

        boolean preserveStructure = request.preserveStructure() != null
                ? request.preserveStructure()
                : parent.preserveStructure();

        EditingMode mode = request.editingMode() != null ? request.editingMode() : parent.editingMode();
        String maskStorageKey = null;

        if (mode == EditingMode.PRECISION_MASK) {
            if (!aiImageProvider.supportsMaskEditing()) {
                throw new BadRequestException("Configured AI provider does not support precision mask editing");
            }
            if (Boolean.TRUE.equals(request.reuseParentMask())) {
                if (parent.editingMode() != EditingMode.PRECISION_MASK || parent.maskStorageKey() == null) {
                    throw new BadRequestException("Parent job has no precision mask to reuse");
                }
                maskStorageKey = parent.maskStorageKey();
            } else if (request.newMaskSourceJobId() != null) {
                AiJobRecord maskSource = aiJobRepository.findById(studioId, request.newMaskSourceJobId())
                        .orElseThrow(() -> new ResourceNotFoundException("Specified mask source job not found"));
                if (maskSource.maskStorageKey() == null) {
                    throw new BadRequestException("Specified mask source job does not have a mask");
                }
                maskStorageKey = maskSource.maskStorageKey();
            } else {
                throw new BadRequestException("Precision edit variation requires either reusing parent mask or specifying a valid mask");
            }
        }

        List<AiJobReferenceInput> refInputs = request.references();
        List<AiJobReferenceRecord> refsToSnapshot = new ArrayList<>();
        Instant now = Instant.now();
        UUID newJobId = UuidV7.randomUuid();

        if (refInputs != null) {
            if (!refInputs.isEmpty()) {
                if (!aiImageProvider.supportsReferenceImages()) {
                    throw new BadRequestException("Configured AI provider does not support reference images");
                }
                if (refInputs.size() > properties.getMaxReferenceImages()) {
                    throw new BadRequestException("Maximum of " + properties.getMaxReferenceImages() + " reference images allowed.");
                }
                int order = 0;
                for (AiJobReferenceInput ref : refInputs) {
                    MediaAssetRecord refMedia = mediaRepository.findMediaAsset(ref.mediaId(), studioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Reference media asset not found in studio: " + ref.mediaId()));
                    if (refMedia.deletedAt() != null || refMedia.processingStatus() != MediaProcessingStatus.READY) {
                        throw new BadRequestException("Reference media asset is not ready: " + ref.mediaId());
                    }
                    if (refMedia.mediaType() != MediaType.REFERENCE) {
                        throw new BadRequestException("Attached media must have media type REFERENCE: " + ref.mediaId());
                    }
                    refsToSnapshot.add(new AiJobReferenceRecord(
                            UuidV7.randomUuid(),
                            newJobId,
                            studioId,
                            ref.mediaId(),
                            ref.purpose(),
                            ref.label() != null ? ref.label().trim() : null,
                            ref.instruction() != null ? ref.instruction().trim() : null,
                            order++,
                            now
                    ));
                }
            }
        } else {
            List<AiJobReferenceRecord> parentRefs = aiReferenceRepository.findReferencesByJobIdAndStudio(studioId, parent.id());
            int order = 0;
            for (AiJobReferenceRecord pr : parentRefs) {
                refsToSnapshot.add(new AiJobReferenceRecord(
                        UuidV7.randomUuid(),
                        newJobId,
                        studioId,
                        pr.mediaId(),
                        pr.purposeSnapshot(),
                        pr.labelSnapshot(),
                        pr.instructionSnapshot(),
                        order++,
                        now
                ));
            }
        }

        int variationCount = aiJobRepository.countByStudio(studioId, parent.projectId());
        String conceptLabel = "Variation " + (variationCount + 1);

        AiJobRecord job = new AiJobRecord(
                newJobId,
                studioId,
                parent.projectId(),
                inputMediaId,
                null,
                aiImageProvider.getProviderKey(),
                null,
                prompt,
                null,
                AiJobStatus.QUEUED,
                null,
                null,
                0,
                request.idempotencyKey(),
                actor.userId(),
                now,
                null,
                null,
                null,
                null,
                0,
                preserveStructure,
                mode,
                maskStorageKey,
                parent.id(),
                rootJobId,
                false,
                false,
                conceptLabel
        );

        aiJobRepository.createJob(job);
        if (!refsToSnapshot.isEmpty()) {
            aiReferenceRepository.createJobReferences(refsToSnapshot);
        }

        auditService.record(
                actor.userId(),
                studioId,
                "AI_VARIATION_SUBMITTED",
                "AI_JOB",
                job.id().toString(),
                Map.of(
                        "projectId", parent.projectId().toString(),
                        "parentJobId", parent.id().toString(),
                        "rootJobId", rootJobId.toString(),
                        "strategy", strategy.name(),
                        "editingMode", mode.name()
                ),
                null,
                null
        );

        executor.submit(() -> executeJobInternal(job.id()));

        return toJobDetail(job, studioId);
    }

    public AiJobDetailResponse toggleShortlist(ActorContext actor, UUID requestedStudioId, UUID jobId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiJobRecord job = aiJobRepository.findById(context.studioId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI generation job not found"));

        boolean newShortlist = !job.isShortlisted();
        aiJobRepository.updateShortlist(context.studioId(), jobId, newShortlist);

        AiJobRecord updated = aiJobRepository.findById(context.studioId(), jobId).orElseThrow();
        return toJobDetail(updated, context.studioId());
    }

    public AiJobDetailResponse toggleStudioSelected(ActorContext actor, UUID requestedStudioId, UUID jobId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiJobRecord job = aiJobRepository.findById(context.studioId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AI generation job not found"));

        boolean newSelected = !job.isStudioSelected();
        aiJobRepository.updateStudioSelected(context.studioId(), jobId, newSelected);

        AiJobRecord updated = aiJobRepository.findById(context.studioId(), jobId).orElseThrow();
        return toJobDetail(updated, context.studioId());
    }

    public AiJobHistoryResponse listJobHistory(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            EditingMode editingMode,
            AiJobStatus status,
            Boolean shortlistedOnly,
            int page,
            int limit
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        int safeLimit = Math.max(1, Math.min(50, limit));
        int safePage = Math.max(0, page);
        int offset = safePage * safeLimit;

        List<AiJobRecord> jobs = aiJobRepository.findHistory(
                context.studioId(),
                projectId,
                editingMode,
                status,
                shortlistedOnly,
                safeLimit,
                offset
        );
        long total = aiJobRepository.countHistory(
                context.studioId(),
                projectId,
                editingMode,
                status,
                shortlistedOnly
        );

        List<AiJobDetailResponse> items = jobs.stream()
                .map(j -> toJobDetail(j, context.studioId()))
                .toList();
        int totalPages = (int) Math.ceil((double) total / safeLimit);

        return new AiJobHistoryResponse(items, safePage, safeLimit, total, totalPages);
    }

    // =========================================================================
    // Phase 24: Client Review Lifecycle & Token Exchange
    // =========================================================================

    public CreateClientReviewResponse createClientReview(ActorContext actor, UUID requestedStudioId, CreateClientReviewRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UUID studioId = context.studioId();

        StudioProjectRecord project = projectRepository.findProjectById(studioId, request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        if (request.conceptJobIds() == null || request.conceptJobIds().isEmpty()) {
            throw new BadRequestException("At least one concept must be selected");
        }
        if (request.conceptJobIds().size() > 10) {
            throw new BadRequestException("Cannot include more than 10 concepts in a single review");
        }

        List<AiJobRecord> validJobs = new ArrayList<>();
        for (UUID jobId : request.conceptJobIds()) {
            AiJobRecord job = aiJobRepository.findById(studioId, jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Concept job not found in studio: " + jobId));
            if (!job.projectId().equals(project.id())) {
                throw new BadRequestException("Cross-project concept inclusion denied: " + jobId);
            }
            if (job.status() != AiJobStatus.SUCCEEDED || job.outputMediaId() == null) {
                throw new BadRequestException("Only succeeded concepts with output images can be shared: " + jobId);
            }
            validJobs.add(job);
        }

        String rawToken = generateSecureToken();
        byte[] tokenHash = hashSha256(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(request.resolvedExpiryDays(), ChronoUnit.DAYS);
        UUID reviewId = UuidV7.randomUuid();

        AiClientReviewRecord review = new AiClientReviewRecord(
                reviewId,
                studioId,
                project.id(),
                request.title().trim(),
                request.customMessage() != null ? request.customMessage().trim() : null,
                tokenHash,
                ClientReviewStatus.OPEN,
                request.resolvedIncludeOriginal(),
                expiresAt,
                null,
                actor.userId(),
                now,
                now,
                0
        );

        aiClientReviewRepository.createReview(review);

        List<AiClientReviewItemRecord> items = new ArrayList<>();
        List<ClientReviewItemDto> itemDtos = new ArrayList<>();
        int order = 0;
        for (AiJobRecord job : validJobs) {
            UUID itemId = UuidV7.randomUuid();
            String displayLabel = "Concept " + (order + 1);
            AiClientReviewItemRecord item = new AiClientReviewItemRecord(
                    itemId,
                    reviewId,
                    studioId,
                    job.id(),
                    job.outputMediaId(),
                    displayLabel,
                    order,
                    now
            );
            items.add(item);
            itemDtos.add(new ClientReviewItemDto(
                    itemId,
                    job.id(),
                    job.outputMediaId(),
                    displayLabel,
                    order,
                    resolvePreviewUrl(job.outputMediaId(), studioId)
            ));
            order++;
        }

        aiClientReviewRepository.createReviewItems(items);

        auditService.record(
                actor.userId(),
                studioId,
                "CLIENT_REVIEW_CREATED",
                "CLIENT_REVIEW",
                reviewId.toString(),
                Map.of("projectId", project.id().toString(), "itemCount", String.valueOf(items.size())),
                null,
                null
        );

        return new CreateClientReviewResponse(
                reviewId,
                project.id(),
                review.title(),
                review.customMessage(),
                rawToken,
                "/review/" + rawToken,
                review.status().name(),
                review.includeOriginal(),
                expiresAt,
                itemDtos,
                now
        );
    }

    public List<ClientReviewDetailResponse> listStudioReviews(ActorContext actor, UUID requestedStudioId, UUID projectId, int page, int limit) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        int safeLimit = Math.max(1, Math.min(50, limit));
        int safePage = Math.max(0, page);
        int offset = safePage * safeLimit;

        List<AiClientReviewRecord> reviews = aiClientReviewRepository.listReviewsByStudio(context.studioId(), projectId, safeLimit, offset);
        return reviews.stream()
                .map(r -> toStudioReviewDetail(r, context.studioId()))
                .toList();
    }

    public ClientReviewDetailResponse getStudioReviewDetail(ActorContext actor, UUID requestedStudioId, UUID reviewId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiClientReviewRecord review = aiClientReviewRepository.findReviewById(context.studioId(), reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Client review not found"));

        return toStudioReviewDetail(review, context.studioId());
    }

    public void closeReview(ActorContext actor, UUID requestedStudioId, UUID reviewId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        aiClientReviewRepository.findReviewById(context.studioId(), reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Client review not found"));

        aiClientReviewRepository.updateReviewStatus(context.studioId(), reviewId, ClientReviewStatus.CLOSED);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "CLIENT_REVIEW_CLOSED",
                "CLIENT_REVIEW",
                reviewId.toString(),
                null,
                null,
                null
        );
    }

    public void revokeReview(ActorContext actor, UUID requestedStudioId, UUID reviewId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        aiClientReviewRepository.findReviewById(context.studioId(), reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Client review not found"));

        aiClientReviewRepository.updateReviewStatus(context.studioId(), reviewId, ClientReviewStatus.REVOKED);
        aiClientReviewRepository.revokeAllSessionsForReview(reviewId);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "CLIENT_REVIEW_REVOKED",
                "CLIENT_REVIEW",
                reviewId.toString(),
                null,
                null,
                null
        );
    }

    public CreateClientReviewResponse rotateReviewToken(ActorContext actor, UUID requestedStudioId, UUID reviewId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        AiClientReviewRecord review = aiClientReviewRepository.findReviewById(context.studioId(), reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Client review not found"));

        aiClientReviewRepository.revokeAllSessionsForReview(reviewId);

        String newRawToken = generateSecureToken();
        byte[] newTokenHash = hashSha256(newRawToken);

        aiClientReviewRepository.updateTokenHash(context.studioId(), reviewId, newTokenHash);

        List<AiClientReviewItemRecord> items = aiClientReviewRepository.findItemsByReviewId(reviewId);
        List<ClientReviewItemDto> itemDtos = items.stream().map(it -> new ClientReviewItemDto(
                it.id(),
                it.jobId(),
                it.mediaId(),
                it.displayLabel(),
                it.displayOrder(),
                resolvePreviewUrl(it.mediaId(), context.studioId())
        )).toList();

        return new CreateClientReviewResponse(
                reviewId,
                review.projectId(),
                review.title(),
                review.customMessage(),
                newRawToken,
                "/review/" + newRawToken,
                ClientReviewStatus.OPEN.name(),
                review.includeOriginal(),
                review.expiresAt(),
                itemDtos,
                review.createdAt()
        );
    }

    public ExchangeReviewSessionResult exchangeReviewToken(String rawToken, String clientIp) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Review token is required");
        }
        rateLimiterService.acquire("review_exchange:" + clientIp, 20, Duration.ofMinutes(1));

        byte[] tokenHash = hashSha256(rawToken.trim());
        AiClientReviewRecord review = aiClientReviewRepository.findReviewByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Review link is invalid or unavailable"));

        if (review.status() == ClientReviewStatus.REVOKED) {
            throw new ResourceNotFoundException("Review link is no longer available");
        }
        if (review.isExpired(Instant.now())) {
            throw new ResourceNotFoundException("Review link has expired");
        }

        String rawSessionToken = generateSecureToken();
        String rawCsrfToken = generateSecureToken();

        byte[] sessionHash = hashSha256(rawSessionToken);
        byte[] csrfHash = hashSha256(rawCsrfToken);

        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                review.id(),
                sessionHash,
                csrfHash,
                review.expiresAt(),
                null,
                Instant.now()
        );

        aiClientReviewRepository.createReviewSession(session);

        return new ExchangeReviewSessionResult(
                review.id(),
                rawSessionToken,
                rawCsrfToken,
                review.expiresAt()
        );
    }

    public PublicClientReviewResponse getPublicReview(String rawSessionToken, String clientIp) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new UnauthorizedException("Review session required");
        }
        rateLimiterService.acquire("review_fetch:" + clientIp, 60, Duration.ofMinutes(1));

        byte[] sessionHash = hashSha256(rawSessionToken);
        AiClientReviewSessionRecord session = aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)
                .orElseThrow(() -> new UnauthorizedException("Review session is invalid"));

        if (!session.isActive(Instant.now())) {
            throw new UnauthorizedException("Review session has expired or been revoked");
        }

        AiClientReviewRecord review = aiClientReviewRepository.findReviewByIdGlobal(session.reviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (review.status() == ClientReviewStatus.REVOKED) {
            throw new ResourceNotFoundException("Review is no longer available");
        }

        String studioName = resolveStudioName(review.studioId());
        String projectTitle = projectRepository.findProjectById(review.studioId(), review.projectId())
                .map(StudioProjectRecord::title)
                .orElse("Interior Project");

        List<AiClientReviewItemRecord> items = aiClientReviewRepository.findItemsByReviewId(review.id());
        List<PublicReviewItemDto> publicItems = new ArrayList<>();

        for (AiClientReviewItemRecord item : items) {
            Optional<AiClientReviewDecisionRecord> decOpt = aiClientReviewRepository.findCurrentDecisionForJob(review.id(), item.jobId());
            String decisionStr = decOpt.map(d -> d.decision().name()).orElse(null);
            publicItems.add(new PublicReviewItemDto(
                    item.id(),
                    item.jobId(),
                    item.mediaId(),
                    item.displayLabel(),
                    item.displayOrder(),
                    "/api/v1/client-review/media/" + item.mediaId(),
                    decisionStr
            ));
        }

        String originalPreviewUrl = null;
        if (review.includeOriginal() && !items.isEmpty()) {
            AiJobRecord firstJob = aiJobRepository.findByIdGlobal(items.get(0).jobId()).orElse(null);
            if (firstJob != null && firstJob.inputMediaId() != null) {
                originalPreviewUrl = "/api/v1/client-review/media/" + firstJob.inputMediaId();
            }
        }

        List<PublicReviewDecisionDto> publicDecisions = aiClientReviewRepository.findDecisionsByReviewId(review.id()).stream()
                .map(d -> new PublicReviewDecisionDto(
                        d.id(),
                        d.jobId(),
                        d.decision(),
                        d.clientName(),
                        d.feedback(),
                        d.isCurrent(),
                        d.createdAt()
                )).toList();

        List<PublicReviewCommentDto> publicComments = aiClientReviewRepository.findCommentsByReviewId(review.id()).stream()
                .map(c -> new PublicReviewCommentDto(
                        c.id(),
                        c.jobId(),
                        c.authorType(),
                        c.authorName(),
                        c.commentText(),
                        c.createdAt()
                )).toList();

        boolean isExpired = review.isExpired(Instant.now());

        return new PublicClientReviewResponse(
                review.id(),
                studioName,
                projectTitle,
                review.title(),
                review.customMessage(),
                review.status().name(),
                review.includeOriginal(),
                originalPreviewUrl,
                review.expiresAt(),
                isExpired,
                review.currentApprovedJobId(),
                publicItems,
                publicDecisions,
                publicComments,
                review.createdAt()
        );
    }

    public byte[] getReviewMediaPreview(String rawSessionToken, UUID mediaId) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new UnauthorizedException("Review session required");
        }
        byte[] sessionHash = hashSha256(rawSessionToken);
        AiClientReviewSessionRecord session = aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)
                .orElseThrow(() -> new UnauthorizedException("Review session is invalid"));

        if (!session.isActive(Instant.now())) {
            throw new UnauthorizedException("Review session has expired or been revoked");
        }

        AiClientReviewRecord review = aiClientReviewRepository.findReviewByIdGlobal(session.reviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (review.status() == ClientReviewStatus.REVOKED || review.isExpired(Instant.now())) {
            throw new ResourceNotFoundException("Review is no longer available");
        }

        boolean isItemMedia = aiClientReviewRepository.isMediaInReview(review.id(), mediaId);
        boolean isOriginalAllowed = false;
        if (!isItemMedia && review.includeOriginal()) {
            List<AiClientReviewItemRecord> items = aiClientReviewRepository.findItemsByReviewId(review.id());
            for (AiClientReviewItemRecord it : items) {
                AiJobRecord job = aiJobRepository.findByIdGlobal(it.jobId()).orElse(null);
                if (job != null && mediaId.equals(job.inputMediaId())) {
                    isOriginalAllowed = true;
                    break;
                }
            }
        }

        if (!isItemMedia && !isOriginalAllowed) {
            throw new ResourceNotFoundException("Media asset not found in this review");
        }

        MediaAssetRecord asset = mediaRepository.findMediaAsset(mediaId, review.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));

        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(mediaId, review.studioId());
        String storageKey = null;
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.MEDIUM) {
                storageKey = d.storageKey();
                break;
            }
        }
        if (storageKey == null) {
            for (MediaDerivativeRecord d : derivatives) {
                if (d.variantName() == DerivativeVariant.LARGE) {
                    storageKey = d.storageKey();
                    break;
                }
            }
        }
        if (storageKey == null && !derivatives.isEmpty()) {
            storageKey = derivatives.get(0).storageKey();
        }
        if (storageKey != null) {
            byte[] bytes = storageService.load(storageKey);
            if (bytes != null && bytes.length > 0) {
                return bytes;
            }
        }

        // Invariant: Clean original is NEVER served directly to client review.
        // If pre-generated derivative is absent, dynamically create a watermarked derivative with mandatory AI disclosure.
        byte[] originalBytes = storageService.load(asset.originalStorageKey());
        if (originalBytes == null || originalBytes.length == 0) {
            throw new ResourceNotFoundException("Media content unavailable");
        }
        StudioWatermarkSettingsRecord wmSettings = mediaRepository.findWatermarkSettings(review.studioId())
                .orElse(new StudioWatermarkSettingsRecord(
                        review.studioId(), true, WatermarkPosition.BOTTOM_RIGHT, new java.math.BigDecimal("0.60"), 15, false, null, Instant.now(), Instant.now()
                ));
        ImageProcessingService.ProcessedDerivative pd = imageProcessingService.createDerivative(
                originalBytes,
                DerivativeVariant.MEDIUM,
                wmSettings,
                true,
                asset.mediaType()
        );
        return pd.content();
    }

    @Transactional
    public void submitClientDecision(String rawSessionToken, String csrfToken, SubmitClientDecisionRequest request, String clientIp) {
        rateLimiterService.acquire("review_decision:" + clientIp, 20, Duration.ofMinutes(1));
        AiClientReviewRecord review = verifySessionAndCsrf(rawSessionToken, csrfToken);

        if (review.status() != ClientReviewStatus.OPEN || review.isExpired(Instant.now())) {
            throw new BadRequestException("This review is closed or expired. New decisions cannot be submitted.");
        }

        aiClientReviewRepository.findItemByReviewAndJob(review.id(), request.jobId())
                .orElseThrow(() -> new BadRequestException("Concept job is not part of this review"));

        aiClientReviewRepository.clearCurrentDecisionsForReview(review.id());

        AiClientReviewDecisionRecord decision = new AiClientReviewDecisionRecord(
                UuidV7.randomUuid(),
                review.id(),
                review.studioId(),
                request.jobId(),
                request.decision(),
                sanitize(request.clientName()),
                sanitize(request.feedback()),
                true,
                Instant.now()
        );
        aiClientReviewRepository.createDecision(decision);

        if (request.decision() == ClientReviewDecisionType.APPROVED) {
            aiClientReviewRepository.updateReviewCurrentApprovedJob(review.id(), request.jobId());
        }

        auditService.record(
                null,
                review.studioId(),
                "CLIENT_DECISION_RECORDED",
                "CLIENT_REVIEW",
                review.id().toString(),
                Map.of("decision", request.decision().name(), "jobId", request.jobId().toString()),
                null,
                null
        );
    }

    public void submitClientComment(String rawSessionToken, String csrfToken, SubmitClientCommentRequest request, String clientIp) {
        rateLimiterService.acquire("review_comment:" + clientIp, 20, Duration.ofMinutes(1));
        AiClientReviewRecord review = verifySessionAndCsrf(rawSessionToken, csrfToken);

        if (review.status() != ClientReviewStatus.OPEN || review.isExpired(Instant.now())) {
            throw new BadRequestException("This review is closed or expired. Comments cannot be added.");
        }

        if (request.jobId() != null) {
            aiClientReviewRepository.findItemByReviewAndJob(review.id(), request.jobId())
                    .orElseThrow(() -> new BadRequestException("Concept job is not part of this review"));
        }

        AiClientReviewCommentRecord comment = new AiClientReviewCommentRecord(
                UuidV7.randomUuid(),
                review.id(),
                review.studioId(),
                request.jobId(),
                CommentAuthorType.CLIENT,
                sanitize(request.authorName()),
                sanitize(request.commentText()),
                Instant.now()
        );
        aiClientReviewRepository.createComment(comment);

        auditService.record(
                null,
                review.studioId(),
                "CLIENT_COMMENT_ADDED",
                "CLIENT_REVIEW",
                review.id().toString(),
                Map.of("authorName", comment.authorName()),
                null,
                null
        );
    }

    private AiClientReviewRecord verifySessionAndCsrf(String rawSessionToken, String rawCsrfToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new UnauthorizedException("Review session required");
        }
        if (rawCsrfToken == null || rawCsrfToken.isBlank()) {
            throw new AccessDeniedException("CSRF token required");
        }
        byte[] sessionHash = hashSha256(rawSessionToken);
        AiClientReviewSessionRecord session = aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid review session"));

        if (!session.isActive(Instant.now())) {
            throw new UnauthorizedException("Review session expired or revoked");
        }

        byte[] headerCsrfHash = hashSha256(rawCsrfToken);
        if (!MessageDigest.isEqual(headerCsrfHash, session.csrfTokenHash())) {
            throw new AccessDeniedException("Invalid CSRF token");
        }

        AiClientReviewRecord review = aiClientReviewRepository.findReviewByIdGlobal(session.reviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (review.status() == ClientReviewStatus.REVOKED || review.isExpired(Instant.now())) {
            throw new ResourceNotFoundException("Review is no longer available");
        }

        return review;
    }

    private ClientReviewDetailResponse toStudioReviewDetail(AiClientReviewRecord review, UUID studioId) {
        String projectTitle = projectRepository.findProjectById(studioId, review.projectId())
                .map(StudioProjectRecord::title)
                .orElse("Interior Project");

        List<AiClientReviewItemRecord> items = aiClientReviewRepository.findItemsByReviewId(review.id());
        List<ClientReviewItemDto> itemDtos = items.stream().map(it -> new ClientReviewItemDto(
                it.id(),
                it.jobId(),
                it.mediaId(),
                it.displayLabel(),
                it.displayOrder(),
                resolvePreviewUrl(it.mediaId(), studioId)
        )).toList();

        List<ClientReviewDecisionDto> decisions = aiClientReviewRepository.findDecisionsByReviewId(review.id()).stream().map(d -> new ClientReviewDecisionDto(
                d.id(),
                d.jobId(),
                d.decision(),
                d.clientName(),
                d.feedback(),
                d.isCurrent(),
                d.createdAt()
        )).toList();

        List<ClientReviewCommentDto> comments = aiClientReviewRepository.findCommentsByReviewId(review.id()).stream().map(c -> new ClientReviewCommentDto(
                c.id(),
                c.jobId(),
                c.authorType(),
                c.authorName(),
                c.commentText(),
                c.createdAt()
        )).toList();

        return new ClientReviewDetailResponse(
                review.id(),
                review.projectId(),
                projectTitle,
                review.title(),
                review.customMessage(),
                review.status().name(),
                review.includeOriginal(),
                review.expiresAt(),
                review.currentApprovedJobId(),
                itemDtos,
                decisions,
                comments,
                review.createdAt(),
                review.updatedAt()
        );
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replace("<", "&lt;").replace(">", "&gt;").trim();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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
