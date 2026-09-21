package com.interior.platform.media;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.dto.*;
import com.interior.platform.media.web.MediaController;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.web.PortfolioController;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.web.ProjectController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
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

import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MediaIntegrationTest {

    @Autowired
    private MediaController mediaController;

    @Autowired
    private ProjectController projectController;

    @Autowired
    private PortfolioController portfolioController;

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private UUID portfolioId;
    private ActorContext designerActor;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() {
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

        userId = UuidV7.randomUuid();
        UserRecord user = new UserRecord(
                userId, "Aura Design Studio", "aura@aurastudio.in", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId, "Aura Design Studio", "aura@aurastudio.in",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        OnboardingCompletionRequest req = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Aura Living",
                "aura-living",
                "Design Director",
                "Curated residential interiors",
                2020,
                "STUDIO_2_5",
                "LUXURY",
                "Indiranagar",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560038",
                "IN",
                true,
                false,
                null,
                List.of("Modular Kitchen", "Living Room"),
                List.of("Modern Minimalist", "Warm Contemporary"),
                List.of("Bengaluru", "Indiranagar"),
                "+919876543210",
                "+919876543210",
                "hello@auraliving.in",
                "https://auraliving.in",
                "https://instagram.com/auraliving",
                true,
                true
        );

        var completion = onboardingService.completeOnboarding(preActor, req, null, null);
        studioId = completion.studio().id();

        designerActor = new ActorContext(
                userId, "Aura Living", "aura@aurastudio.in",
                Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioId, "OWNER", "PASSWORD", true
        );

        // Initialize Portfolio
        MockHttpServletRequest request = createMockRequest(designerActor);
        var pInit = portfolioController.initializePortfolio(
                request, null, studioId, new InitializePortfolioRequest(com.interior.platform.portfolio.domain.PortfolioTemplateKey.WARM_NATURAL)
        );
        portfolioId = pInit.getBody().id();

        // Create Project
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Penthouse Serenity",
                ProjectCategory.LIVING_ROOM,
                "Minimalist penthouse with panoramic views",
                "Complete interior overhaul featuring custom teak woodwork.",
                null, null, null,
                "Bengaluru", "East", "Karnataka", "IN",
                2024,
                com.interior.platform.projects.domain.BudgetVisibility.RANGE,
                new BigDecimal("5000000"), new BigDecimal("7500000"), "INR",
                com.interior.platform.projects.domain.ClientNameVisibility.DISPLAY,
                "Dr. Varma",
                new BigDecimal("3200"),
                com.interior.platform.projects.domain.AreaUnit.SQ_FT,
                VisibilityStatus.PORTFOLIO,
                true,
                "Confidential execution notes"
        );
        var projResp = projectController.createProject(request, studioId.toString(), null, projReq);
        projectId = projResp.getBody().id();

        sampleImageBytes = MediaTestHelper.createSampleImageBytes(1200, 900, new Color(45, 65, 85));
    }

    @Test
    @DisplayName("Complete Media Lifecycle: Upload intent -> Quarantine upload -> Commit -> Derivatives -> Portfolio presentation -> Public CDN delivery -> Delete")
    void testCompleteMediaLifecycle() {
        MockHttpServletRequest req = createMockRequest(designerActor);

        // 1. Create upload intent
        CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                projectId, MediaType.REAL_PROJECT, "image/jpeg", sampleImageBytes.length, "penthouse-living.jpg"
        );
        ResponseEntity<UploadIntentResponse> intentResp = mediaController.createUploadIntent(req, studioId.toString(), null, intentReq);
        assertEquals(HttpStatus.CREATED, intentResp.getStatusCode());
        UploadIntentResponse intentData = intentResp.getBody();
        assertNotNull(intentData);
        assertNotNull(intentData.uploadIntentId());

        // 2. Direct upload to quarantine
        ResponseEntity<Map<String, Object>> uploadResp = mediaController.uploadQuarantine(
                intentData.uploadIntentId(), sampleImageBytes, "image/jpeg"
        );
        assertEquals(HttpStatus.OK, uploadResp.getStatusCode());
        assertEquals("uploaded", uploadResp.getBody().get("status"));

        // 3. Commit upload
        CommitUploadRequest commitReq = new CommitUploadRequest(
                intentData.uploadIntentId(), "Penthouse Living Area", "Spacious open concept living", true, MediaVisibility.PORTFOLIO, true
        );
        ResponseEntity<MediaDetailResponse> commitResp = mediaController.commitUpload(req, studioId.toString(), null, commitReq);
        assertEquals(HttpStatus.CREATED, commitResp.getStatusCode());
        MediaDetailResponse mediaDetail = commitResp.getBody();
        assertNotNull(mediaDetail);
        assertTrue(mediaDetail.isCover());
        assertEquals(MediaType.REAL_PROJECT, mediaDetail.mediaType());
        assertEquals(3, mediaDetail.derivatives().size(), "THUMBNAIL, MEDIUM, LARGE must be generated");

        UUID mediaId = mediaDetail.id();

        // 4. List project media via ProjectController
        ResponseEntity<List<MediaDetailResponse>> listResp = projectController.listProjectMedia(req, studioId.toString(), null, projectId);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertEquals(1, listResp.getBody().size());
        assertEquals(mediaId, listResp.getBody().get(0).id());

        // 5. Verify portfolio projects presentation now includes real cover image URL
        ResponseEntity<PortfolioPreviewResponse> previewResp = portfolioController.getPortfolioPreview(req, studioId.toString(), null);
        assertEquals(HttpStatus.OK, previewResp.getStatusCode());
        PortfolioPreviewResponse preview = previewResp.getBody();
        assertNotNull(preview);
        assertNotNull(preview.portfolioProjects());
        assertFalse(preview.portfolioProjects().isEmpty());
        String coverUrl = preview.portfolioProjects().get(0).coverImageUrl();
        assertNotNull(coverUrl, "Cover image URL must be populated by Media Engine in Phase 19");
        assertTrue(coverUrl.contains("/media/public/"));

        // 6. Test Public Derivative Delivery route
        MockHttpServletRequest publicReq = new MockHttpServletRequest();
        publicReq.setRequestURI(coverUrl);
        ResponseEntity<byte[]> publicResp = mediaController.getPublicDerivative(publicReq);
        assertEquals(HttpStatus.OK, publicResp.getStatusCode());
        assertNotNull(publicResp.getBody());
        assertTrue(publicResp.getBody().length > 0);
        assertEquals("public, max-age=31536000, immutable", publicResp.getHeaders().getFirst("Cache-Control"));

        // 7. Watermark settings inspection and update
        ResponseEntity<WatermarkSettingsResponse> wmGet = mediaController.getWatermarkSettings(req, studioId.toString(), null);
        assertEquals(HttpStatus.OK, wmGet.getStatusCode());
        assertTrue(wmGet.getBody().enabled());

        WatermarkSettingsRequest wmUpdateReq = new WatermarkSettingsRequest(
                true, WatermarkPosition.BOTTOM_LEFT, new BigDecimal("0.70"), 18, false, "Aura Living Signature"
        );
        ResponseEntity<WatermarkSettingsResponse> wmUpdate = mediaController.updateWatermarkSettings(req, studioId.toString(), null, wmUpdateReq);
        assertEquals(HttpStatus.OK, wmUpdate.getStatusCode());
        assertEquals(WatermarkPosition.BOTTOM_LEFT, wmUpdate.getBody().position());
        assertEquals("Aura Living Signature", wmUpdate.getBody().fallbackText());

        // 8. Delete media
        ResponseEntity<Void> deleteResp = mediaController.deleteMedia(req, studioId.toString(), null, mediaId);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        // Verify project media list is now empty
        ResponseEntity<List<MediaDetailResponse>> afterDeleteList = projectController.listProjectMedia(req, studioId.toString(), null, projectId);
        assertEquals(0, afterDeleteList.getBody().size());
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }
}
