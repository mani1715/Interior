package com.interior.platform.ai;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.*;
import com.interior.platform.ai.dto.ClientReviewAnnotationDto;
import com.interior.platform.ai.dto.CreateAnnotationRequest;
import com.interior.platform.ai.dto.SetPreferredJobRequest;
import com.interior.platform.ai.dto.ShareRevisionRequest;
import com.interior.platform.ai.provider.AiImageProvider;
import com.interior.platform.ai.repository.AiClientReviewRepository;
import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.repository.AiReferenceRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClientCollaborationTest {

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
    private AuthorizationService authorizationService;
    private AiVisualizerProperties properties;
    private AiVisualizerService aiVisualizerService;

    private final UUID studioId = UuidV7.randomUuid();
    private final UUID userId = UuidV7.randomUuid();
    private final UUID projectId = UuidV7.randomUuid();
    private final UUID jobId1 = UuidV7.randomUuid();
    private final UUID mediaId1 = UuidV7.randomUuid();
    private final UUID reviewId = UuidV7.randomUuid();

    private ActorContext designerActor;
    private AiClientReviewRecord openReview;
    private final String sessionToken = "valid-session-token";
    private final String csrfToken = "valid-csrf-token";
    private byte[] sessionHash;
    private byte[] csrfHash;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        authorizationService = new AuthorizationService();

        properties = new AiVisualizerProperties();
        properties.setProvider("mock");
        properties.setApiKey("test-key");
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

        designerActor = new ActorContext(
                userId,
                "Jane Designer",
                "designer@example.com",
                Set.of("ROLE_PROFESSIONAL", "DESIGNER"),
                studioId,
                "OWNER",
                true
        );

        sessionHash = AiVisualizerService.hashSha256(sessionToken);
        csrfHash = AiVisualizerService.hashSha256(csrfToken);

        openReview = new AiClientReviewRecord(
                reviewId,
                studioId,
                projectId,
                "Living Room Redesign",
                "Client concept board",
                AiVisualizerService.hashSha256("raw-secret"),
                ClientReviewStatus.OPEN,
                true,
                Instant.now().plus(7, ChronoUnit.DAYS),
                null,
                null,
                1,
                userId,
                Instant.now(),
                Instant.now(),
                0
        );

        UserRecord user = new UserRecord(userId, "Jane Designer", "designer@example.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L);
        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        StudioMemberRecord member = new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Jane Studio", "jane-studio", userId, "OWNER", Instant.now());
        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(member));
    }

    private void mockValidSession() {
        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewId,
                sessionHash,
                csrfHash,
                Instant.now().plus(2, ChronoUnit.HOURS),
                null,
                Instant.now()
        );
        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));
        when(aiClientReviewRepository.findReviewByIdGlobal(reviewId)).thenReturn(Optional.of(openReview));
    }

    @Test
    @DisplayName("Client places pin annotation with normalized coordinates [0.0 - 1.0]")
    void submitClientAnnotationSuccess() {
        mockValidSession();

        AiClientReviewItemRecord item = new AiClientReviewItemRecord(
                UuidV7.randomUuid(),
                reviewId,
                studioId,
                jobId1,
                mediaId1,
                "Option 1",
                0,
                Instant.now()
        );
        when(aiClientReviewRepository.findItemByReviewAndJob(reviewId, jobId1)).thenReturn(Optional.of(item));
        when(aiClientReviewRepository.findAnnotationsByReviewId(reviewId)).thenReturn(List.of());

        CreateAnnotationRequest request = new CreateAnnotationRequest(
                jobId1,
                0.45,
                0.65,
                "Client Alice",
                "Make this console table walnut finish",
                true,
                null
        );

        ClientReviewAnnotationDto result = aiVisualizerService.submitClientAnnotation(sessionToken, csrfToken, request, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.coordX()).isEqualTo(0.45);
        assertThat(result.coordY()).isEqualTo(0.65);
        assertThat(result.pinNumber()).isEqualTo(1);
        assertThat(result.authorName()).isEqualTo("Client Alice");
        assertThat(result.commentText()).isEqualTo("Make this console table walnut finish");
        assertThat(result.isChangeRequest()).isTrue();
        assertThat(result.revisionRound()).isEqualTo(1);

        verify(aiClientReviewRepository).createAnnotation(any(AiClientReviewAnnotationRecord.class));
    }

    @Test
    @DisplayName("Client annotation rejects invalid CSRF token")
    void submitClientAnnotationRejectsBadCsrf() {
        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewId,
                sessionHash,
                csrfHash,
                Instant.now().plus(2, ChronoUnit.HOURS),
                null,
                Instant.now()
        );
        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));

        CreateAnnotationRequest request = new CreateAnnotationRequest(
                jobId1,
                0.2,
                0.3,
                "Client",
                "Note",
                false,
                null
        );

        assertThatThrownBy(() -> aiVisualizerService.submitClientAnnotation(sessionToken, "wrong-csrf", request, "127.0.0.1"))
                .isInstanceOf(com.interior.platform.common.exception.AccessDeniedException.class)
                .hasMessageContaining("CSRF");
    }

    @Test
    @DisplayName("Client marks preferred concept among options")
    void setPreferredConceptSuccess() {
        mockValidSession();

        AiClientReviewItemRecord item = new AiClientReviewItemRecord(
                UuidV7.randomUuid(),
                reviewId,
                studioId,
                jobId1,
                mediaId1,
                "Option 1",
                0,
                Instant.now()
        );
        when(aiClientReviewRepository.findItemByReviewAndJob(reviewId, jobId1)).thenReturn(Optional.of(item));

        SetPreferredJobRequest request = new SetPreferredJobRequest(jobId1);
        aiVisualizerService.setPreferredConcept(sessionToken, csrfToken, request, "127.0.0.1");

        verify(aiClientReviewRepository).updateReviewPreferredJob(reviewId, jobId1);
    }

    @Test
    @DisplayName("Studio replies to client annotation pin")
    void addStudioAnnotationReplySuccess() {
        when(aiClientReviewRepository.findReviewById(studioId, reviewId)).thenReturn(Optional.of(openReview));

        UUID parentAnnotationId = UuidV7.randomUuid();
        AiClientReviewAnnotationRecord parent = new AiClientReviewAnnotationRecord(
                parentAnnotationId,
                reviewId,
                studioId,
                jobId1,
                mediaId1,
                1,
                0.5,
                0.5,
                CommentAuthorType.CLIENT,
                "Client Alice",
                "Try gold accents here",
                true,
                null,
                null,
                null,
                1,
                Instant.now(),
                Instant.now(),
                0
        );
        when(aiClientReviewRepository.findAnnotationById(parentAnnotationId)).thenReturn(Optional.of(parent));

        ClientReviewAnnotationDto reply = aiVisualizerService.addStudioAnnotationReply(
                designerActor,
                studioId,
                reviewId,
                parentAnnotationId,
                "Noted! We will test brushed brass in revision 2."
        );

        assertThat(reply).isNotNull();
        assertThat(reply.parentAnnotationId()).isEqualTo(parentAnnotationId);
        assertThat(reply.authorType()).isEqualTo("STUDIO");
        assertThat(reply.commentText()).isEqualTo("Noted! We will test brushed brass in revision 2.");

        verify(aiClientReviewRepository).createAnnotation(any(AiClientReviewAnnotationRecord.class));
    }

    @Test
    @DisplayName("Studio resolves and reopens client annotation pin")
    void resolveAndReopenAnnotation() {
        when(aiClientReviewRepository.findReviewById(studioId, reviewId)).thenReturn(Optional.of(openReview));

        UUID annotationId = UuidV7.randomUuid();
        AiClientReviewAnnotationRecord annotation = new AiClientReviewAnnotationRecord(
                annotationId,
                reviewId,
                studioId,
                jobId1,
                mediaId1,
                1,
                0.3,
                0.4,
                CommentAuthorType.CLIENT,
                "Client",
                "Fix lighting",
                true,
                null,
                null,
                null,
                1,
                Instant.now(),
                Instant.now(),
                0
        );
        when(aiClientReviewRepository.findAnnotationById(annotationId)).thenReturn(Optional.of(annotation));

        aiVisualizerService.resolveAnnotation(designerActor, studioId, reviewId, annotationId);
        verify(aiClientReviewRepository).resolveAnnotation(annotationId, "Jane Designer");

        aiVisualizerService.reopenAnnotation(designerActor, studioId, reviewId, annotationId);
        verify(aiClientReviewRepository).reopenAnnotation(annotationId);
    }

    @Test
    @DisplayName("Studio publishes new revision round with newly generated concept jobs")
    void shareNewRevisionSuccess() {
        when(aiClientReviewRepository.findReviewById(studioId, reviewId)).thenReturn(Optional.of(openReview));

        UUID revJobId = UuidV7.randomUuid();
        UUID revMediaId = UuidV7.randomUuid();
        AiJobRecord revJob = new AiJobRecord(
                revJobId,
                studioId,
                projectId,
                null,
                revMediaId,
                "stability",
                "rev-provider-id",
                "Brushed brass finishes",
                null,
                AiJobStatus.SUCCEEDED,
                null,
                null,
                0,
                null,
                userId,
                Instant.now(),
                Instant.now(),
                Instant.now(),
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
                "Revision Option 2"
        );
        when(aiJobRepository.findById(studioId, revJobId)).thenReturn(Optional.of(revJob));
        when(aiClientReviewRepository.findItemsByReviewId(reviewId)).thenReturn(List.of());

        StudioProjectRecord project = new StudioProjectRecord(
                projectId,
                studioId,
                "penthouse-suite",
                "Penthouse Suite",
                "Desc",
                "Full Desc",
                com.interior.platform.projects.domain.ProjectCategory.LIVING_ROOM,
                com.interior.platform.projects.domain.ProjectStatus.READY,
                com.interior.platform.projects.domain.VisibilityStatus.PORTFOLIO,
                false,
                0,
                "Bengaluru",
                null,
                "Karnataka",
                "IN",
                null,
                null,
                2024,
                BudgetVisibility.HIDDEN,
                null,
                null,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                null,
                null,
                null,
                1L,
                userId,
                Instant.now(),
                Instant.now(),
                null
        );
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(project));

        ShareRevisionRequest shareReq = new ShareRevisionRequest(List.of(revJobId), "Check Revision 2");
        aiVisualizerService.shareNewRevision(designerActor, studioId, reviewId, shareReq);

        verify(aiClientReviewRepository).updateReviewRevisionRound(reviewId, 2);
        verify(aiClientReviewRepository).createReviewItems(anyList());
    }

    @Test
    @DisplayName("Security isolation: Client session from expired review cannot add annotations")
    void clientAnnotationOnExpiredReviewThrows() {
        AiClientReviewRecord expiredReview = new AiClientReviewRecord(
                reviewId,
                studioId,
                projectId,
                "Expired Review",
                null,
                new byte[32],
                ClientReviewStatus.OPEN,
                true,
                Instant.now().minusSeconds(3600),
                null,
                null,
                1,
                userId,
                Instant.now(),
                Instant.now(),
                0
        );

        AiClientReviewSessionRecord session = new AiClientReviewSessionRecord(
                UuidV7.randomUuid(),
                reviewId,
                sessionHash,
                csrfHash,
                Instant.now().plus(2, ChronoUnit.HOURS),
                null,
                Instant.now()
        );
        when(aiClientReviewRepository.findReviewSessionByTokenHash(sessionHash)).thenReturn(Optional.of(session));
        when(aiClientReviewRepository.findReviewByIdGlobal(reviewId)).thenReturn(Optional.of(expiredReview));

        CreateAnnotationRequest request = new CreateAnnotationRequest(
                jobId1,
                0.5,
                0.5,
                "Client",
                "Late comment",
                false,
                null
        );

        assertThatThrownBy(() -> aiVisualizerService.submitClientAnnotation(sessionToken, csrfToken, request, "127.0.0.1"))
                .isInstanceOf(com.interior.platform.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("no longer available");
    }
}
