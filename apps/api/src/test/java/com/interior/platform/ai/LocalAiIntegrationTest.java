package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.ai.dto.AiJobDetailResponse;
import com.interior.platform.ai.dto.AiJobReferenceInput;
import com.interior.platform.ai.dto.CreateAiJobRequest;
import com.interior.platform.ai.provider.MockAiImageProvider;
import com.interior.platform.ai.repository.AiClientReviewRepository;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.media.storage.LocalStorageService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalAiIntegrationTest {

    @Mock private AiJobRepository aiJobRepository;
    @Mock private AiReferenceRepository aiReferenceRepository;
    @Mock private AiClientReviewRepository aiClientReviewRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;
    @Mock private PlatformOperationsService platformOperationsService;

    private MockAiImageProvider mockAiImageProvider;
    private AiVisualizerProperties properties;
    private StorageService storageService;
    private ImageProcessingService imageProcessingService;
    private RateLimiterService rateLimiterService;
    private AuthorizationService authorizationService;
    private AiVisualizerService aiVisualizerService;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private UUID inputMediaId;
    private ActorContext actor;
    private StudioProjectRecord sampleProject;
    private MediaAssetRecord sampleInputMedia;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() throws IOException {
        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();
        inputMediaId = UuidV7.randomUuid();

        actor = new ActorContext(
                userId, "Aura Designer", "designer@aura.in",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        mockAiImageProvider = new MockAiImageProvider();
        properties = new AiVisualizerProperties();
        properties.setProvider("mock_configured");
        properties.setApiKey("test-key-123");
        properties.setDailyStudioLimit(20);
        properties.setMaxPromptLength(500);

        Path tempDir = Files.createTempDirectory("ai-local-integration-storage");
        storageService = new LocalStorageService(tempDir.toString(), "http://localhost:8080/api/v1/media/public");
        imageProcessingService = new ImageProcessingService();
        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        authorizationService = new AuthorizationService();

        aiVisualizerService = new AiVisualizerService(
                aiJobRepository,
                aiReferenceRepository,
                aiClientReviewRepository,
                mockAiImageProvider,
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
        aiVisualizerService.setPlatformOperationsService(platformOperationsService);
        when(platformOperationsService.isAiEnabled()).thenReturn(true);

        // Generate valid test image bytes
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(245, 240, 235));
        g.fillRect(0, 0, 400, 300);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        sampleImageBytes = baos.toByteArray();

        String inputStorageKey = "studio/" + studioId + "/projects/" + projectId + "/originals/" + inputMediaId + ".jpg";
        storageService.store(inputStorageKey, sampleImageBytes, "image/jpeg");

        sampleProject = new StudioProjectRecord(
                projectId, studioId, "penthouse-living", "Penthouse Living Room",
                "Short description", "Full description",
                ProjectCategory.LIVING_ROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userId, Instant.now(), Instant.now(), null
        );

        sampleInputMedia = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.REAL_PROJECT,
                MediaVisibility.PRIVATE, MediaProcessingStatus.READY,
                inputStorageKey, "image/jpeg", sampleImageBytes.length,
                400, 300, 0, false, "Input photo", "Photo", false,
                userId, Instant.now(), Instant.now(), null
        );

        // Default mock auth
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(
                new UserRecord(userId, "Aura Designer", "designer@aura.in", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L)
        ));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Aura Studio", "aura-studio", userId, "OWNER", Instant.now())
        ));
    }

    @Test
    @DisplayName("Happy Path: Creates job, mock provider generates image, private asset and derivatives stored, cost recorded")
    void happyPath_generationSuccessAndCostRecorded() {
        UUID jobId = UuidV7.randomUuid();
        AiJobRecord queuedJob = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Add teak veneer wardrobes and subtle cove lighting", null, AiJobStatus.QUEUED, null, null, 0,
                "idem-happy-1", userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(queuedJob));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));

        // Execute internal processing synchronously
        aiVisualizerService.executeJobInternal(jobId);

        // Verify provider was invoked exactly once
        assertEquals(1, mockAiImageProvider.getSubmissionCount());

        // Verify status was updated to PROCESSING then SUCCEEDED
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.PROCESSING), any(), any(), any(), any(), any(), any(), any(), eq(0L));
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.SUCCEEDED), any(), any(), any(), any(), any(), any(UUID.class), any(), eq(1L));

        // Verify generated media is PRIVATE and AI_CONCEPT
        ArgumentCaptor<MediaAssetRecord> assetCaptor = ArgumentCaptor.forClass(MediaAssetRecord.class);
        verify(mediaRepository).createMediaAsset(assetCaptor.capture());
        MediaAssetRecord created = assetCaptor.getValue();
        assertEquals(MediaType.AI_CONCEPT, created.mediaType());
        assertEquals(MediaVisibility.PRIVATE, created.visibility());
        assertNotNull(created.originalStorageKey());

        // Verify cost ledger record was written
        verify(aiJobRepository).recordCost(argThat(cost ->
                cost.studioId().equals(studioId) &&
                        cost.jobId().equals(jobId) &&
                        "IMAGE_GENERATION".equals(cost.operation()) &&
                        cost.amountMinor() == 4L &&
                        "USD".equals(cost.currency())
        ));
    }

    @Test
    @DisplayName("Masked Edit: Validates precision mask storage key and passes mask bytes to provider")
    void maskedEdit_precisionMaskValidation() throws IOException {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(0);

        // Store valid mask PNG
        String maskKey = "studio/" + studioId + "/masks/" + UUID.randomUUID() + ".png";
        storageService.store(maskKey, sampleImageBytes, "image/png");

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Change wardrobe door finish to fluted charcoal oak",
                "idem-mask-1", true, List.of(), EditingMode.PRECISION_MASK, maskKey
        );

        AiJobDetailResponse response = aiVisualizerService.createGenerationJob(actor, studioId, req);
        assertNotNull(response);

        // Execute internal job
        UUID jobId = response.id();
        AiJobRecord queuedJob = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                req.prompt(), null, AiJobStatus.QUEUED, null, null, 0,
                req.idempotencyKey(), userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.PRECISION_MASK, maskKey
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(queuedJob));

        aiVisualizerService.executeJobInternal(jobId);

        // Verify mask bytes were passed to provider
        assertNotNull(mockAiImageProvider.getLastMaskBytes());
        assertTrue(mockAiImageProvider.getLastMaskBytes().length > 0);
    }

    @Test
    @DisplayName("Cross-Tenant: Rejects mask storage key belonging to a different studio")
    void crossTenant_maskRejection() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(0);

        UUID rogueStudioId = UuidV7.randomUuid();
        String rogueMaskKey = "studio/" + rogueStudioId + "/masks/" + UUID.randomUUID() + ".png";

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Malicious cross-tenant mask request",
                "idem-cross-1", true, List.of(), EditingMode.PRECISION_MASK, rogueMaskKey
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req));
        assertTrue(ex.getMessage().contains("Invalid maskStorageKey. Mask must belong to the current studio"));
    }

    @Test
    @DisplayName("Cross-Tenant: Rejects input media asset belonging to a different studio")
    void crossTenant_inputMediaRejection() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.empty());
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(0);

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Cross-tenant image access attempt",
                "idem-cross-2", true, List.of()
        );

        assertThrows(ResourceNotFoundException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req));
    }

    @Test
    @DisplayName("Multi-Modal References: Attaches up to 4 reference snapshots successfully")
    void multiModalReferences_successfulSnapshot() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(0);

        UUID refMediaId1 = UuidV7.randomUuid();
        MediaAssetRecord refAsset1 = new MediaAssetRecord(
                refMediaId1, studioId, projectId, MediaType.REFERENCE,
                MediaVisibility.PRIVATE, MediaProcessingStatus.READY,
                "ref1.jpg", "image/jpeg", 100, 100, 100, 0, false, null, null, false,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(refMediaId1, studioId)).thenReturn(Optional.of(refAsset1));

        List<AiJobReferenceInput> refs = List.of(
                new AiJobReferenceInput(refMediaId1, com.interior.platform.ai.domain.ReferencePurpose.MATERIAL, "Walnut wood", "Apply to front panels", 0)
        );

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Generate with walnut reference",
                "idem-ref-1", true, refs
        );

        AiJobDetailResponse response = aiVisualizerService.createGenerationJob(actor, studioId, req);
        assertNotNull(response);

        verify(aiReferenceRepository).createJobReferences(argThat(list ->
                list.size() == 1 && list.get(0).mediaId().equals(refMediaId1)
        ));
    }

    @Test
    @DisplayName("Daily Quota: Rejects 21st request when studio reaches 20 generations per day")
    void dailyQuota_exceedingLimit_throwsRateLimitExceeded() {
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(20);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(20);

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Over quota generation attempt",
                "idem-quota-1", true, List.of()
        );

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req));
        assertTrue(ex.getMessage().contains("Daily AI concept generation quota reached"));
    }

    @Test
    @DisplayName("Idempotency: Re-submitting with identical idempotencyKey returns existing job without duplicate creation")
    void idempotency_duplicateSubmission_returnsExisting() {
        String key = "client-idempotency-token-12345";
        AiJobRecord existing = new AiJobRecord(
                UuidV7.randomUuid(), studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Prompt", null, AiJobStatus.QUEUED, null, null, 0,
                key, userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );

        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(aiJobRepository.countTodayActiveReservations(studioId)).thenReturn(0);
        when(aiJobRepository.findByIdempotencyKey(studioId, key)).thenReturn(Optional.of(existing));

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Prompt", key, true, List.of()
        );

        AiJobDetailResponse response = aiVisualizerService.createGenerationJob(actor, studioId, req);

        assertNotNull(response);
        assertEquals(existing.id(), response.id());
        verify(aiJobRepository, never()).createJob(any());
    }

    @Test
    @DisplayName("AI Kill Switch: When properties.enabled is false, generation is immediately blocked")
    void aiKillSwitch_whenDisabled_blocksGeneration() {
        properties.setEnabled(false);

        CreateAiJobRequest req = new CreateAiJobRequest(
                inputMediaId, projectId, "Prompt", "idem-kill-1", true, List.of()
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req));
        assertTrue(ex.getMessage().contains("disabled by administrator"));
    }

    @Test
    @DisplayName("Provider 429: Rate limit failure from provider updates job to FAILED with PROVIDER_QUOTA_REACHED")
    void rateLimit_provider429_failsGracefully() {
        mockAiImageProvider.setSimulationMode(MockAiImageProvider.SimulationMode.RATE_LIMIT);
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));

        UUID jobId = UuidV7.randomUuid();
        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Prompt", null, AiJobStatus.QUEUED, null, null, 0,
                null, userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        aiVisualizerService.executeJobInternal(jobId);

        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.FAILED), any(), any(), any(), eq("PROVIDER_QUOTA_REACHED"), any(), any(), any(), eq(1L));
    }

    @Test
    @DisplayName("Provider Safety Rejection: Content moderation rejection terminates job without retries")
    void safetyFilter_providerRejection_failsWithoutRetry() {
        mockAiImageProvider.setSimulationMode(MockAiImageProvider.SimulationMode.REJECTION);
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));

        UUID jobId = UuidV7.randomUuid();
        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Unsafe prompt", null, AiJobStatus.QUEUED, null, null, 0,
                null, userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        aiVisualizerService.executeJobInternal(jobId);

        assertEquals(1, mockAiImageProvider.getSubmissionCount());
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.FAILED), any(), any(), any(), eq("AI_SAFETY_REJECTED"), any(), any(), any(), eq(1L));
    }

    @Test
    @DisplayName("Provider Auth Failure: HTTP 401/403 terminates job without retries")
    void authFailure_provider401_failsGracefully() {
        mockAiImageProvider.setSimulationMode(MockAiImageProvider.SimulationMode.AUTH_FAILURE);
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));

        UUID jobId = UuidV7.randomUuid();
        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Prompt", null, AiJobStatus.QUEUED, null, null, 0,
                null, userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        aiVisualizerService.executeJobInternal(jobId);

        assertEquals(1, mockAiImageProvider.getSubmissionCount());
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.FAILED), any(), any(), any(), eq("AI_AUTH_FAILED"), any(), any(), any(), eq(1L));
    }

    @Test
    @DisplayName("Malformed Output: Corrupted image data fails closed with INGESTION_ERROR without saving broken asset")
    void malformedOutput_failsClosed() {
        mockAiImageProvider.setSimulationMode(MockAiImageProvider.SimulationMode.MALFORMED_OUTPUT);
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(sampleInputMedia));

        UUID jobId = UuidV7.randomUuid();
        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Prompt", null, AiJobStatus.QUEUED, null, null, 0,
                null, userId, Instant.now(), null, null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        aiVisualizerService.executeJobInternal(jobId);

        verify(mediaRepository, never()).createMediaAsset(any());
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.FAILED), any(), any(), any(), eq("INGESTION_ERROR"), any(), any(), any(), eq(1L));
    }

    @Test
    @DisplayName("Crash Recovery: Stuck processing jobs past timeout are safely reconciled to FAILED")
    void stuckJobRecovery_reconcilesToFailed() {
        UUID stuckJobId = UuidV7.randomUuid();
        AiJobRecord stuckJob = new AiJobRecord(
                stuckJobId, studioId, projectId, inputMediaId, null, "mock_configured", null,
                "Prompt", null, AiJobStatus.PROCESSING, null, null, 1,
                null, userId, Instant.now().minus(Duration.ofMinutes(15)),
                Instant.now().minus(Duration.ofMinutes(15)), null, null, null,
                0L, true, EditingMode.FULL_IMAGE, null
        );
        when(aiJobRepository.findStuckJobs(eq(AiJobStatus.PROCESSING), any(Instant.class)))
                .thenReturn(List.of(stuckJob));

        int reconciled = aiVisualizerService.reconcileStuckProcessingJobs(Duration.ofMinutes(10));

        assertEquals(1, reconciled);
        verify(aiJobRepository).updateStatus(eq(stuckJobId), eq(AiJobStatus.FAILED), any(), any(), any(Instant.class), eq("PROCESSING_TIMEOUT"), any(), any(), any(), eq(0L));
    }
}
