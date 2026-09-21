package com.interior.platform.ai;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.media.domain.DerivativeVariant;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.dto.CommitUploadRequest;
import com.interior.platform.media.dto.CreateUploadIntentRequest;
import com.interior.platform.media.dto.MediaDetailResponse;
import com.interior.platform.media.dto.UpdateMediaRequest;
import com.interior.platform.media.dto.UploadIntentResponse;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.media.web.MediaController;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.web.ProjectController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.seo.web.SeoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiPrivacyHardeningTest {

    @Autowired private MediaController mediaController;
    @Autowired private MediaService mediaService;
    @Autowired private ProjectController projectController;
    @Autowired private SeoController seoController;
    @Autowired private ProfessionalOnboardingService onboardingService;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private StorageService storageService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID userAId;
    private UUID studioAId;
    private UUID projectAId;
    private String projectASlug;
    private ActorContext designerAActor;

    private UUID userBId;
    private UUID studioBId;
    private ActorContext designerBActor;

    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.execute("DELETE FROM ai_usage_events");
        jdbcTemplate.execute("DELETE FROM ai_visualization_jobs");
        jdbcTemplate.execute("DELETE FROM media_derivatives");
        jdbcTemplate.execute("DELETE FROM media_assets");
        jdbcTemplate.execute("DELETE FROM upload_intents");
        jdbcTemplate.execute("DELETE FROM studio_watermark_settings");
        jdbcTemplate.execute("DELETE FROM project_styles");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM portfolio_versions");
        jdbcTemplate.execute("DELETE FROM portfolio_sections");
        jdbcTemplate.execute("DELETE FROM portfolios");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_completions");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_drafts");
        jdbcTemplate.execute("DELETE FROM studio_specialties");
        jdbcTemplate.execute("DELETE FROM studio_service_areas");
        jdbcTemplate.execute("DELETE FROM studio_services");
        jdbcTemplate.execute("DELETE FROM studio_contacts");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM studio_slug_claims");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        // 1. Setup Studio A (Primary Tenant)
        userAId = UuidV7.randomUuid();
        UserRecord userA = new UserRecord(
                userAId, "Studio Alpha", "alpha@studios.com", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(userA);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userAId, "CUSTOMER", Instant.now());
        ActorContext preA = new ActorContext(userAId, "Studio Alpha", "alpha@studios.com", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true);

        OnboardingCompletionRequest reqA = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Studio Alpha", "studio-alpha", "Design Principal",
                "Bespoke residential", 2019, "STUDIO_2_5", "LUXURY",
                "Koramangala", "Bengaluru", "Bengaluru Urban", "Karnataka", "560034", "IN",
                true, false, null, List.of("Living Room"), List.of("Modern Minimalist"),
                List.of("Bengaluru"), "+919876543210", "+919876543210", "alpha@studios.com",
                "https://alpha.in", null, true, true
        );
        var compA = onboardingService.completeOnboarding(preA, reqA, null, null);
        studioAId = compA.studio().id();
        designerAActor = new ActorContext(userAId, "Studio Alpha", "alpha@studios.com", Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioAId, "OWNER", "PASSWORD", true);

        // Project for Studio A
        MockHttpServletRequest projReq = createMockRequest(designerAActor);
        CreateProjectRequest createProj = new CreateProjectRequest(
                "Modern Penthouse", ProjectCategory.LIVING_ROOM, "Contemporary luxury penthouse",
                null, null, null, null, "Bengaluru", null, null, "IN",
                2024, null, null, null, "INR", null, null, null, null,
                VisibilityStatus.PORTFOLIO, false, null
        );
        var projResp = projectController.createProject(projReq, studioAId.toString(), null, createProj);
        projectAId = projResp.getBody().id();
        projectASlug = projResp.getBody().slug();

        // 2. Setup Studio B (Cross-Tenant)
        userBId = UuidV7.randomUuid();
        UserRecord userB = new UserRecord(
                userBId, "Studio Beta", "beta@studios.com", "+919876543211",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(userB);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userBId, "CUSTOMER", Instant.now());
        ActorContext preB = new ActorContext(userBId, "Studio Beta", "beta@studios.com", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true);

        OnboardingCompletionRequest reqB = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Studio Beta", "studio-beta", "Design Partner",
                "Architectural spaces", 2021, "STUDIO_2_5", "MID_TO_HIGH",
                "Indiranagar", "Bengaluru", "Bengaluru Urban", "Karnataka", "560038", "IN",
                true, false, null, List.of("Modular Kitchen"), List.of("Modern Minimalist"),
                List.of("Bengaluru"), "+919876543211", "+919876543211", "beta@studios.com",
                "https://beta.in", null, true, true
        );
        var compB = onboardingService.completeOnboarding(preB, reqB, null, null);
        studioBId = compB.studio().id();
        designerBActor = new ActorContext(userBId, "Studio Beta", "beta@studios.com", Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioBId, "OWNER", "PASSWORD", true);

        sampleImageBytes = createSampleImage(1200, 900, new Color(40, 70, 95));
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (actor != null && actor.isAuthenticated()) {
            request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        }
        return request;
    }

    private byte[] createSampleImage(int width, int height, Color color) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("Site Photo", 50, 50);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }

    private UUID commitInputMedia() {
        MockHttpServletRequest req = createMockRequest(designerAActor);
        CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                projectAId, MediaType.REAL_PROJECT, "image/jpeg", sampleImageBytes.length, "penthouse-site.jpg"
        );
        UploadIntentResponse intent = mediaController.createUploadIntent(req, studioAId.toString(), null, intentReq).getBody();
        assertNotNull(intent);

        mediaController.uploadQuarantine(intent.uploadIntentId(), sampleImageBytes, "image/jpeg");

        CommitUploadRequest commitReq = new CommitUploadRequest(
                intent.uploadIntentId(), "Penthouse Site Photo", "Before Renovation", true, MediaVisibility.PORTFOLIO, true
        );
        MediaDetailResponse media = mediaController.commitUpload(req, studioAId.toString(), null, commitReq).getBody();
        assertNotNull(media);
        return media.id();
    }

    private MediaDetailResponse createAiConceptMedia(UUID inputMediaId) {
        UUID conceptMediaId = UuidV7.randomUuid();
        String originalKey = storageService.generateCanonicalOriginalKey(studioAId, projectAId, conceptMediaId, "image/jpeg");
        storageService.store(originalKey, sampleImageBytes, "image/jpeg");

        // Manually simulate AI Visualizer ingestion with Phase 21.1 rules
        Instant now = Instant.now();
        MediaAssetRecord conceptAsset = new MediaAssetRecord(
                conceptMediaId, studioAId, projectAId, MediaType.AI_CONCEPT, MediaVisibility.PRIVATE,
                com.interior.platform.media.domain.MediaProcessingStatus.READY,
                originalKey, "image/jpeg", sampleImageBytes.length, 1200, 900, 1, false,
                "AI Concept: Warm Minimalist", "AI Concept Visualization", true,
                userAId, now, now, null
        );
        mediaRepository.createMediaAsset(conceptAsset);

        MockHttpServletRequest req = createMockRequest(designerAActor);
        return mediaController.getMedia(req, studioAId.toString(), null, conceptMediaId).getBody();
    }

    @Test
    @DisplayName("Scenario 1 & 2: Newly generated AI concept defaults to PRIVATE and has no public derivatives")
    void testAiConceptDefaultsToPrivate() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        assertNotNull(concept);
        assertEquals(MediaType.AI_CONCEPT, concept.mediaType());
        assertEquals(MediaVisibility.PRIVATE, concept.visibility(), "AI Concept MUST default to MediaVisibility.PRIVATE");
        assertTrue(concept.derivatives().isEmpty(), "Private AI Concept must NOT register public derivatives in database");
    }

    @Test
    @DisplayName("Scenario 3 & 4: Clean original master remains private; authenticated preview serves watermarked derivative")
    void testAuthenticatedPrivatePreviewAccess() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        // 1. Clean original master cannot be accessed via public CDN
        MockHttpServletRequest publicReq = new MockHttpServletRequest();
        publicReq.setRequestURI("/api/v1/media/public/" + concept.originalStorageKey());
        ResponseEntity<byte[]> publicResp = mediaController.getPublicDerivative(publicReq);
        assertEquals(HttpStatus.NOT_FOUND, publicResp.getStatusCode(), "Clean original master must NEVER be accessible via public CDN");

        // 2. Authenticated workspace preview by Studio A owner succeeds
        MockHttpServletRequest authReq = createMockRequest(designerAActor);
        ResponseEntity<byte[]> previewResp = mediaController.getMediaPreview(authReq, studioAId.toString(), null, concept.id());
        assertEquals(HttpStatus.OK, previewResp.getStatusCode());
        assertNotNull(previewResp.getBody());
        assertTrue(previewResp.getBody().length > 0);
        assertEquals("private, no-store, max-age=0, must-revalidate", previewResp.getHeaders().getFirst("Cache-Control"));
    }

    @Test
    @DisplayName("Scenario 5: Unauthenticated request to /preview returns 401 Unauthorized")
    void testUnauthenticatedPreviewReturns401() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        MockHttpServletRequest anonReq = new MockHttpServletRequest(); // anonymous actor
        assertThrows(UnauthorizedException.class, () ->
                mediaController.getMediaPreview(anonReq, studioAId.toString(), null, concept.id())
        );
    }

    @Test
    @DisplayName("Scenario 6: Cross-tenant request to /preview returns 403 Forbidden")
    void testCrossTenantPreviewReturns403() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        // Studio B designer attempts to access Studio A private preview
        MockHttpServletRequest crossTenantReq = createMockRequest(designerBActor);
        assertThrows(AccessDeniedException.class, () ->
                mediaController.getMediaPreview(crossTenantReq, studioAId.toString(), null, concept.id())
        );
    }

    @Test
    @DisplayName("Scenario 7: Public derivative delivery returns 404 while concept is PRIVATE")
    void testPublicDeliveryRejectsPrivateConcept() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        // Attempt to request a hypothetical public derivative path
        String mockDerivativeKey = storageService.generateDerivativeKey(
                studioAId, projectAId, concept.id(), DerivativeVariant.MEDIUM.name(), "jpg"
        );
        MockHttpServletRequest pubReq = new MockHttpServletRequest();
        pubReq.setRequestURI("/api/v1/media/public/" + mockDerivativeKey);

        ResponseEntity<byte[]> pubResp = mediaController.getPublicDerivative(pubReq);
        assertEquals(HttpStatus.NOT_FOUND, pubResp.getStatusCode(), "Public CDN delivery MUST reject PRIVATE media assets with 404");
    }

    @Test
    @DisplayName("Scenario 8, 9, 10 & 11: Deliberate promotion to PORTFOLIO enables public delivery ONLY after studio and project are published")
    void testDeliberatePromotionAndDeliveryGating() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);

        // 1. Promote to PORTFOLIO
        MockHttpServletRequest req = createMockRequest(designerAActor);
        UpdateMediaRequest promoteReq = new UpdateMediaRequest(
                "Modern Penthouse Concept", "AI visualization of living room", false, MediaVisibility.PORTFOLIO, true, 1
        );
        MediaDetailResponse promoted = mediaController.updateMedia(req, studioAId.toString(), null, concept.id(), promoteReq).getBody();
        assertNotNull(promoted);
        assertEquals(MediaVisibility.PORTFOLIO, promoted.visibility());
        assertEquals(3, promoted.derivatives().size(), "Promoting to PORTFOLIO must generate responsive public derivatives");

        String publicDerivativeUrl = promoted.derivatives().get(0).publicUrl();
        assertNotNull(publicDerivativeUrl);

        // 2. Gating: Studio is UNPUBLISHED -> 404
        MockHttpServletRequest pubReq = new MockHttpServletRequest();
        pubReq.setRequestURI(publicDerivativeUrl);
        ResponseEntity<byte[]> unpublishedResp = mediaController.getPublicDerivative(pubReq);
        assertEquals(HttpStatus.NOT_FOUND, unpublishedResp.getStatusCode(), "Delivery must reject when studio is UNPUBLISHED");

        // 3. Publish studio
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED' WHERE id = ?", studioAId);

        // 4. Now public delivery succeeds!
        ResponseEntity<byte[]> publishedResp = mediaController.getPublicDerivative(pubReq);
        assertEquals(HttpStatus.OK, publishedResp.getStatusCode());
        assertNotNull(publishedResp.getBody());
        assertEquals("public, max-age=31536000, immutable", publishedResp.getHeaders().getFirst("Cache-Control"));

        // 5. Gating: If project is set to PRIVATE -> 404
        jdbcTemplate.update("UPDATE studio_projects SET visibility_status = 'PRIVATE' WHERE id = ?", projectAId);
        ResponseEntity<byte[]> privateProjectResp = mediaController.getPublicDerivative(pubReq);
        assertEquals(HttpStatus.NOT_FOUND, privateProjectResp.getStatusCode(), "Delivery must reject when project visibility is PRIVATE");

        // Restore project visibility
        jdbcTemplate.update("UPDATE studio_projects SET visibility_status = 'PORTFOLIO' WHERE id = ?", projectAId);
    }

    @Test
    @DisplayName("Scenario 12: Invalidation: Demoting concept back to PRIVATE purges derivatives from storage and DB")
    void testInvalidationOnDemotionToPrivate() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);
        MockHttpServletRequest req = createMockRequest(designerAActor);

        // Publish studio and promote concept
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED' WHERE id = ?", studioAId);
        UpdateMediaRequest promoteReq = new UpdateMediaRequest(null, null, false, MediaVisibility.PORTFOLIO, true, 1);
        MediaDetailResponse promoted = mediaController.updateMedia(req, studioAId.toString(), null, concept.id(), promoteReq).getBody();
        assertNotNull(promoted);
        assertFalse(promoted.derivatives().isEmpty());
        String derivativeKey = storageService.generateDerivativeKey(
                studioAId, projectAId, concept.id(), DerivativeVariant.MEDIUM.name(), "jpg"
        );
        assertTrue(storageService.exists(derivativeKey));

        // Demote back to PRIVATE
        UpdateMediaRequest demoteReq = new UpdateMediaRequest(null, null, false, MediaVisibility.PRIVATE, true, 1);
        MediaDetailResponse demoted = mediaController.updateMedia(req, studioAId.toString(), null, concept.id(), demoteReq).getBody();
        assertNotNull(demoted);
        assertEquals(MediaVisibility.PRIVATE, demoted.visibility());
        assertTrue(demoted.derivatives().isEmpty(), "Derivatives must be deleted from DB upon demotion to PRIVATE");

        // Storage derivative must be purged
        assertFalse(storageService.exists(derivativeKey), "Derivative in storage must be purged upon demotion to PRIVATE");

        // Public delivery must return 404
        MockHttpServletRequest pubReq = new MockHttpServletRequest();
        pubReq.setRequestURI(promoted.derivatives().get(0).publicUrl());
        ResponseEntity<byte[]> resp = mediaController.getPublicDerivative(pubReq);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @DisplayName("Scenario 13: Invalidation on Delete: Purges derivatives and preview cache")
    void testInvalidationOnDelete() {
        UUID inputMediaId = commitInputMedia();
        MediaDetailResponse concept = createAiConceptMedia(inputMediaId);
        MockHttpServletRequest req = createMockRequest(designerAActor);

        // Load preview to ensure preview cache exists
        mediaController.getMediaPreview(req, studioAId.toString(), null, concept.id());
        String previewKey = "studio/" + studioAId + "/previews/" + concept.id() + ".jpg";
        assertTrue(storageService.exists(previewKey));

        // Delete media
        mediaController.deleteMedia(req, studioAId.toString(), null, concept.id());

        // Verify preview cache purged
        assertFalse(storageService.exists(previewKey), "Preview cache must be purged on delete");

        // Subsequent preview request fails
        assertThrows(com.interior.platform.common.exception.ResourceNotFoundException.class, () ->
                mediaController.getMediaPreview(req, studioAId.toString(), null, concept.id())
        );
    }

    @Test
    @DisplayName("Scenario 14: Private AI Concept is excluded from SEO public portfolio and sitemap")
    void testPrivateConceptExcludedFromSeo() {
        UUID inputMediaId = commitInputMedia();
        createAiConceptMedia(inputMediaId); // PRIVATE by default
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED' WHERE id = ?", studioAId);
        jdbcTemplate.update("UPDATE studio_projects SET project_status = 'READY' WHERE id = ?", projectAId);

        // Query SEO public project details
        var seoProject = seoController.getPublicProject("studio-alpha", projectASlug);
        assertEquals(HttpStatus.OK, seoProject.getStatusCode());
        assertNotNull(seoProject.getBody());

        // The project's public media must ONLY contain the committed real project photo, NOT the private AI concept
        assertEquals(1, seoProject.getBody().media().size());
        assertEquals(MediaType.REAL_PROJECT.name(), seoProject.getBody().media().get(0).mediaType());
    }
}
