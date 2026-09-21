package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.dto.AiJobDetailResponse;
import com.interior.platform.ai.dto.AiStudioStatusResponse;
import com.interior.platform.ai.dto.CreateAiJobRequest;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.provider.ProviderGenerationResponse;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.media.storage.LocalStorageService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.projects.domain.*;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiVisualizerServiceTest {

    @Mock private AiJobRepository aiJobRepository;
    @Mock private AiImageProvider aiImageProvider;
    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;

    private RateLimiterService rateLimiterService;
    private StorageService storageService;
    private ImageProcessingService imageProcessingService;
    private AiVisualizerProperties properties;
    private AuthorizationService authorizationService;
    private AiVisualizerService aiVisualizerService;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private UUID inputMediaId;
    private ActorContext actor;
    private StudioProjectRecord sampleProject;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() throws IOException {
        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();
        inputMediaId = UuidV7.randomUuid();

        actor = new ActorContext(
                userId, "Studio Designer", "designer@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        Path tempDir = Files.createTempDirectory("ai-visualizer-test-storage");
        storageService = new LocalStorageService(tempDir.toString(), "http://localhost:8080/api/v1/media/public");
        imageProcessingService = new ImageProcessingService();
        authorizationService = new AuthorizationService();

        properties = new AiVisualizerProperties();
        properties.setProvider("stability");
        properties.setApiKey("test-key-123");
        properties.setDailyStudioLimit(50);
        properties.setMaxPromptLength(500);

        sampleProject = new StudioProjectRecord(
                projectId, studioId, "living-room", "Living Room Remodel",
                "Short description", "Full description",
                ProjectCategory.LIVING_ROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userId, Instant.now(), Instant.now(), null
        );

        aiVisualizerService = new AiVisualizerService(
                aiJobRepository,
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

        // Generate valid test image
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(250, 248, 245));
        g.fillRect(0, 0, 800, 600);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        sampleImageBytes = baos.toByteArray();
    }

    private void mockAuth() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(
                new UserRecord(userId, "Studio Designer", "designer@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L)
        ));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Aura Studio", "aura-studio", userId, "OWNER", Instant.now())
        ));
    }

    @Test
    @DisplayName("Should return studio status with quota remaining")
    void testGetStudioStatus() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.getProviderKey()).thenReturn("stability");
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(5);

        AiStudioStatusResponse status = aiVisualizerService.getStudioStatus(actor, studioId);

        assertTrue(status.isConfigured());
        assertEquals("stability", status.providerKey());
        assertEquals(50, status.dailyQuota());
        assertEquals(5, status.usedToday());
        assertEquals(45, status.remainingToday());
    }

    @Test
    @DisplayName("Should throw AiProviderNotConfiguredException when provider is not configured")
    void testCreateJobThrowsWhenUnconfigured() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(false);

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Add modern oak floors and warm recessed lighting", null);

        assertThrows(AiProviderNotConfiguredException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req)
        );
    }

    @Test
    @DisplayName("Should reject generation when daily quota is exceeded")
    void testDailyQuotaExceeded() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(50);

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Add warm lighting", null);

        assertThrows(RateLimitExceededException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req)
        );
    }

    @Test
    @DisplayName("Should return existing job when idempotency key is matched")
    void testIdempotencyDeduplication() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(2);

        String idempKey = "idemp-abc-123";
        AiJobRecord existingJob = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                null,
                "stability",
                null,
                "Add warm lighting",
                null,
                AiJobStatus.QUEUED,
                null,
                null,
                0,
                idempKey,
                userId,
                Instant.now(),
                null,
                null,
                null,
                null,
                0L
        );
        when(aiJobRepository.findByIdempotencyKey(studioId, idempKey)).thenReturn(Optional.of(existingJob));

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Add warm lighting", idempKey);
        AiJobDetailResponse res = aiVisualizerService.createGenerationJob(actor, studioId, req);

        assertEquals(existingJob.id(), res.id());
        verify(aiJobRepository, never()).createJob(any());
    }

    @Test
    @DisplayName("Should reject when input media is CLIENT_PRIVATE")
    void testRejectClientPrivateInput() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));

        MediaAssetRecord privateAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.CLIENT_PRIVATE, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "orig.jpg", "image/jpeg", 1000L, 800, 600, 0, false, null, null, false,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(privateAsset));

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Add scandinavian minimal furniture", null);

        assertThrows(BadRequestException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req)
        );
    }

    @Test
    @DisplayName("Should reject when input media processing status is not READY")
    void testRejectPendingInputMedia() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));

        MediaAssetRecord pendingAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.BEFORE, MediaVisibility.PUBLIC,
                MediaProcessingStatus.PROCESSING, "orig.jpg", "image/jpeg", 1000L, 800, 600, 0, false, null, null, true,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(pendingAsset));

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Add scandinavian minimal furniture", null);

        assertThrows(BadRequestException.class, () ->
                aiVisualizerService.createGenerationJob(actor, studioId, req)
        );
    }

    @Test
    @DisplayName("Should submit job successfully and dispatch execution")
    void testCreateGenerationJobSuccess() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.getProviderKey()).thenReturn("stability");
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));

        MediaAssetRecord validAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.BEFORE, MediaVisibility.PUBLIC,
                MediaProcessingStatus.READY, "orig.jpg", "image/jpeg", 1000L, 800, 600, 0, false, null, null, true,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(validAsset));

        CreateAiJobRequest req = new CreateAiJobRequest(inputMediaId, projectId, "Transform into modern warm minimalist with oak accents", "idemp-1");
        AiJobDetailResponse res = aiVisualizerService.createGenerationJob(actor, studioId, req);

        assertNotNull(res.id());
        assertEquals(AiJobStatus.QUEUED, res.status());
        assertEquals("stability", res.providerKey());
        verify(aiJobRepository).createJob(any());
        verify(auditService).record(eq(userId), eq(studioId), eq("AI_GENERATION_SUBMITTED"), eq("AI_JOB"), anyString(), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Should execute job pipeline synchronously to SUCCEEDED and ingest AI Concept media")
    void testExecuteJobInternalSuccess() {
        UUID jobId = UuidV7.randomUuid();
        String originalKey = "studios/" + studioId + "/input.jpg";
        storageService.store(originalKey, sampleImageBytes, "image/jpeg");

        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", null,
                "Transform to mid-century modern", null, AiJobStatus.QUEUED, null, null,
                0, null, userId, Instant.now(), null, null, null, null, 0L
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        MediaAssetRecord inputAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.BEFORE, MediaVisibility.PUBLIC,
                MediaProcessingStatus.READY, originalKey, "image/jpeg", sampleImageBytes.length, 800, 600, 0, false, null, null, true,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        StudioDetailRecord studioDetail = mock(StudioDetailRecord.class);
        when(studioDetail.name()).thenReturn("Studio Luxe");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioDetail));

        // Provider returns generated sample image
        when(aiImageProvider.submitGeneration(eq(job), any(), eq("image/jpeg")))
                .thenReturn(ProviderGenerationResponse.immediateSuccess(sampleImageBytes, "image/jpeg", "{\"model\":\"sdxl\"}"));
        when(aiImageProvider.getProviderKey()).thenReturn("stability");

        // Run execution pipeline
        aiVisualizerService.executeJobInternal(jobId);

        // Verify status marked PROCESSING then SUCCEEDED
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.PROCESSING), any(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0L));
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.SUCCEEDED), isNull(), any(), isNull(), isNull(), isNull(), any(UUID.class), anyString(), eq(1L));

        // Verify AI_CONCEPT media asset is PRIVATE by default (no public derivatives)
        verify(mediaRepository).createMediaAsset(argThat(asset -> asset.mediaType() == MediaType.AI_CONCEPT && asset.visibility() == MediaVisibility.PRIVATE));
        verify(mediaRepository, never()).saveDerivatives(any());

        // Verify usage event recorded
        verify(aiJobRepository).recordUsageEvent(argThat(event -> "GENERATION_SUCCESS".equals(event.eventType())));
        verify(auditService).record(eq(userId), eq(studioId), eq("AI_GENERATION_SUCCEEDED"), eq("AI_JOB"), eq(jobId.toString()), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Should mark job FAILED when provider returns failure")
    void testExecuteJobInternalFailure() {
        UUID jobId = UuidV7.randomUuid();
        String originalKey = "studios/" + studioId + "/input.jpg";
        storageService.store(originalKey, sampleImageBytes, "image/jpeg");

        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", null,
                "Transform to industrial loft", null, AiJobStatus.QUEUED, null, null,
                0, null, userId, Instant.now(), null, null, null, null, 0L
        );
        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));

        MediaAssetRecord inputAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.BEFORE, MediaVisibility.PUBLIC,
                MediaProcessingStatus.READY, originalKey, "image/jpeg", sampleImageBytes.length, 800, 600, 0, false, null, null, true,
                userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        when(aiImageProvider.submitGeneration(eq(job), any(), eq("image/jpeg")))
                .thenReturn(ProviderGenerationResponse.failure("PROVIDER_OVERLOADED", "Service temporarily overloaded"));
        when(aiImageProvider.getProviderKey()).thenReturn("stability");

        aiVisualizerService.executeJobInternal(jobId);

        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.FAILED), isNull(), isNull(), any(), eq("PROVIDER_OVERLOADED"), anyString(), isNull(), isNull(), eq(1L));
        verify(aiJobRepository).recordUsageEvent(argThat(event -> "GENERATION_FAILED".equals(event.eventType())));
        verify(auditService).record(eq(userId), eq(studioId), eq("AI_GENERATION_FAILED"), eq("AI_JOB"), eq(jobId.toString()), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Should cancel non-terminal job successfully")
    void testCancelJobSuccess() {
        mockAuth();
        UUID jobId = UuidV7.randomUuid();
        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", "prov-job-123",
                "Transform to bohemian style", null, AiJobStatus.PROCESSING, null, null,
                1, null, userId, Instant.now(), Instant.now(), null, null, null, 1L
        );
        when(aiJobRepository.findById(studioId, jobId)).thenReturn(Optional.of(job));
        when(aiImageProvider.getProviderKey()).thenReturn("stability");

        AiJobDetailResponse cancelled = aiVisualizerService.cancelJob(actor, studioId, jobId);

        verify(aiImageProvider).cancel("prov-job-123");
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.CANCELLED), isNull(), isNull(), any(), eq("CANCELLED_BY_USER"), anyString(), isNull(), isNull(), eq(1L));
        verify(aiJobRepository).recordUsageEvent(argThat(event -> "GENERATION_CANCELLED".equals(event.eventType())));
    }
}
