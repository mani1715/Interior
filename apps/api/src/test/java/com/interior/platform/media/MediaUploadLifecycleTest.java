package com.interior.platform.media;

import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.service.SsrfProtectionService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.dto.*;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.media.storage.LocalStorageService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.projects.repository.ProjectRoomRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaUploadLifecycleTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;
    @Mock private ProjectRoomRepository projectRoomRepository;
    @Mock private EntitlementService entitlementService;

    private AuthorizationService authorizationService;
    private StorageService storageService;
    private ImageProcessingService imageProcessingService;
    private MediaService mediaService;
    private SsrfProtectionService ssrfProtectionService;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private ActorContext ownerActor;
    private StudioProjectRecord sampleProject;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        storageService = new LocalStorageService("target/test-storage-lifecycle", "/api/v1/media/public");
        imageProcessingService = new ImageProcessingService();
        ssrfProtectionService = new SsrfProtectionService();

        mediaService = new MediaService(
                mediaRepository,
                projectRepository,
                studioRepository,
                securityRepository,
                authorizationService,
                auditService,
                storageService,
                imageProcessingService,
                projectRoomRepository
        );
        ReflectionTestUtils.setField(mediaService, "entitlementService", entitlementService);

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();

        ownerActor = new ActorContext(
                userId, "Studio Principal", "principal@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        sampleProject = new StudioProjectRecord(
                projectId, studioId, "penthouse-suite", "Penthouse Suite",
                "Luxury suite", "Detailed penthouse interior design",
                ProjectCategory.LIVING_ROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                false, 0, "Mumbai", null, "Maharashtra", "IN",
                null, null, 2024,
                com.interior.platform.projects.domain.BudgetVisibility.HIDDEN,
                null, null, "INR",
                com.interior.platform.projects.domain.ClientNameVisibility.HIDDEN,
                null, null, null, null, 1L, userId,
                Instant.now(), Instant.now(), null
        );

        sampleImageBytes = MediaTestHelper.createSampleImageBytes(800, 600, new Color(50, 100, 150));

        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                userId, "Studio Principal", "principal@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L
        )));

        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Prestige Studio", "prestige-studio", userId, "OWNER", Instant.now())
        ));

        StudioDetailRecord studioDetail = mock(StudioDetailRecord.class);
        lenient().when(studioDetail.name()).thenReturn("Prestige Studio");
        lenient().when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioDetail));
    }

    @Test
    @DisplayName("Upload lifecycle: Create intent -> direct upload -> commit succeeds with public derivatives")
    void testFullUploadLifecycle() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));

        CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                projectId, MediaType.REAL_PROJECT, "image/jpeg", (long) sampleImageBytes.length, "penthouse.jpg"
        );
        UploadIntentResponse intentRes = mediaService.createUploadIntent(ownerActor, studioId, intentReq);
        assertNotNull(intentRes);
        assertNotNull(intentRes.uploadIntentId());

        // Simulate quarantine upload
        UploadIntentRecord pendingIntent = new UploadIntentRecord(
                intentRes.uploadIntentId(),
                studioId,
                projectId,
                MediaType.REAL_PROJECT,
                "image/jpeg",
                sampleImageBytes.length,
                intentRes.quarantineKey(),
                UploadIntentStatus.PENDING,
                intentRes.expiresAt(),
                userId,
                Instant.now(),
                intentRes.mediaAssetId()
        );
        when(mediaRepository.findUploadIntent(intentRes.uploadIntentId(), studioId)).thenReturn(Optional.of(pendingIntent));
        storageService.store(intentRes.quarantineKey(), sampleImageBytes, "image/jpeg");

        // Commit upload
        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentRes.uploadIntentId(), "Main salon", "Penthouse living view", false, MediaVisibility.PORTFOLIO, true, null, false
        );
        MediaDetailResponse detail = mediaService.commitUpload(ownerActor, studioId, commitReq);

        assertNotNull(detail);
        assertEquals(intentRes.mediaAssetId(), detail.id());
        assertEquals(MediaProcessingStatus.READY, detail.processingStatus());
        assertEquals(MediaVisibility.PORTFOLIO, detail.visibility());
        assertFalse(detail.derivatives().isEmpty());
        verify(mediaRepository).linkUploadIntentMediaAsset(intentRes.uploadIntentId(), intentRes.mediaAssetId());
    }

    @Test
    @DisplayName("Commit idempotency: Retrying commitUpload on an already committed intent returns existing asset safely")
    void testCommitUploadIdempotency() {
        UUID intentId = UuidV7.randomUuid();
        UUID mediaAssetId = UuidV7.randomUuid();

        UploadIntentRecord committedIntent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.REAL_PROJECT, "image/jpeg",
                1024L, "quarantine/test.jpg", UploadIntentStatus.COMMITTED,
                Instant.now().plusSeconds(3600), userId, Instant.now(), mediaAssetId
        );
        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(committedIntent));

        MediaAssetRecord existingAsset = new MediaAssetRecord(
                mediaAssetId, studioId, projectId, MediaType.REAL_PROJECT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "canonical/test.jpg", "image/jpeg", 1024L, 800, 600,
                0, true, "Alt", "Caption", true, userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAssetByUploadIntent(intentId, studioId)).thenReturn(Optional.of(existingAsset));
        when(mediaRepository.findDerivativesByMediaId(mediaAssetId, studioId)).thenReturn(List.of());

        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentId, "Alt", "Caption", true, MediaVisibility.PORTFOLIO, true, null, false
        );
        MediaDetailResponse res = mediaService.commitUpload(ownerActor, studioId, commitReq);

        assertNotNull(res);
        assertEquals(mediaAssetId, res.id());
        // Verify no duplicate creation
        verify(mediaRepository, never()).createMediaAsset(any());
    }

    @Test
    @DisplayName("Cancel upload intent: Pending intent cancelled and quarantine object purged")
    void testCancelUploadIntentPending() {
        UUID intentId = UuidV7.randomUuid();
        String qKey = "pending/" + studioId + "/" + intentId + ".jpg";
        storageService.store(qKey, sampleImageBytes, "image/jpeg");
        assertTrue(storageService.exists(qKey));

        UploadIntentRecord intent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.REAL_PROJECT, "image/jpeg",
                sampleImageBytes.length, qKey, UploadIntentStatus.PENDING,
                Instant.now().plusSeconds(3600), userId, Instant.now(), UuidV7.randomUuid()
        );
        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(intent));

        mediaService.cancelUploadIntent(ownerActor, studioId, intentId);

        verify(mediaRepository).cancelUploadIntent(intentId, studioId);
        assertFalse(storageService.exists(qKey));
    }

    @Test
    @DisplayName("Cancel upload intent: Committed intent cannot be cancelled")
    void testCancelUploadIntentCommittedRejected() {
        UUID intentId = UuidV7.randomUuid();
        UploadIntentRecord committedIntent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.REAL_PROJECT, "image/jpeg",
                1024L, "quarantine/test.jpg", UploadIntentStatus.COMMITTED,
                Instant.now().plusSeconds(3600), userId, Instant.now(), UuidV7.randomUuid()
        );
        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(committedIntent));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                mediaService.cancelUploadIntent(ownerActor, studioId, intentId)
        );
        assertTrue(ex.getMessage().contains("already committed"));
    }

    @Test
    @DisplayName("Photo replacement: Slot-preserving and quota-neutral update of photo content and derivatives")
    void testPhotoReplacementSlotPreserving() {
        UUID mediaId = UuidV7.randomUuid();
        UUID newIntentId = UuidV7.randomUuid();
        UUID roomId = UuidV7.randomUuid();

        MediaAssetRecord existing = new MediaAssetRecord(
                mediaId, studioId, projectId, MediaType.REAL_PROJECT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "canonical/old.jpg", "image/jpeg", 50000L, 800, 600,
                3, true, "Living room", "Grand view", true, userId, Instant.now(), Instant.now(), null,
                roomId, true, new BigDecimal("45.00"), new BigDecimal("65.00"), true, true
        );
        when(mediaRepository.findMediaAsset(mediaId, studioId)).thenReturn(Optional.of(existing));

        byte[] newImageBytes = MediaTestHelper.createSampleImageBytes(1024, 768, Color.DARK_GRAY);
        String qKey = "pending/" + studioId + "/replace_" + newIntentId + ".jpg";
        storageService.store(qKey, newImageBytes, "image/jpeg");

        UploadIntentRecord intent = new UploadIntentRecord(
                newIntentId, studioId, projectId, MediaType.REAL_PROJECT, "image/jpeg",
                newImageBytes.length, qKey, UploadIntentStatus.PENDING,
                Instant.now().plusSeconds(3600), userId, Instant.now(), null
        );
        when(mediaRepository.findUploadIntent(newIntentId, studioId)).thenReturn(Optional.of(intent));

        ReplaceMediaRequest req = new ReplaceMediaRequest(newIntentId);
        MediaDetailResponse res = mediaService.replaceMedia(ownerActor, studioId, mediaId, req);

        assertNotNull(res);
        assertEquals(mediaId, res.id());
        // Slot properties preserved
        assertEquals(roomId, res.roomId());
        assertTrue(res.isRoomCover());
        assertTrue(res.isCover());
        assertEquals("Living room", res.altText());
        assertEquals("Grand view", res.caption());

        // Verify photo quota was NOT deducted (quota neutrality)
        verify(entitlementService, never()).assertProjectPhotoQuotaAllowed(any(), any(), anyInt());
        verify(mediaRepository).replaceMediaAssetContent(eq(mediaId), eq(studioId), anyString(), eq("image/jpeg"), eq((long) newImageBytes.length), eq(1024), eq(768));
    }

    @Test
    @DisplayName("Storage quota assertion: Excess bytes rejected when storage limit exceeded")
    void testStorageQuotaExceeded() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));
        doThrow(new BadRequestException("Storage limit reached for your current plan"))
                .when(entitlementService).assertStorageQuotaAllowed(eq(studioId), anyLong());

        CreateUploadIntentRequest req = new CreateUploadIntentRequest(
                projectId, MediaType.REAL_PROJECT, "image/jpeg", 20000000L, "huge.jpg"
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                mediaService.createUploadIntent(ownerActor, studioId, req)
        );
        assertTrue(ex.getMessage().contains("Storage limit reached"));
    }

    @Test
    @DisplayName("Portfolio enrollment gate: Unenrolled photo checks photo quota and REFERENCE is disallowed")
    void testPortfolioEnrollmentGate() {
        UUID refMediaId = UuidV7.randomUuid();
        MediaAssetRecord refAsset = new MediaAssetRecord(
                refMediaId, studioId, projectId, MediaType.REFERENCE, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "canonical/ref.jpg", "image/jpeg", 50000L, 800, 600,
                0, false, null, null, false, userId, Instant.now(), Instant.now(), null,
                null, false, new BigDecimal("50.00"), new BigDecimal("50.00"), true, false
        );
        when(mediaRepository.findMediaAsset(refMediaId, studioId)).thenReturn(Optional.of(refAsset));

        UpdateMediaRequest enrollRef = new UpdateMediaRequest(
                null, null, false, MediaVisibility.PRIVATE, false, 0, null, false, false, null, null, false, true
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                mediaService.updateMedia(ownerActor, studioId, refMediaId, enrollRef)
        );
        assertTrue(ex.getMessage().contains("cannot be enrolled in portfolio"));
    }

    @Test
    @DisplayName("Portfolio enrollment gate: AI Concept enrolled forces watermark disclosure badge")
    void testPortfolioEnrollmentAiConceptWatermarked() {
        UUID aiMediaId = UuidV7.randomUuid();
        MediaAssetRecord aiAsset = new MediaAssetRecord(
                aiMediaId, studioId, projectId, MediaType.AI_CONCEPT, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "canonical/ai.jpg", "image/jpeg", 50000L, 800, 600,
                0, false, null, null, false, userId, Instant.now(), Instant.now(), null,
                null, false, new BigDecimal("50.00"), new BigDecimal("50.00"), true, false
        );
        when(mediaRepository.findMediaAsset(aiMediaId, studioId)).thenReturn(Optional.of(aiAsset));

        UpdateMediaRequest enrollAi = new UpdateMediaRequest(
                null, null, false, MediaVisibility.PORTFOLIO, false, 0, null, false, false, null, null, false, true
        );

        MediaDetailResponse res = mediaService.updateMedia(ownerActor, studioId, aiMediaId, enrollAi);
        assertNotNull(res);
        assertTrue(res.watermarkEnabled()); // Enforced watermark
        verify(entitlementService).assertProjectPhotoQuotaAllowed(studioId, projectId, 1);
    }

    @Test
    @DisplayName("Decompression bomb protection: Corrupt or invalid signatures rejected")
    void testDecompressionBombProtection() {
        assertThrows(IllegalArgumentException.class, () ->
                imageProcessingService.validateAndGetDimensions(new byte[10])
        );
    }

    @Test
    @DisplayName("SSRF Protection: Blocks loopback, private networks, cloud metadata, and invalid schemes")
    void testSsrfProtectionService() {
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("http://127.0.0.1/secret"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("http://localhost:8080"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("http://192.168.1.10/admin"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("http://10.0.0.5/api"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("http://169.254.169.254/latest/meta-data"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("file:///etc/passwd"));
        assertFalse(ssrfProtectionService.isSafeRemoteUrl("gopher://127.0.0.1/"));
        assertTrue(ssrfProtectionService.isSafeRemoteUrl("https://images.unsplash.com/photo-12345.jpg"));
    }

    @Test
    @DisplayName("Storage reconciliation: Cleans expired intents and returns accurate report")
    void testStorageReconciliationReport() {
        UUID expiredIntentId = UuidV7.randomUuid();
        String qKey = "pending/" + studioId + "/expired_" + expiredIntentId + ".jpg";
        storageService.store(qKey, sampleImageBytes, "image/jpeg");

        UploadIntentRecord expiredIntent = new UploadIntentRecord(
                expiredIntentId, studioId, projectId, MediaType.REAL_PROJECT, "image/jpeg",
                sampleImageBytes.length, qKey, UploadIntentStatus.PENDING,
                Instant.now().minusSeconds(3600), userId, Instant.now().minusSeconds(7200), null
        );
        when(mediaRepository.findExpiredUploadIntents(any(Instant.class))).thenReturn(List.of(expiredIntent));
        when(mediaRepository.countCommittedStorageBytes(studioId)).thenReturn(1000000L);
        when(mediaRepository.countPendingStorageBytes(studioId)).thenReturn(0L);
        when(mediaRepository.countCommittedPortfolioPhotos(studioId, null)).thenReturn(5);
        when(mediaRepository.countPendingPortfolioUploadIntents(studioId, null)).thenReturn(0);
        when(mediaRepository.countActiveMediaByStudio(studioId)).thenReturn(8L);

        StorageReconciliationReport report = mediaService.reconcileStorage(ownerActor, studioId);

        assertNotNull(report);
        assertEquals(studioId, report.studioId());
        assertEquals(5, report.portfolioPhotoCount());
        assertEquals(0, report.pendingPhotoReservations());
        assertEquals(1000000L, report.committedStorageBytes());
        assertEquals(1, report.expiredIntentsCount());
        assertEquals(8, report.activeMediaCount());
        assertFalse(storageService.exists(qKey)); // Cleaned
    }
}
