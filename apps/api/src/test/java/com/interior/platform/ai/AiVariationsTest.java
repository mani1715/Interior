package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.*;
import com.interior.platform.ai.dto.AiJobDetailResponse;
import com.interior.platform.ai.dto.AiJobHistoryResponse;
import com.interior.platform.ai.dto.CreateVariationRequest;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.repository.AiClientReviewRepository;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
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

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiVariationsTest {

    @Mock private AiJobRepository aiJobRepository;
    @Mock private AiReferenceRepository aiReferenceRepository;
    @Mock private AiClientReviewRepository aiClientReviewRepository;
    @Mock private AiImageProvider aiImageProvider;
    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;
    @Mock private StorageService storageService;
    @Mock private ImageProcessingService imageProcessingService;

    private RateLimiterService rateLimiterService;
    private AiVisualizerProperties properties;
    private AuthorizationService authorizationService;
    private AiVisualizerService aiVisualizerService;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private UUID inputMediaId;
    private UUID outputMediaId;
    private ActorContext actor;
    private StudioProjectRecord sampleProject;
    private AiJobRecord parentJob;

    @BeforeEach
    void setUp() {
        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();
        inputMediaId = UuidV7.randomUuid();
        outputMediaId = UuidV7.randomUuid();

        actor = new ActorContext(
                userId, "Test Designer", "designer@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        authorizationService = new AuthorizationService();

        properties = new AiVisualizerProperties();
        properties.setProvider("stability");
        properties.setApiKey("test-key-123");
        properties.setDailyStudioLimit(50);
        properties.setMaxPromptLength(500);
        properties.setSupportsReferenceImages(true);
        properties.setMaxReferenceImages(4);

        aiVisualizerService = new AiVisualizerService(
                aiJobRepository,
                aiReferenceRepository,
                aiClientReviewRepository,
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
                projectId, studioId, "living-room", "Living Room Remodel",
                "Short description", "Full description",
                com.interior.platform.projects.domain.ProjectCategory.LIVING_ROOM,
                com.interior.platform.projects.domain.ProjectStatus.READY,
                com.interior.platform.projects.domain.VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024, BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userId, Instant.now(), Instant.now(), null
        );

        parentJob = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                outputMediaId,
                "stability",
                "prov-123",
                "Scandinavian living room with warm wood",
                null,
                AiJobStatus.SUCCEEDED,
                null,
                null,
                0,
                "idemp-1",
                userId,
                Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(90),
                Instant.now().minusSeconds(10),
                Instant.now(),
                null,
                0,
                true,
                EditingMode.FULL_IMAGE,
                null,
                null,
                null,
                false,
                false,
                "Base Concept"
        );

        UserRecord user = new UserRecord(userId, "Test Designer", "designer@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L);
        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        StudioMemberRecord member = new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Test Studio", "test-studio", userId, "OWNER", Instant.now());
        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(member));
        lenient().when(aiImageProvider.isConfigured()).thenReturn(true);
        lenient().when(aiImageProvider.getProviderKey()).thenReturn("stability");
    }

    @Test
    @DisplayName("Create variation with REFINE_ORIGINAL uses parent's inputMediaId and preserves root lineage")
    void createVariationRefineOriginal() {
        when(aiJobRepository.findById(studioId, parentJob.id())).thenReturn(Optional.of(parentJob));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(2);
        when(aiJobRepository.countByStudio(studioId, projectId)).thenReturn(1);

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.REFINE_ORIGINAL,
                "Slightly lighter timber tone",
                true,
                EditingMode.FULL_IMAGE,
                false,
                null,
                null,
                null
        );

        AiJobDetailResponse response = aiVisualizerService.createVariation(actor, studioId, parentJob.id(), req);

        assertThat(response).isNotNull();
        ArgumentCaptor<AiJobRecord> captor = ArgumentCaptor.forClass(AiJobRecord.class);
        verify(aiJobRepository).createJob(captor.capture());

        AiJobRecord savedJob = captor.getValue();
        assertThat(savedJob.parentJobId()).isEqualTo(parentJob.id());
        assertThat(savedJob.rootJobId()).isEqualTo(parentJob.id());
        assertThat(savedJob.inputMediaId()).isEqualTo(parentJob.inputMediaId());
        assertThat(savedJob.prompt()).isEqualTo("Slightly lighter timber tone");
        assertThat(savedJob.status()).isEqualTo(AiJobStatus.QUEUED);

        verify(auditService).record(eq(userId), eq(studioId), eq("AI_VARIATION_SUBMITTED"), eq("AI_JOB"), anyString(), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Create variation with EVOLVE_CONCEPT uses parent's outputMediaId as new input")
    void createVariationEvolveConcept() {
        when(aiJobRepository.findById(studioId, parentJob.id())).thenReturn(Optional.of(parentJob));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(5);
        when(aiJobRepository.countByStudio(studioId, projectId)).thenReturn(2);

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.EVOLVE_CONCEPT,
                "Add brass floor lamp",
                true,
                EditingMode.FULL_IMAGE,
                false,
                null,
                null,
                null
        );

        AiJobDetailResponse response = aiVisualizerService.createVariation(actor, studioId, parentJob.id(), req);

        assertThat(response).isNotNull();
        ArgumentCaptor<AiJobRecord> captor = ArgumentCaptor.forClass(AiJobRecord.class);
        verify(aiJobRepository).createJob(captor.capture());

        AiJobRecord savedJob = captor.getValue();
        assertThat(savedJob.parentJobId()).isEqualTo(parentJob.id());
        assertThat(savedJob.inputMediaId()).isEqualTo(parentJob.outputMediaId());
    }

    @Test
    @DisplayName("Propagates existing rootJobId across deep variation chains")
    void propagatesRootJobId() {
        UUID grandParentId = UuidV7.randomUuid();
        AiJobRecord childJob = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                outputMediaId,
                "stability",
                "prov-123",
                "Warm modern",
                null,
                AiJobStatus.SUCCEEDED,
                null,
                null,
                0,
                null,
                userId,
                Instant.now(),
                null,
                null,
                null,
                null,
                0,
                true,
                EditingMode.FULL_IMAGE,
                null,
                parentJob.id(),
                grandParentId,
                false,
                false,
                "Variation 2"
        );

        when(aiJobRepository.findById(studioId, childJob.id())).thenReturn(Optional.of(childJob));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(1);
        when(aiJobRepository.countByStudio(studioId, projectId)).thenReturn(3);

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.REFINE_ORIGINAL,
                "Another refinement",
                true,
                null,
                false,
                null,
                null,
                null
        );

        aiVisualizerService.createVariation(actor, studioId, childJob.id(), req);

        ArgumentCaptor<AiJobRecord> captor = ArgumentCaptor.forClass(AiJobRecord.class);
        verify(aiJobRepository).createJob(captor.capture());
        assertThat(captor.getValue().rootJobId()).isEqualTo(grandParentId);
    }

    @Test
    @DisplayName("Rejects variation if parent job is not SUCCEEDED")
    void rejectsFailedParentJob() {
        AiJobRecord failedParent = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                null,
                "stability",
                null,
                "Scandinavian",
                null,
                AiJobStatus.FAILED,
                "Generation timed out",
                null,
                0,
                null,
                userId,
                Instant.now(),
                null,
                null,
                null,
                null,
                0,
                true,
                EditingMode.FULL_IMAGE,
                null,
                null,
                null,
                false,
                false,
                "Base Concept"
        );

        when(aiJobRepository.findById(studioId, failedParent.id())).thenReturn(Optional.of(failedParent));

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.REFINE_ORIGINAL,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> aiVisualizerService.createVariation(actor, studioId, failedParent.id(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create a variation from a job that has not succeeded");
    }

    @Test
    @DisplayName("Rejects variation when daily quota is exceeded")
    void rejectsWhenDailyQuotaExceeded() {
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(50);

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.REFINE_ORIGINAL,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> aiVisualizerService.createVariation(actor, studioId, parentJob.id(), req))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Daily AI concept generation quota reached");
    }

    @Test
    @DisplayName("Precision mask variation can reuse parent mask")
    void precisionMaskVariationReusesParentMask() {
        AiJobRecord precisionParent = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                outputMediaId,
                "stability",
                "prov-123",
                "Wardrobe edit",
                null,
                AiJobStatus.SUCCEEDED,
                null,
                null,
                0,
                null,
                userId,
                Instant.now(),
                null,
                null,
                null,
                null,
                0,
                true,
                EditingMode.PRECISION_MASK,
                "studio/" + studioId + "/masks/mask-123.png",
                null,
                null,
                false,
                false,
                "Wardrobe Concept"
        );

        when(aiJobRepository.findById(studioId, precisionParent.id())).thenReturn(Optional.of(precisionParent));
        when(aiJobRepository.countTodayUsage(studioId)).thenReturn(3);
        when(aiJobRepository.countByStudio(studioId, projectId)).thenReturn(2);
        when(aiImageProvider.supportsMaskEditing()).thenReturn(true);

        CreateVariationRequest req = new CreateVariationRequest(
                VariationStrategy.REFINE_ORIGINAL,
                "Dark walnut laminate on shutters",
                true,
                EditingMode.PRECISION_MASK,
                true,
                null,
                null,
                null
        );

        aiVisualizerService.createVariation(actor, studioId, precisionParent.id(), req);

        ArgumentCaptor<AiJobRecord> captor = ArgumentCaptor.forClass(AiJobRecord.class);
        verify(aiJobRepository).createJob(captor.capture());
        AiJobRecord saved = captor.getValue();
        assertThat(saved.editingMode()).isEqualTo(EditingMode.PRECISION_MASK);
        assertThat(saved.maskStorageKey()).isEqualTo("studio/" + studioId + "/masks/mask-123.png");
    }

    @Test
    @DisplayName("Toggle shortlist flips shortlist state and persists")
    void toggleShortlist() {
        when(aiJobRepository.findById(studioId, parentJob.id()))
                .thenReturn(Optional.of(parentJob))
                .thenReturn(Optional.of(parentJob.withShortlist(true)));

        AiJobDetailResponse res = aiVisualizerService.toggleShortlist(actor, studioId, parentJob.id());

        verify(aiJobRepository).updateShortlist(studioId, parentJob.id(), true);
        assertThat(res.isShortlisted()).isTrue();
    }

    @Test
    @DisplayName("Toggle studio selected flips studio selection state and persists")
    void toggleStudioSelected() {
        when(aiJobRepository.findById(studioId, parentJob.id()))
                .thenReturn(Optional.of(parentJob))
                .thenReturn(Optional.of(parentJob.withStudioSelected(true)));

        AiJobDetailResponse res = aiVisualizerService.toggleStudioSelected(actor, studioId, parentJob.id());

        verify(aiJobRepository).updateStudioSelected(studioId, parentJob.id(), true);
        assertThat(res.isStudioSelected()).isTrue();
    }

    @Test
    @DisplayName("List history returns paginated jobs and total count")
    void listJobHistory() {
        when(aiJobRepository.findHistory(studioId, projectId, null, null, false, 20, 0))
                .thenReturn(List.of(parentJob));
        when(aiJobRepository.countHistory(studioId, projectId, null, null, false))
                .thenReturn(1L);

        AiJobHistoryResponse res = aiVisualizerService.listJobHistory(actor, studioId, projectId, null, null, false, 0, 20);

        assertThat(res.items()).hasSize(1);
        assertThat(res.totalItems()).isEqualTo(1L);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.totalPages()).isEqualTo(1);
    }
}
