package com.interior.platform.media.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.dto.*;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;
    private final ProjectRepository projectRepository;
    private final StudioRepository studioRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;

    public MediaService(
            MediaRepository mediaRepository,
            ProjectRepository projectRepository,
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            StorageService storageService,
            ImageProcessingService imageProcessingService
    ) {
        this.mediaRepository = mediaRepository;
        this.projectRepository = projectRepository;
        this.studioRepository = studioRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.storageService = storageService;
        this.imageProcessingService = imageProcessingService;
    }

    // 1. Upload Intent
    @Transactional
    public UploadIntentResponse createUploadIntent(ActorContext actor, UUID requestedStudioId, CreateUploadIntentRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        StudioProjectRecord project = projectRepository.findProjectById(context.studioId(), request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        UUID uploadIntentId = UuidV7.randomUuid();
        UUID mediaAssetId = UuidV7.randomUuid();

        String quarantineKey = storageService.generateQuarantineKey(
                context.studioId(),
                uploadIntentId,
                mediaAssetId,
                request.expectedContentType()
        );

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        UploadIntentRecord intentRecord = new UploadIntentRecord(
                uploadIntentId,
                context.studioId(),
                project.id(),
                request.mediaType(),
                request.expectedContentType(),
                request.expectedSizeBytes(),
                quarantineKey,
                UploadIntentStatus.PENDING,
                expiresAt,
                actor.userId(),
                Instant.now()
        );

        mediaRepository.createUploadIntent(intentRecord);

        String uploadUrl = storageService.generateUploadUrl(uploadIntentId, quarantineKey);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "MEDIA_UPLOAD_INTENT_CREATED",
                "UPLOAD_INTENT",
                uploadIntentId.toString(),
                Map.of("projectId", project.id().toString(), "mediaType", request.mediaType().name()),
                null,
                null
        );

        return new UploadIntentResponse(uploadIntentId, mediaAssetId, uploadUrl, quarantineKey, expiresAt);
    }

    // 2. Direct Quarantine Storage Endpoint handler
    public void handleQuarantineUpload(UUID uploadIntentId, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("Uploaded file content cannot be empty");
        }
        if (content.length > 26214400) {
            throw new BadRequestException("Uploaded file size exceeds 25MB maximum");
        }

        // Search intent in DB
        // Find studio using intent
        // We'll update the quarantine storage
        // Since caller sends uploadIntentId, load intent
        // Check across studios or direct SQL
        // We have findUploadIntent with studioId, let's look up or use direct query
        // Let's implement finding intent by ID
        Optional<UploadIntentRecord> intentOpt = findIntentByIdGlobal(uploadIntentId);
        if (intentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Upload intent not found");
        }
        UploadIntentRecord intent = intentOpt.get();
        if (intent.status() != UploadIntentStatus.PENDING) {
            throw new BadRequestException("Upload intent is no longer pending");
        }
        if (intent.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Upload intent has expired");
        }

        storageService.store(intent.quarantineKey(), content, contentType != null ? contentType : intent.expectedContentType());
    }

    // 3. Commit Upload
    @Transactional
    public MediaDetailResponse commitUpload(ActorContext actor, UUID requestedStudioId, CommitUploadRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        UploadIntentRecord intent = mediaRepository.findUploadIntent(request.uploadIntentId(), context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Upload intent not found"));

        if (intent.status() != UploadIntentStatus.PENDING) {
            throw new BadRequestException("Upload intent has already been processed or cancelled");
        }
        if (intent.expiresAt().isBefore(Instant.now())) {
            mediaRepository.updateUploadIntentStatus(intent.id(), UploadIntentStatus.EXPIRED);
            throw new BadRequestException("Upload intent has expired");
        }

        if (!storageService.exists(intent.quarantineKey())) {
            throw new BadRequestException("No uploaded file found for intent. Please upload the file bytes before committing.");
        }

        byte[] rawBytes = storageService.load(intent.quarantineKey());
        if (rawBytes == null || rawBytes.length == 0) {
            throw new BadRequestException("Uploaded file data is empty");
        }

        // Validate and get image dimensions
        ImageProcessingService.ImageDimensions dims = imageProcessingService.validateAndGetDimensions(rawBytes);

        // Visibility rules
        MediaType mediaType = intent.mediaType();
        MediaVisibility visibility = request.visibility();
        if (mediaType == MediaType.REFERENCE || mediaType == MediaType.CLIENT_PRIVATE) {
            if (visibility != null && visibility != MediaVisibility.PRIVATE) {
                throw new BadRequestException(mediaType.name() + " media is strictly private and cannot have PORTFOLIO or PUBLIC visibility");
            }
            visibility = MediaVisibility.PRIVATE;
        } else {
            if (visibility == null) {
                visibility = MediaVisibility.PORTFOLIO;
            }
        }

        UUID mediaAssetId = UuidV7.randomUuid();
        String canonicalKey = storageService.generateCanonicalOriginalKey(
                context.studioId(),
                intent.projectId(),
                mediaAssetId,
                intent.expectedContentType()
        );

        // Move raw file from quarantine to canonical private original
        storageService.move(intent.quarantineKey(), canonicalKey);

        // Determine cover logic
        boolean isCover = Boolean.TRUE.equals(request.isCover());
        Optional<MediaAssetRecord> existingCover = mediaRepository.findCoverMedia(intent.projectId(), context.studioId());
        if (!isCover && existingCover.isEmpty() && visibility != MediaVisibility.PRIVATE) {
            isCover = true;
        }
        if (isCover) {
            mediaRepository.unsetOtherCovers(intent.projectId(), context.studioId(), mediaAssetId);
        }

        int sortOrder = mediaRepository.getNextSortOrder(intent.projectId(), context.studioId());
        boolean watermarkEnabled = request.watermarkEnabled() == null || request.watermarkEnabled();

        Instant now = Instant.now();
        MediaAssetRecord asset = new MediaAssetRecord(
                mediaAssetId,
                context.studioId(),
                intent.projectId(),
                mediaType,
                visibility,
                MediaProcessingStatus.PROCESSING,
                canonicalKey,
                intent.expectedContentType(),
                rawBytes.length,
                dims.width(),
                dims.height(),
                sortOrder,
                isCover,
                request.altText(),
                request.caption(),
                watermarkEnabled,
                actor.userId(),
                now,
                now,
                null
        );

        mediaRepository.createMediaAsset(asset);

        // Generate public derivatives if eligible
        List<MediaDerivativeRecord> derivatives = new ArrayList<>();
        if (visibility != MediaVisibility.PRIVATE && mediaType.isPublicEligible()) {
            StudioWatermarkSettingsRecord watermarkSettings = getOrCreateWatermarkSettings(context.studioId());

            for (DerivativeVariant variant : DerivativeVariant.values()) {
                ImageProcessingService.ProcessedDerivative pd = imageProcessingService.createDerivative(
                        rawBytes,
                        variant,
                        watermarkSettings,
                        watermarkEnabled,
                        mediaType
                );

                String derivativeKey = storageService.generateDerivativeKey(
                        context.studioId(),
                        intent.projectId(),
                        mediaAssetId,
                        variant.name(),
                        pd.format()
                );

                storageService.store(derivativeKey, pd.content(), "image/jpeg");
                String publicUrl = storageService.resolvePublicUrl(derivativeKey);

                derivatives.add(new MediaDerivativeRecord(
                        UuidV7.randomUuid(),
                        mediaAssetId,
                        context.studioId(),
                        variant,
                        pd.width(),
                        pd.height(),
                        pd.format(),
                        pd.fileSize(),
                        derivativeKey,
                        publicUrl,
                        pd.isWatermarked(),
                        now
                ));
            }
            mediaRepository.saveDerivatives(derivatives);
        }

        // Mark asset READY and intent COMMITTED
        MediaAssetRecord readyAsset = new MediaAssetRecord(
                asset.id(),
                asset.studioId(),
                asset.projectId(),
                asset.mediaType(),
                asset.visibility(),
                MediaProcessingStatus.READY,
                asset.originalStorageKey(),
                asset.contentType(),
                asset.fileSize(),
                asset.width(),
                asset.height(),
                asset.sortOrder(),
                asset.isCover(),
                asset.altText(),
                asset.caption(),
                asset.watermarkEnabled(),
                asset.createdBy(),
                asset.createdAt(),
                now,
                null
        );
        mediaRepository.updateMediaAsset(readyAsset);
        mediaRepository.updateUploadIntentStatus(intent.id(), UploadIntentStatus.COMMITTED);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "MEDIA_COMMITTED",
                "MEDIA_ASSET",
                mediaAssetId.toString(),
                Map.of("projectId", intent.projectId().toString(), "mediaType", mediaType.name()),
                null,
                null
        );

        return toDetailResponse(readyAsset, derivatives);
    }

    // 4. List Media for Project
    @Transactional(readOnly = true)
    public List<MediaDetailResponse> listProjectMedia(ActorContext actor, UUID requestedStudioId, UUID projectId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        List<MediaAssetRecord> assets = mediaRepository.findMediaAssetsByProject(projectId, context.studioId(), false);

        return assets.stream()
                .map(a -> {
                    List<MediaDerivativeRecord> d = mediaRepository.findDerivativesByMediaId(a.id(), context.studioId());
                    return toDetailResponse(a, d);
                })
                .toList();
    }

    // 5. Get Media Detail
    @Transactional(readOnly = true)
    public MediaDetailResponse getMedia(ActorContext actor, UUID requestedStudioId, UUID mediaId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        MediaAssetRecord asset = mediaRepository.findMediaAsset(mediaId, context.studioId())
                .filter(a -> a.deletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));

        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(asset.id(), context.studioId());
        return toDetailResponse(asset, derivatives);
    }

    // 6. Update Media
    @Transactional
    public MediaDetailResponse updateMedia(ActorContext actor, UUID requestedStudioId, UUID mediaId, UpdateMediaRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        MediaAssetRecord asset = mediaRepository.findMediaAsset(mediaId, context.studioId())
                .filter(a -> a.deletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));

        MediaVisibility newVisibility = request.visibility() != null ? request.visibility() : asset.visibility();
        if (asset.mediaType() == MediaType.REFERENCE || asset.mediaType() == MediaType.CLIENT_PRIVATE) {
            if (newVisibility != MediaVisibility.PRIVATE) {
                throw new BadRequestException(asset.mediaType().name() + " media is strictly private and cannot have PORTFOLIO or PUBLIC visibility");
            }
        }

        boolean newIsCover = request.isCover() != null ? request.isCover() : asset.isCover();
        if (newIsCover && !asset.isCover()) {
            mediaRepository.unsetOtherCovers(asset.projectId(), context.studioId(), asset.id());
        }

        String newAlt = request.altText() != null ? request.altText() : asset.altText();
        String newCaption = request.caption() != null ? request.caption() : asset.caption();
        boolean newWatermark = request.watermarkEnabled() != null ? request.watermarkEnabled() : asset.watermarkEnabled();
        int newSort = request.sortOrder() != null ? request.sortOrder() : asset.sortOrder();

        boolean regenerateDerivatives = (newWatermark != asset.watermarkEnabled() || newVisibility != asset.visibility())
                && newVisibility != MediaVisibility.PRIVATE && asset.mediaType().isPublicEligible();

        MediaAssetRecord updated = new MediaAssetRecord(
                asset.id(),
                asset.studioId(),
                asset.projectId(),
                asset.mediaType(),
                newVisibility,
                asset.processingStatus(),
                asset.originalStorageKey(),
                asset.contentType(),
                asset.fileSize(),
                asset.width(),
                asset.height(),
                newSort,
                newIsCover,
                newAlt,
                newCaption,
                newWatermark,
                asset.createdBy(),
                asset.createdAt(),
                Instant.now(),
                null
        );

        mediaRepository.updateMediaAsset(updated);

        if (regenerateDerivatives) {
            mediaRepository.deleteDerivativesByMediaId(asset.id(), context.studioId());
            byte[] originalBytes = storageService.load(asset.originalStorageKey());
            if (originalBytes != null) {
                StudioWatermarkSettingsRecord watermarkSettings = getOrCreateWatermarkSettings(context.studioId());
                List<MediaDerivativeRecord> newDerivatives = new ArrayList<>();
                for (DerivativeVariant variant : DerivativeVariant.values()) {
                    ImageProcessingService.ProcessedDerivative pd = imageProcessingService.createDerivative(
                            originalBytes,
                            variant,
                            watermarkSettings,
                            newWatermark,
                            asset.mediaType()
                    );
                    String derivativeKey = storageService.generateDerivativeKey(
                            context.studioId(),
                            asset.projectId(),
                            asset.id(),
                            variant.name(),
                            pd.format()
                    );
                    storageService.store(derivativeKey, pd.content(), "image/jpeg");
                    String publicUrl = storageService.resolvePublicUrl(derivativeKey);
                    newDerivatives.add(new MediaDerivativeRecord(
                            UuidV7.randomUuid(),
                            asset.id(),
                            context.studioId(),
                            variant,
                            pd.width(),
                            pd.height(),
                            pd.format(),
                            pd.fileSize(),
                            derivativeKey,
                            publicUrl,
                            pd.isWatermarked(),
                            Instant.now()
                    ));
                }
                mediaRepository.saveDerivatives(newDerivatives);
            }
        }

        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(asset.id(), context.studioId());
        return toDetailResponse(updated, derivatives);
    }

    // 7. Reorder Media
    @Transactional
    public void reorderMedia(ActorContext actor, UUID requestedStudioId, UUID projectId, ReorderMediaRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        List<UUID> orderedIds = request.orderedMediaIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            mediaRepository.updateSortOrder(orderedIds.get(i), context.studioId(), i);
        }
    }

    // 8. Soft Delete Media
    @Transactional
    public void deleteMedia(ActorContext actor, UUID requestedStudioId, UUID mediaId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        MediaAssetRecord asset = mediaRepository.findMediaAsset(mediaId, context.studioId())
                .filter(a -> a.deletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));

        mediaRepository.softDeleteMediaAsset(asset.id(), context.studioId());

        // Delete public derivatives
        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(asset.id(), context.studioId());
        for (MediaDerivativeRecord d : derivatives) {
            storageService.delete(d.storageKey());
        }
        mediaRepository.deleteDerivativesByMediaId(asset.id(), context.studioId());

        // If it was cover, reassign cover to another active media if exists
        if (asset.isCover()) {
            List<MediaAssetRecord> remaining = mediaRepository.findMediaAssetsByProject(asset.projectId(), context.studioId(), false);
            if (!remaining.isEmpty()) {
                MediaAssetRecord newCover = remaining.getFirst();
                mediaRepository.updateMediaAsset(new MediaAssetRecord(
                        newCover.id(),
                        newCover.studioId(),
                        newCover.projectId(),
                        newCover.mediaType(),
                        newCover.visibility(),
                        newCover.processingStatus(),
                        newCover.originalStorageKey(),
                        newCover.contentType(),
                        newCover.fileSize(),
                        newCover.width(),
                        newCover.height(),
                        newCover.sortOrder(),
                        true,
                        newCover.altText(),
                        newCover.caption(),
                        newCover.watermarkEnabled(),
                        newCover.createdBy(),
                        newCover.createdAt(),
                        Instant.now(),
                        null
                ));
            }
        }

        auditService.record(
                actor.userId(),
                context.studioId(),
                "MEDIA_DELETED",
                "MEDIA_ASSET",
                mediaId.toString(),
                Map.of("projectId", asset.projectId().toString()),
                null,
                null
        );
    }

    // 9. Studio-wide Media Library listing
    @Transactional(readOnly = true)
    public List<MediaDetailResponse> listStudioMedia(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            MediaType mediaType,
            MediaVisibility visibility,
            MediaProcessingStatus status
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        List<MediaAssetRecord> list = mediaRepository.findMediaAssetsByStudio(
                context.studioId(),
                projectId,
                mediaType,
                visibility,
                status,
                false
        );

        return list.stream()
                .map(a -> {
                    List<MediaDerivativeRecord> d = mediaRepository.findDerivativesByMediaId(a.id(), context.studioId());
                    return toDetailResponse(a, d);
                })
                .toList();
    }

    // 10. Watermark Settings
    @Transactional(readOnly = true)
    public WatermarkSettingsResponse getWatermarkSettings(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        StudioWatermarkSettingsRecord s = getOrCreateWatermarkSettings(context.studioId());
        return new WatermarkSettingsResponse(
                s.studioId(),
                s.enabled(),
                s.position(),
                s.opacity(),
                s.sizePercentage(),
                s.useLogo(),
                s.fallbackText()
        );
    }

    @Transactional
    public WatermarkSettingsResponse updateWatermarkSettings(
            ActorContext actor,
            UUID requestedStudioId,
            WatermarkSettingsRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        String fallbackText = request.fallbackText();
        if (fallbackText == null || fallbackText.isBlank()) {
            fallbackText = resolveStudioName(context.studioId());
        }

        StudioWatermarkSettingsRecord toSave = new StudioWatermarkSettingsRecord(
                context.studioId(),
                Boolean.TRUE.equals(request.enabled()),
                request.position(),
                request.opacity(),
                request.sizePercentage(),
                Boolean.TRUE.equals(request.useLogo()),
                fallbackText,
                Instant.now(),
                Instant.now()
        );

        StudioWatermarkSettingsRecord saved = mediaRepository.saveWatermarkSettings(toSave);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "WATERMARK_SETTINGS_UPDATED",
                "STUDIO_WATERMARK_SETTINGS",
                context.studioId().toString(),
                Map.of("enabled", saved.enabled(), "position", saved.position().name()),
                null,
                null
        );

        return new WatermarkSettingsResponse(
                saved.studioId(),
                saved.enabled(),
                saved.position(),
                saved.opacity(),
                saved.sizePercentage(),
                saved.useLogo(),
                saved.fallbackText()
        );
    }

    // 11. Public & Portfolio Presentation Query (no auth required for public portfolio)
    @Transactional(readOnly = true)
    public List<MediaPresentationDto> getProjectMediaPresentation(UUID studioId, UUID projectId) {
        List<MediaAssetRecord> assets = mediaRepository.findMediaAssetsByProject(projectId, studioId, false);
        List<MediaPresentationDto> dtos = new ArrayList<>();

        for (MediaAssetRecord a : assets) {
            if (a.visibility() == MediaVisibility.PRIVATE || !a.mediaType().isPublicEligible()) {
                continue;
            }
            if (a.processingStatus() != MediaProcessingStatus.READY) {
                continue;
            }

            List<MediaDerivativeRecord> d = mediaRepository.findDerivativesByMediaId(a.id(), studioId);
            String thumb = null;
            String med = null;
            String large = null;
            boolean watermarked = false;

            for (MediaDerivativeRecord drv : d) {
                if (drv.variantName() == DerivativeVariant.THUMBNAIL) thumb = drv.publicUrl();
                else if (drv.variantName() == DerivativeVariant.MEDIUM) med = drv.publicUrl();
                else if (drv.variantName() == DerivativeVariant.LARGE) large = drv.publicUrl();
                if (drv.isWatermarked()) watermarked = true;
            }

            dtos.add(new MediaPresentationDto(
                    a.id(),
                    a.projectId(),
                    a.mediaType().name(),
                    thumb != null ? thumb : med,
                    med != null ? med : large,
                    large != null ? large : med,
                    a.isCover(),
                    a.sortOrder(),
                    a.altText(),
                    a.caption(),
                    a.width(),
                    a.height(),
                    watermarked,
                    a.mediaType() == MediaType.AI_CONCEPT
            ));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public Optional<String> getProjectCoverImageUrl(UUID studioId, UUID projectId) {
        Optional<MediaAssetRecord> coverOpt = mediaRepository.findCoverMedia(projectId, studioId);
        if (coverOpt.isEmpty()) {
            List<MediaAssetRecord> list = mediaRepository.findMediaAssetsByProject(projectId, studioId, false);
            coverOpt = list.stream().filter(a -> a.visibility() != MediaVisibility.PRIVATE && a.mediaType().isPublicEligible()).findFirst();
        }

        if (coverOpt.isPresent()) {
            List<MediaDerivativeRecord> d = mediaRepository.findDerivativesByMediaId(coverOpt.get().id(), studioId);
            for (MediaDerivativeRecord drv : d) {
                if (drv.variantName() == DerivativeVariant.LARGE) return Optional.of(drv.publicUrl());
                if (drv.variantName() == DerivativeVariant.MEDIUM) return Optional.of(drv.publicUrl());
                if (drv.variantName() == DerivativeVariant.THUMBNAIL) return Optional.of(drv.publicUrl());
            }
        }
        return Optional.empty();
    }

    public long getActiveMediaCount(UUID studioId) {
        return mediaRepository.countActiveMediaByStudio(studioId);
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

    private Optional<UploadIntentRecord> findIntentByIdGlobal(UUID intentId) {
        return mediaRepository.findUploadIntentGlobal(intentId);
    }

    private MediaDetailResponse toDetailResponse(MediaAssetRecord a, List<MediaDerivativeRecord> derivatives) {
        List<MediaDerivativeDto> dtos = derivatives.stream()
                .map(d -> new MediaDerivativeDto(
                        d.id(),
                        d.variantName(),
                        d.width(),
                        d.height(),
                        d.format(),
                        d.fileSize(),
                        d.publicUrl(),
                        d.isWatermarked()
                ))
                .toList();

        return new MediaDetailResponse(
                a.id(),
                a.studioId(),
                a.projectId(),
                a.mediaType(),
                a.mediaType().getDisplayName(),
                a.visibility(),
                a.processingStatus(),
                a.originalStorageKey(),
                a.contentType(),
                a.fileSize(),
                a.width(),
                a.height(),
                a.sortOrder(),
                a.isCover(),
                a.altText(),
                a.caption(),
                a.watermarkEnabled(),
                dtos,
                a.createdAt(),
                a.updatedAt()
        );
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
            throw new AccessDeniedException("Professional onboarding required to access media engine");
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

    private void requireStudioManagePermission(ResolvedStudioContext context, ActorContext actor) {
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }
        if (!"OWNER".equalsIgnoreCase(context.role()) && !"ADMIN".equalsIgnoreCase(context.role())) {
            throw new AccessDeniedException("Access denied: elevated studio role (OWNER or ADMIN) required for media management");
        }
    }

    private record ResolvedStudioContext(UUID studioId, String role) {}
}
