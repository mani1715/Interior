package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.*;
import com.interior.platform.ai.dto.*;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.repository.AiClientReviewRepository;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClientReviewTest {

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
    private AiJobRecord conceptJob;
    private AiClientReviewRecord reviewRecord;

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
                projectId, studioId, "master-bedroom", "Master Bedroom Design",
                "Description", "Full description",
                com.interior.platform.projects.domain.ProjectCategory.BEDROOM,
                com.interior.platform.projects.domain.ProjectStatus.READY,
                com.interior.platform.projects.domain.VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024, BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userId, Instant.now(), Instant.now(), null
        );

        conceptJob = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                inputMediaId,
                outputMediaId,
                "stability",
                "prov-123",
                "Warm minimalist master bedroom",
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
                null,
                null,
                true,
                false,
                "Option 1"
        );

        reviewRecord = new AiClientReviewRecord(
                UuidV7.randomUuid(),
                studioId,
                projectId,
                "Master Bedroom Concepts",
                "Please review these options",
                AiVisualizerService.hashSha256("test-raw-token"),
                ClientReviewStatus.OPEN,
                true,
                Instant.now().plus(7, ChronoUnit.DAYS),
                null,
                userId,
                Instant.now(),
                Instant.now(),
                0
        );

        UserRecord user = new UserRecord(userId, "Test Designer", "designer@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L);
        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        StudioMemberRecord member = new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Test Studio", "test-studio", userId, "OWNER", Instant.now());
        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(member));
    }

    @Test
    @DisplayName("Create client review hashes raw token and creates immutable items snapshot")
    void createClientReview() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(aiJobRepository.findById(studioId, conceptJob.id())).thenReturn(Optional.of(conceptJob));

        CreateClientReviewRequest req = new CreateClientReviewRequest(
                projectId,
                "Master Bedroom Options",
                "Take a look",
                7,
                true,
                List.of(conceptJob.id())
        );

        CreateClientReviewResponse res = aiVisualizerService.createClientReview(actor, studioId, req);

        assertThat(res).isNotNull();
        assertThat(res.rawToken()).isNotBlank();
        assertThat(res.reviewUrl()).isEqualTo("/review/" + res.rawToken());
        assertThat(res.title()).isEqualTo("Master Bedroom Options");

        ArgumentCaptor<AiClientReviewRecord> reviewCaptor = ArgumentCaptor.forClass(AiClientReviewRecord.class);
        verify(aiClientReviewRepository).createReview(reviewCaptor.capture());
        AiClientReviewRecord savedReview = reviewCaptor.getValue();

        assertThat(savedReview.tokenHash()).isNotEqualTo(res.rawToken().getBytes(StandardCharsets.UTF_8));
        assertThat(savedReview.tokenHash()).isEqualTo(AiVisualizerService.hashSha256(res.rawToken()));

        ArgumentCaptor<List<AiClientReviewItemRecord>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiClientReviewRepository).createReviewItems(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).jobId()).isEqualTo(conceptJob.id());
    }

    @Test
    @DisplayName("Create client review rejects cross-project concept inclusion")
    void rejectsCrossProjectConcept() {
        UUID otherProjectId = UuidV7.randomUuid();
        AiJobRecord foreignJob = new AiJobRecord(
                UuidV7.randomUuid(),
                studioId,
                otherProjectId,
                inputMediaId,
                outputMediaId,
                "stability",
                "prov-123",
                "Foreign concept",
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
                null,
                null,
                false,
                false,
                "Option X"
        );

        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        when(aiJobRepository.findById(studioId, foreignJob.id())).thenReturn(Optional.of(foreignJob));

        CreateClientReviewRequest req = new CreateClientReviewRequest(
                projectId,
                "Review",
                null,
                7,
                false,
                List.of(foreignJob.id())
        );

        assertThatThrownBy(() -> aiVisualizerService.createClientReview(actor, studioId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cross-project concept inclusion denied");
    }

    @Test
    @DisplayName("Exchange review token creates HttpOnly session and CSRF token")
    void exchangeReviewTokenSuccess() {
        String rawToken = "client-access-secret-token";
        byte[] tokenHash = AiVisualizerService.hashSha256(rawToken);
        AiClientReviewRecord activeReview = new AiClientReviewRecord(
                reviewRecord.id(),
                reviewRecord.studioId(),
                reviewRecord.projectId(),
                reviewRecord.title(),
                reviewRecord.customMessage(),
                tokenHash,
                ClientReviewStatus.OPEN,
                reviewRecord.includeOriginal(),
                reviewRecord.expiresAt(),
                null,
                userId,
                Instant.now(),
                Instant.now(),
                0
        );

        when(aiClientReviewRepository.findReviewByTokenHash(tokenHash)).thenReturn(Optional.of(activeReview));

        ExchangeReviewSessionResult result = aiVisualizerService.exchangeReviewToken(rawToken, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.reviewPublicId()).isEqualTo(activeReview.id());
        assertThat(result.sessionToken()).isNotBlank();
        assertThat(result.csrfToken()).isNotBlank();

        verify(aiClientReviewRepository).createReviewSession(any(AiClientReviewSessionRecord.class));
    }

    @Test
    @DisplayName("Exchange review token rejects expired review")
    void exchangeReviewTokenRejectsExpired() {
        String rawToken = "expired-token";
        byte[] tokenHash = AiVisualizerService.hashSha256(rawToken);
        AiClientReviewRecord expiredReview = new AiClientReviewRecord(
                reviewRecord.id(),
                reviewRecord.studioId(),
                reviewRecord.projectId(),
                reviewRecord.title(),
                reviewRecord.customMessage(),
                tokenHash,
                ClientReviewStatus.OPEN,
                reviewRecord.includeOriginal(),
                Instant.now().minusSeconds(3600),
                null,
                userId,
                Instant.now(),
                Instant.now(),
                0
        );

        when(aiClientReviewRepository.findReviewByTokenHash(tokenHash)).thenReturn(Optional.of(expiredReview));

        assertThatThrownBy(() -> aiVisualizerService.exchangeReviewToken(rawToken, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review link has expired");
    }

    @Test
    @DisplayName("Submit client decision records decision and supersedes previous approvals")
    void submitClientDecisionSuccess() {
        String sessionToken = "valid-session";
        String csrfToken = "valid-csrf";
        byte[] sessionHash = AiVisualizerService.hashSha256(sessionToken);
        byte[] csrfHash = AiVisualizerService.hashSha256(csrfToken);

        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewRecord.id(),
                sessionHash,
                csrfHash,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now()
        );

        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));
        when(aiClientReviewRepository.findReviewByIdGlobal(reviewRecord.id())).thenReturn(Optional.of(reviewRecord));
        when(aiClientReviewRepository.findItemByReviewAndJob(reviewRecord.id(), conceptJob.id()))
                .thenReturn(Optional.of(new AiClientReviewItemRecord(UuidV7.randomUuid(), reviewRecord.id(), studioId, conceptJob.id(), outputMediaId, "Option 1", 0, Instant.now())));

        SubmitClientDecisionRequest req = new SubmitClientDecisionRequest(
                conceptJob.id(),
                ClientReviewDecisionType.APPROVED,
                "Mr. Sharma",
                "We love the wood paneling! Proceed with this."
        );

        aiVisualizerService.submitClientDecision(sessionToken, csrfToken, req, "127.0.0.1");

        verify(aiClientReviewRepository).clearCurrentDecisionsForReview(reviewRecord.id());

        ArgumentCaptor<AiClientReviewDecisionRecord> decCaptor = ArgumentCaptor.forClass(AiClientReviewDecisionRecord.class);
        verify(aiClientReviewRepository).createDecision(decCaptor.capture());
        AiClientReviewDecisionRecord savedDec = decCaptor.getValue();
        assertThat(savedDec.decision()).isEqualTo(ClientReviewDecisionType.APPROVED);
        assertThat(savedDec.clientName()).isEqualTo("Mr. Sharma");
        assertThat(savedDec.isCurrent()).isTrue();

        verify(aiClientReviewRepository).updateReviewCurrentApprovedJob(reviewRecord.id(), conceptJob.id());
        verify(auditService).record(isNull(), eq(studioId), eq("CLIENT_DECISION_RECORDED"), eq("CLIENT_REVIEW"), eq(reviewRecord.id().toString()), anyMap(), isNull(), isNull());
    }

    @Test
    @DisplayName("Submit client decision rejects invalid CSRF token")
    void submitClientDecisionRejectsInvalidCsrf() {
        String sessionToken = "valid-session";
        String legitimateCsrf = "valid-csrf";
        String attackerCsrf = "evil-csrf";
        byte[] sessionHash = AiVisualizerService.hashSha256(sessionToken);
        byte[] csrfHash = AiVisualizerService.hashSha256(legitimateCsrf);

        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewRecord.id(),
                sessionHash,
                csrfHash,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now()
        );

        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));

        SubmitClientDecisionRequest req = new SubmitClientDecisionRequest(
                conceptJob.id(),
                ClientReviewDecisionType.APPROVED,
                "Client",
                "Ok"
        );

        assertThatThrownBy(() -> aiVisualizerService.submitClientDecision(sessionToken, attackerCsrf, req, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid CSRF token");
    }

    @Test
    @DisplayName("Review media preview serves review items but forbids unassociated media")
    void reviewMediaPreviewScoping() {
        String sessionToken = "media-session";
        byte[] sessionHash = AiVisualizerService.hashSha256(sessionToken);

        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewRecord.id(),
                sessionHash,
                new byte[32],
                Instant.now().plusSeconds(3600),
                null,
                Instant.now()
        );

        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));
        when(aiClientReviewRepository.findReviewByIdGlobal(reviewRecord.id())).thenReturn(Optional.of(reviewRecord));

        when(aiClientReviewRepository.isMediaInReview(reviewRecord.id(), outputMediaId)).thenReturn(true);
        MediaAssetRecord outputAsset = new MediaAssetRecord(
                outputMediaId, studioId, projectId, MediaType.AI_CONCEPT, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "master-key", "image/jpeg", 1024, 800, 600,
                0, false, null, null, false, userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAsset(outputMediaId, studioId)).thenReturn(Optional.of(outputAsset));
        when(mediaRepository.findDerivativesByMediaId(outputMediaId, studioId)).thenReturn(List.of(
                new MediaDerivativeRecord(UuidV7.randomUuid(), outputMediaId, studioId, DerivativeVariant.MEDIUM, 800, 600, "image/jpeg", 500L, "derivative-medium-key", null, false, Instant.now())
        ));
        when(storageService.load("derivative-medium-key")).thenReturn(new byte[]{1, 2, 3});

        byte[] preview = aiVisualizerService.getReviewMediaPreview(sessionToken, outputMediaId);
        assertThat(preview).isEqualTo(new byte[]{1, 2, 3});

        UUID rogueMediaId = UuidV7.randomUuid();
        when(aiClientReviewRepository.isMediaInReview(reviewRecord.id(), rogueMediaId)).thenReturn(false);

        assertThatThrownBy(() -> aiVisualizerService.getReviewMediaPreview(sessionToken, rogueMediaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Media asset not found in this review");
    }

    @Test
    @DisplayName("Revoke review invalidates all active sessions")
    void revokeReviewInvalidatesSessions() {
        when(aiClientReviewRepository.findReviewById(studioId, reviewRecord.id())).thenReturn(Optional.of(reviewRecord));

        aiVisualizerService.revokeReview(actor, studioId, reviewRecord.id());

        verify(aiClientReviewRepository).updateReviewStatus(studioId, reviewRecord.id(), ClientReviewStatus.REVOKED);
        verify(aiClientReviewRepository).revokeAllSessionsForReview(reviewRecord.id());
        verify(auditService).record(eq(userId), eq(studioId), eq("CLIENT_REVIEW_REVOKED"), eq("CLIENT_REVIEW"), eq(reviewRecord.id().toString()), isNull(), isNull(), isNull());
    }
}
