package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.ai.dto.AiJobDetailResponse;
import com.interior.platform.ai.dto.CreateAiJobRequest;
import com.interior.platform.ai.dto.UploadMaskResponse;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.provider.ProviderGenerationResponse;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.StudioProjectRecord;
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
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPrecisionEditingTest {

    @Mock private AiJobRepository aiJobRepository;
    @Mock private AiReferenceRepository aiReferenceRepository;
    @Mock private AiImageProvider aiImageProvider;
    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;
    @Mock private StorageService storageService;

    private RateLimiterService rateLimiterService;
    private ImageProcessingService imageProcessingService;
    private AiVisualizerProperties properties;
    private AuthorizationService authorizationService;
    private AiVisualizerService aiVisualizerService;

    private UUID userId;
    private UUID studioId;
    private UUID otherStudioId;
    private UUID projectId;
    private UUID inputMediaId;
    private ActorContext actor;
    private StudioProjectRecord sampleProject;
    private MediaAssetRecord inputAsset;

    @BeforeEach
    void setUp() {
        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        otherStudioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();
        inputMediaId = UuidV7.randomUuid();

        actor = new ActorContext(
                userId, "Test Designer", "designer@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        imageProcessingService = new ImageProcessingService();
        properties = new AiVisualizerProperties();
        properties.setProvider("stability");
        properties.setApiKey("test-key-123");
        properties.setSupportsMaskEditing(true);
        properties.setDailyStudioLimit(50);
        properties.setMaxPromptLength(500);

        authorizationService = new AuthorizationService();

        aiVisualizerService = new AiVisualizerService(
                aiJobRepository,
                aiReferenceRepository,
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

        sampleProject = new StudioProjectRecord(
                projectId, studioId, "master-bedroom", "Master Bedroom Remodel",
                "Short description", "Full description",
                com.interior.platform.projects.domain.ProjectCategory.LIVING_ROOM,
                com.interior.platform.projects.domain.ProjectStatus.READY,
                com.interior.platform.projects.domain.VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024, BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userId, Instant.now(), Instant.now(), null
        );

        inputAsset = new MediaAssetRecord(
                inputMediaId, studioId, projectId, MediaType.BEFORE, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "studio/" + studioId + "/input.png", "image/png",
                1024L, 800, 600, 0, false, null, null, false,
                userId, Instant.now(), Instant.now(), null
        );
    }

    private void mockAuth() {
        UserRecord user = new UserRecord(userId, "Test Designer", "designer@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L);
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));

        StudioMemberRecord member = new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Test Studio", "test-studio", userId, "OWNER", Instant.now());
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(member));
    }

    private byte[] createMaskPng(int width, int height, boolean paintRegion, double regionFraction) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Clear transparent
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);

        if (paintRegion) {
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setColor(new Color(184, 138, 90, 200)); // Bronze overlay
            int paintWidth = (int) (width * regionFraction);
            g2d.fillRect(0, 0, paintWidth, height);
        }
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should successfully upload a valid precision mask matching input dimensions")
    void testUploadMaskSuccess() throws Exception {
        mockAuth();
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        byte[] maskBytes = createMaskPng(800, 600, true, 0.25);
        MockMultipartFile file = new MockMultipartFile("file", "mask.png", "image/png", maskBytes);

        UploadMaskResponse response = aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file);

        assertThat(response).isNotNull();
        assertThat(response.maskId()).isNotNull();
        assertThat(response.maskStorageKey()).startsWith("studio/" + studioId + "/masks/");
        assertThat(response.width()).isEqualTo(800);
        assertThat(response.height()).isEqualTo(600);
        assertThat(response.coverageRatio()).isBetween(0.20, 0.30);

        verify(storageService).store(eq(response.maskStorageKey()), eq(maskBytes), eq("image/png"));
    }

    @Test
    @DisplayName("Should reject upload when mask has no painted pixels (empty mask)")
    void testUploadEmptyMaskFails() throws Exception {
        mockAuth();
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        byte[] emptyMask = createMaskPng(800, 600, false, 0.0);
        MockMultipartFile file = new MockMultipartFile("file", "mask.png", "image/png", emptyMask);

        assertThatThrownBy(() -> aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("The mask is empty");
    }

    @Test
    @DisplayName("Should reject upload when mask covers almost the entire image (>98%)")
    void testUploadExcessiveCoverageMaskFails() throws Exception {
        mockAuth();
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        byte[] fullMask = createMaskPng(800, 600, true, 0.99);
        MockMultipartFile file = new MockMultipartFile("file", "mask.png", "image/png", fullMask);

        assertThatThrownBy(() -> aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("covers almost the entire image");
    }

    @Test
    @DisplayName("Should reject upload when mask dimensions do not match source image")
    void testUploadMismatchedDimensionsFails() throws Exception {
        mockAuth();
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        byte[] wrongSizeMask = createMaskPng(1024, 768, true, 0.3);
        MockMultipartFile file = new MockMultipartFile("file", "mask.png", "image/png", wrongSizeMask);

        assertThatThrownBy(() -> aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must match input media dimensions");
    }

    @Test
    @DisplayName("Should reject upload when input media belongs to another studio")
    void testUploadMaskCrossTenantFails() throws Exception {
        mockAuth();
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.empty());

        byte[] maskBytes = createMaskPng(800, 600, true, 0.25);
        MockMultipartFile file = new MockMultipartFile("file", "mask.png", "image/png", maskBytes);

        assertThatThrownBy(() -> aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Input media asset not found in studio");
    }

    @Test
    @DisplayName("Should create PRECISION_MASK job when valid maskStorageKey provided and provider supports it")
    void testCreatePrecisionMaskJobSuccess() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.supportsMaskEditing()).thenReturn(true);
        when(aiImageProvider.getProviderKey()).thenReturn("stability");
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        String maskKey = "studio/" + studioId + "/masks/" + UUID.randomUUID() + ".png";
        CreateAiJobRequest request = new CreateAiJobRequest(
                inputMediaId,
                projectId,
                "Change only the wardrobe shutters to fluted oak",
                null,
                true,
                List.of(),
                EditingMode.PRECISION_MASK,
                maskKey
        );

        AiJobDetailResponse response = aiVisualizerService.createGenerationJob(actor, studioId, request);

        assertThat(response).isNotNull();
        assertThat(response.editingMode()).isEqualTo(EditingMode.PRECISION_MASK);
        assertThat(response.maskPreviewUrl()).isEqualTo("/api/ai/jobs/" + response.id() + "/mask");

        ArgumentCaptor<AiJobRecord> jobCaptor = ArgumentCaptor.forClass(AiJobRecord.class);
        verify(aiJobRepository).createJob(jobCaptor.capture());

        AiJobRecord created = jobCaptor.getValue();
        assertThat(created.editingMode()).isEqualTo(EditingMode.PRECISION_MASK);
        assertThat(created.maskStorageKey()).isEqualTo(maskKey);
    }

    @Test
    @DisplayName("Should reject PRECISION_MASK job when provider does not support mask editing")
    void testCreatePrecisionMaskJobProviderUnsupported() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.supportsMaskEditing()).thenReturn(false);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        String maskKey = "studio/" + studioId + "/masks/" + UUID.randomUUID() + ".png";
        CreateAiJobRequest request = new CreateAiJobRequest(
                inputMediaId,
                projectId,
                "Change only this wall color",
                null,
                true,
                List.of(),
                EditingMode.PRECISION_MASK,
                maskKey
        );

        assertThatThrownBy(() -> aiVisualizerService.createGenerationJob(actor, studioId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not support precision mask editing");
    }

    @Test
    @DisplayName("Should reject PRECISION_MASK job when maskStorageKey is missing")
    void testCreatePrecisionMaskJobMissingKey() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.supportsMaskEditing()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        CreateAiJobRequest request = new CreateAiJobRequest(
                inputMediaId,
                projectId,
                "Change only this wall color",
                null,
                true,
                List.of(),
                EditingMode.PRECISION_MASK,
                null
        );

        assertThatThrownBy(() -> aiVisualizerService.createGenerationJob(actor, studioId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maskStorageKey is required");
    }

    @Test
    @DisplayName("Should reject PRECISION_MASK job when maskStorageKey belongs to another studio")
    void testCreatePrecisionMaskJobCrossTenantMaskKey() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiImageProvider.supportsMaskEditing()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));

        String otherStudioMaskKey = "studio/" + otherStudioId + "/masks/" + UUID.randomUUID() + ".png";
        CreateAiJobRequest request = new CreateAiJobRequest(
                inputMediaId,
                projectId,
                "Change only this wall color",
                null,
                true,
                List.of(),
                EditingMode.PRECISION_MASK,
                otherStudioMaskKey
        );

        assertThatThrownBy(() -> aiVisualizerService.createGenerationJob(actor, studioId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mask must belong to the current studio");
    }

    @Test
    @DisplayName("Should execute PRECISION_MASK job passing mask bytes to provider")
    void testExecutePrecisionMaskJob() throws Exception {
        UUID jobId = UuidV7.randomUuid();
        String maskKey = "studio/" + studioId + "/masks/mask-123.png";
        byte[] maskBytes = new byte[]{1, 2, 3, 4};
        byte[] inputBytes = createMaskPng(800, 600, false, 0.0);
        byte[] outputBytes = createMaskPng(800, 600, true, 0.3);

        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", null,
                "Change only countertop to black granite", null, AiJobStatus.QUEUED, null, null,
                0, null, userId, Instant.now(), null, null, null, null, 0L,
                true, EditingMode.PRECISION_MASK, maskKey
        );

        when(aiJobRepository.findByIdGlobal(jobId)).thenReturn(Optional.of(job));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));
        when(storageService.load(inputAsset.originalStorageKey())).thenReturn(inputBytes);
        when(storageService.load(maskKey)).thenReturn(maskBytes);

        StudioDetailRecord studioDetail = mock(StudioDetailRecord.class);
        when(studioDetail.name()).thenReturn("Studio Luxe");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioDetail));

        when(aiImageProvider.submitGeneration(eq(job), eq(inputBytes), eq("image/png"), eq(maskBytes), eq("image/png"), anyList()))
                .thenReturn(ProviderGenerationResponse.immediateSuccess(outputBytes, "image/png", "{}"));
        when(aiImageProvider.getProviderKey()).thenReturn("stability");

        aiVisualizerService.executeJobInternal(jobId);

        verify(aiImageProvider).submitGeneration(eq(job), eq(inputBytes), eq("image/png"), eq(maskBytes), eq("image/png"), anyList());
        verify(aiJobRepository).updateStatus(eq(jobId), eq(AiJobStatus.SUCCEEDED), any(), any(), isNull(), isNull(), isNull(), any(UUID.class), anyString(), eq(1L));
    }

    @Test
    @DisplayName("Should retrieve private mask bytes with tenant isolation")
    void testGetJobMaskSuccess() {
        mockAuth();
        UUID jobId = UuidV7.randomUuid();
        String maskKey = "studio/" + studioId + "/masks/mask-456.png";
        byte[] maskBytes = new byte[]{11, 22, 33};

        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", null,
                "Edit region", null, AiJobStatus.SUCCEEDED, null, null,
                0, null, userId, Instant.now(), null, null, null, null, 0L,
                true, EditingMode.PRECISION_MASK, maskKey
        );

        when(aiJobRepository.findById(studioId, jobId)).thenReturn(Optional.of(job));
        when(storageService.load(maskKey)).thenReturn(maskBytes);

        byte[] result = aiVisualizerService.getJobMask(actor, studioId, jobId);

        assertThat(result).isEqualTo(maskBytes);
    }

    @Test
    @DisplayName("Should reject mask retrieval if job is not PRECISION_MASK")
    void testGetJobMaskFullImageFails() {
        mockAuth();
        UUID jobId = UuidV7.randomUuid();

        AiJobRecord job = new AiJobRecord(
                jobId, studioId, projectId, inputMediaId, null, "stability", null,
                "Full concept", null, AiJobStatus.SUCCEEDED, null, null,
                0, null, userId, Instant.now(), null, null, null, null, 0L,
                true, EditingMode.FULL_IMAGE, null
        );

        when(aiJobRepository.findById(studioId, jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> aiVisualizerService.getJobMask(actor, studioId, jobId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No mask associated with this job");
    }

    @Test
    @DisplayName("Regression: Reject maskStorageKey with path traversal or invalid format")
    void testRejectMaskStorageKeyTraversalAndInvalidFormat() {
        mockAuth();
        when(aiImageProvider.isConfigured()).thenReturn(true);
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(0);
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(mediaRepository.findMediaAsset(inputMediaId, studioId)).thenReturn(Optional.of(inputAsset));
        when(aiImageProvider.supportsMaskEditing()).thenReturn(true);

        // Path traversal attempt
        CreateAiJobRequest traversalReq = new CreateAiJobRequest(
                inputMediaId, projectId, "Paint wardrobe dark wood", null, true, List.of(),
                EditingMode.PRECISION_MASK, "studio/" + studioId + "/masks/../../etc/passwd.png"
        );

        assertThatThrownBy(() -> aiVisualizerService.createGenerationJob(actor, studioId, traversalReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid maskStorageKey");

        // Invalid extension
        CreateAiJobRequest invalidExtReq = new CreateAiJobRequest(
                inputMediaId, projectId, "Paint wardrobe dark wood", null, true, List.of(),
                EditingMode.PRECISION_MASK, "studio/" + studioId + "/masks/mask-123.jpg"
        );

        assertThatThrownBy(() -> aiVisualizerService.createGenerationJob(actor, studioId, invalidExtReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid maskStorageKey");
    }
}
