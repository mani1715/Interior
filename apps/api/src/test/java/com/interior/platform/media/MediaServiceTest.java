package com.interior.platform.media;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
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

import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuditService auditService;

    private AuthorizationService authorizationService;
    private StorageService storageService;
    private ImageProcessingService imageProcessingService;
    private MediaService mediaService;

    private UUID userId;
    private UUID studioId;
    private UUID otherStudioId;
    private UUID projectId;
    private ActorContext ownerActor;
    private ActorContext memberActor;
    private StudioProjectRecord sampleProject;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        storageService = new LocalStorageService("target/test-storage", "/api/v1/media/public");
        imageProcessingService = new ImageProcessingService();

        mediaService = new MediaService(
                mediaRepository,
                projectRepository,
                studioRepository,
                securityRepository,
                authorizationService,
                auditService,
                storageService,
                imageProcessingService
        );

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        otherStudioId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();

        ownerActor = new ActorContext(
                userId, "Designer Owner", "owner@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        memberActor = new ActorContext(
                userId, "Designer Member", "member@studio.com",
                Set.of("DESIGNER_TEAM"), studioId, "MEMBER", true
        );

        sampleProject = new StudioProjectRecord(
                projectId, studioId, "luxury-living-room", "Luxury Living Room",
                "Short description", "Full description",
                ProjectCategory.LIVING_ROOM, ProjectStatus.DRAFT, VisibilityStatus.PORTFOLIO,
                false, 0, "Bengaluru", null, "Karnataka", "IN",
                null, null, 2024,
                com.interior.platform.projects.domain.BudgetVisibility.HIDDEN,
                null, null, "INR",
                com.interior.platform.projects.domain.ClientNameVisibility.HIDDEN,
                null, null, null, null, 1L, userId,
                Instant.now(), Instant.now(), null
        );

        sampleImageBytes = MediaTestHelper.createSampleImageBytes(800, 600, new Color(40, 60, 80));

        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                userId, "Designer Owner", "owner@studio.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L
        )));

        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Aura Studio", "aura-studio", userId, "OWNER", Instant.now())
        ));

        StudioDetailRecord studioDetail = mock(StudioDetailRecord.class);
        lenient().when(studioDetail.name()).thenReturn("Aura Studio");
        lenient().when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioDetail));
    }

    @Test
    @DisplayName("createUploadIntent succeeds and creates pending record")
    void testCreateUploadIntentSuccess() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(sampleProject));

        CreateUploadIntentRequest req = new CreateUploadIntentRequest(
                projectId, MediaType.REAL_PROJECT, "image/jpeg", 102400L, "living.jpg"
        );

        UploadIntentResponse response = mediaService.createUploadIntent(ownerActor, studioId, req);

        assertNotNull(response);
        assertNotNull(response.uploadIntentId());
        assertNotNull(response.mediaAssetId());
        assertTrue(response.uploadUrl().contains(response.uploadIntentId().toString()));
        assertTrue(response.quarantineKey().startsWith("pending/" + studioId));
        verify(mediaRepository).createUploadIntent(any(UploadIntentRecord.class));
    }

    @Test
    @DisplayName("createUploadIntent denies access if project does not belong to studio")
    void testCreateUploadIntentCrossTenantDenied() {
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.empty());

        CreateUploadIntentRequest req = new CreateUploadIntentRequest(
                projectId, MediaType.REAL_PROJECT, "image/jpeg", 102400L, "living.jpg"
        );

        assertThrows(ResourceNotFoundException.class, () ->
                mediaService.createUploadIntent(ownerActor, studioId, req)
        );
    }

    @Test
    @DisplayName("commitUpload successfully promotes quarantine file, strips EXIF, and generates responsive watermarked derivatives")
    void testCommitUploadSuccessWithWatermarks() {
        UUID intentId = UuidV7.randomUuid();
        String quarantineKey = "pending/" + studioId + "/" + intentId + "/test.jpg";
        storageService.store(quarantineKey, sampleImageBytes, "image/jpeg");

        UploadIntentRecord intent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.REAL_PROJECT,
                "image/jpeg", sampleImageBytes.length, quarantineKey,
                UploadIntentStatus.PENDING, Instant.now().plusSeconds(3600), userId, Instant.now()
        );

        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(intent));
        when(mediaRepository.findCoverMedia(projectId, studioId)).thenReturn(Optional.empty());
        when(mediaRepository.getNextSortOrder(projectId, studioId)).thenReturn(0);
        when(mediaRepository.findWatermarkSettings(studioId)).thenReturn(Optional.of(new StudioWatermarkSettingsRecord(
                studioId, true, WatermarkPosition.BOTTOM_RIGHT, new BigDecimal("0.60"), 15, false, "Aura Studio", Instant.now(), Instant.now()
        )));

        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentId, "Living Room Main View", "Spacious living area", true, MediaVisibility.PORTFOLIO, true
        );

        MediaDetailResponse res = mediaService.commitUpload(ownerActor, studioId, commitReq);

        assertNotNull(res);
        assertTrue(res.isCover());
        assertEquals("Living Room Main View", res.altText());
        assertEquals(MediaType.REAL_PROJECT, res.mediaType());
        assertEquals(3, res.derivatives().size(), "Should produce THUMBNAIL, MEDIUM, and LARGE derivatives");
        assertTrue(res.derivatives().stream().allMatch(MediaDerivativeDto::isWatermarked), "Derivatives should be watermarked");

        // Verify original was moved to private canonical storage and deleted from quarantine
        assertFalse(storageService.exists(quarantineKey), "Quarantine file should have been moved");
        assertTrue(storageService.exists(res.originalStorageKey()), "Canonical original must exist");
        assertTrue(res.originalStorageKey().contains("studio/" + studioId + "/projects/" + projectId + "/original/"));

        // Verify public derivatives were saved to storage
        for (MediaDerivativeDto d : res.derivatives()) {
            assertNotNull(d.publicUrl());
            assertTrue(d.publicUrl().contains("/public/"));
        }
    }

    @Test
    @DisplayName("commitUpload enforces mandatory AI Concept badge for AI_CONCEPT media")
    void testCommitUploadAiConceptMandatoryBadge() {
        UUID intentId = UuidV7.randomUuid();
        String quarantineKey = "pending/" + studioId + "/" + intentId + "/ai.jpg";
        storageService.store(quarantineKey, sampleImageBytes, "image/jpeg");

        UploadIntentRecord intent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.AI_CONCEPT,
                "image/jpeg", sampleImageBytes.length, quarantineKey,
                UploadIntentStatus.PENDING, Instant.now().plusSeconds(3600), userId, Instant.now()
        );

        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(intent));
        when(mediaRepository.findCoverMedia(projectId, studioId)).thenReturn(Optional.empty());
        when(mediaRepository.getNextSortOrder(projectId, studioId)).thenReturn(0);
        // Even if studio watermark is disabled, AI concept badge must be applied!
        when(mediaRepository.findWatermarkSettings(studioId)).thenReturn(Optional.of(new StudioWatermarkSettingsRecord(
                studioId, false, WatermarkPosition.BOTTOM_RIGHT, new BigDecimal("0.60"), 15, false, "Aura Studio", Instant.now(), Instant.now()
        )));

        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentId, "AI Conceptual Living Room", null, false, MediaVisibility.PORTFOLIO, false
        );

        MediaDetailResponse res = mediaService.commitUpload(ownerActor, studioId, commitReq);

        assertNotNull(res);
        assertEquals(MediaType.AI_CONCEPT, res.mediaType());
        // Derivatives are marked as watermarked because AI Concept badge was applied!
        assertTrue(res.derivatives().stream().allMatch(MediaDerivativeDto::isWatermarked), "AI Concept badge must be stamped on derivatives");
    }

    @Test
    @DisplayName("commitUpload rejects public visibility for REFERENCE or CLIENT_PRIVATE media")
    void testCommitUploadRejectsPublicVisibilityForPrivateMediaTypes() {
        UUID intentId = UuidV7.randomUuid();
        String quarantineKey = "pending/" + studioId + "/" + intentId + "/ref.jpg";
        storageService.store(quarantineKey, sampleImageBytes, "image/jpeg");

        UploadIntentRecord intent = new UploadIntentRecord(
                intentId, studioId, projectId, MediaType.REFERENCE,
                "image/jpeg", sampleImageBytes.length, quarantineKey,
                UploadIntentStatus.PENDING, Instant.now().plusSeconds(3600), userId, Instant.now()
        );

        when(mediaRepository.findUploadIntent(intentId, studioId)).thenReturn(Optional.of(intent));

        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentId, null, null, false, MediaVisibility.PUBLIC, false
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                mediaService.commitUpload(ownerActor, studioId, commitReq)
        );
        assertTrue(ex.getMessage().contains("strictly private"));
    }

    @Test
    @DisplayName("updateMedia allows updating metadata and cover flag")
    void testUpdateMediaSuccess() {
        UUID mediaId = UuidV7.randomUuid();
        MediaAssetRecord asset = new MediaAssetRecord(
                mediaId, studioId, projectId, MediaType.REAL_PROJECT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "studio/" + studioId + "/original/1.jpg", "image/jpeg",
                102400L, 800, 600, 0, false, "Old Alt", "Old Caption", true,
                userId, Instant.now(), Instant.now(), null
        );

        when(mediaRepository.findMediaAsset(mediaId, studioId)).thenReturn(Optional.of(asset));

        UpdateMediaRequest updateReq = new UpdateMediaRequest(
                "New Alt Text", "Updated Caption", true, MediaVisibility.PORTFOLIO, true, 0
        );

        MediaDetailResponse res = mediaService.updateMedia(ownerActor, studioId, mediaId, updateReq);

        assertNotNull(res);
        verify(mediaRepository).unsetOtherCovers(projectId, studioId, mediaId);
        verify(mediaRepository).updateMediaAsset(any(MediaAssetRecord.class));
    }

    @Test
    @DisplayName("updateMedia denies access to non-manager role")
    void testUpdateMediaDeniedForMemberRole() {
        UUID mediaId = UuidV7.randomUuid();
        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                new StudioMemberRecord(UuidV7.randomUuid(), studioId, "Aura Studio", "aura-studio", userId, "MEMBER", Instant.now())
        ));

        UpdateMediaRequest updateReq = new UpdateMediaRequest(
                "New Alt Text", "Updated Caption", true, MediaVisibility.PORTFOLIO, true, 0
        );

        assertThrows(AccessDeniedException.class, () ->
                mediaService.updateMedia(memberActor, studioId, mediaId, updateReq)
        );
    }

    @Test
    @DisplayName("deleteMedia soft deletes asset and removes public derivatives")
    void testDeleteMediaSuccess() {
        UUID mediaId = UuidV7.randomUuid();
        String derivativeKey = "public/studio/" + studioId + "/projects/" + projectId + "/derivatives/thumb.jpg";
        storageService.store(derivativeKey, sampleImageBytes, "image/jpeg");

        MediaAssetRecord asset = new MediaAssetRecord(
                mediaId, studioId, projectId, MediaType.REAL_PROJECT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "studio/" + studioId + "/original/1.jpg", "image/jpeg",
                102400L, 800, 600, 0, true, "Alt", "Caption", true,
                userId, Instant.now(), Instant.now(), null
        );

        when(mediaRepository.findMediaAsset(mediaId, studioId)).thenReturn(Optional.of(asset));
        when(mediaRepository.findDerivativesByMediaId(mediaId, studioId)).thenReturn(List.of(
                new MediaDerivativeRecord(UuidV7.randomUuid(), mediaId, studioId, DerivativeVariant.THUMBNAIL, 400, 300, "jpg", 5000L, derivativeKey, "/api/v1/media/public/" + derivativeKey, true, Instant.now())
        ));
        when(mediaRepository.findMediaAssetsByProject(projectId, studioId, false)).thenReturn(Collections.emptyList());

        mediaService.deleteMedia(ownerActor, studioId, mediaId);

        verify(mediaRepository).softDeleteMediaAsset(mediaId, studioId);
        verify(mediaRepository).deleteDerivativesByMediaId(mediaId, studioId);
        assertFalse(storageService.exists(derivativeKey), "Public derivative should be deleted from storage");
    }

    @Test
    @DisplayName("getWatermarkSettings returns studio settings or default")
    void testGetWatermarkSettings() {
        when(mediaRepository.findWatermarkSettings(studioId)).thenReturn(Optional.empty());

        WatermarkSettingsResponse res = mediaService.getWatermarkSettings(ownerActor, studioId);

        assertNotNull(res);
        assertTrue(res.enabled());
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, res.position());
        assertEquals("Aura Studio", res.fallbackText());
    }

    @Test
    @DisplayName("updateWatermarkSettings saves new configuration")
    void testUpdateWatermarkSettings() {
        WatermarkSettingsRequest req = new WatermarkSettingsRequest(
                true, WatermarkPosition.TOP_LEFT, new BigDecimal("0.75"), 20, false, "Custom Watermark"
        );

        when(mediaRepository.saveWatermarkSettings(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WatermarkSettingsResponse res = mediaService.updateWatermarkSettings(ownerActor, studioId, req);

        assertNotNull(res);
        assertEquals(WatermarkPosition.TOP_LEFT, res.position());
        assertEquals(new BigDecimal("0.75"), res.opacity());
        assertEquals(20, res.sizePercentage());
        assertEquals("Custom Watermark", res.fallbackText());
    }
}
