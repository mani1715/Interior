package com.interior.platform.seo;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.web.PortfolioController;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.web.ProjectController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.seo.dto.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SeoIntegrationTest {

    @Autowired
    private SeoController seoController;

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
    private String studioSlug;
    private ActorContext designerActor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM studio_seo_settings");
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
        jdbcTemplate.execute("DELETE FROM studio_slug_claims WHERE studio_id IS NOT NULL");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        userId = UuidV7.randomUuid();
        studioSlug = "arc-studio-" + UUID.randomUUID().toString().substring(0, 8);

        UserRecord user = new UserRecord(
                userId,
                "Ananya Sharma",
                "ananya." + UUID.randomUUID().toString().substring(0, 8) + "@studio.com",
                "+919876543210",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId,
                "Ananya Sharma",
                user.email(),
                Set.of("CUSTOMER"),
                Set.of(),
                null,
                null,
                "PASSWORD",
                true
        );

        OnboardingCompletionRequest onboardingReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Arc Interior Studio",
                studioSlug,
                "Principal Architect & Interior Designer",
                "Bespoke sustainable architectural interiors",
                2019,
                "STUDIO_2_5",
                "LUXURY",
                "45 Indiranagar 100ft Road",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560038",
                "IN",
                true,
                false,
                null,
                List.of("Living Room"),
                List.of("Modern Minimalist"),
                List.of("Bengaluru", "Indiranagar"),
                "+919876543210",
                "+919876543210",
                "hello@arcstudio.in",
                "https://arcstudio.in",
                "https://instagram.com/arcstudio",
                true,
                true
        );

        var onboardingResp = onboardingService.completeOnboarding(preActor, onboardingReq, null, null);
        studioId = onboardingResp.studio().id();

        designerActor = new ActorContext(
                userId,
                "Ananya Sharma",
                user.email(),
                Set.of("DESIGNER", "CUSTOMER"),
                Set.of("STUDIO_MANAGE"),
                studioId,
                "OWNER",
                "PASSWORD",
                true
        );
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }

    @Test
    @DisplayName("Complete SEO Lifecycle: Status -> Initialize Portfolio -> Publish -> Public Route -> Unpublish -> 404")
    void testCompleteSeoAndPublicationLifecycle() {
        MockHttpServletRequest authReq = createMockRequest(designerActor);

        // 1. Initial SEO status: UNPUBLISHED
        ResponseEntity<SeoStatusResponse> statusResp = seoController.getSeoStatus(authReq, studioId);
        assertEquals(HttpStatus.OK, statusResp.getStatusCode());
        assertNotNull(statusResp.getBody());
        assertEquals("UNPUBLISHED", statusResp.getBody().publicationStatus());
        assertFalse(statusResp.getBody().isPublishable(), "Portfolio is not yet initialized so not publishable");

        // 2. Initialize portfolio and set status to READY
        ResponseEntity<?> initPortfolioResp = portfolioController.initializePortfolio(
                authReq,
                null,
                studioId,
                new InitializePortfolioRequest(PortfolioTemplateKey.MODERN)
        );
        assertEquals(HttpStatus.OK, initPortfolioResp.getStatusCode());

        // Update portfolio to READY status
        jdbcTemplate.update("UPDATE portfolios SET status = 'READY' WHERE studio_id = ?", studioId);

        // 3. SEO status now shows publishable!
        statusResp = seoController.getSeoStatus(authReq, studioId);
        assertTrue(statusResp.getBody().isPublishable());

        // 4. Update SEO Settings (Custom meta title & description)
        ResponseEntity<SeoStatusResponse> settingsResp = seoController.updateSeoSettings(
                authReq,
                studioId,
                new UpdateSeoSettingsRequest("Arc Studio | Top Interior Architects Bengaluru", "Custom description for search appearance", null, true)
        );
        assertEquals(HttpStatus.OK, settingsResp.getStatusCode());
        assertEquals("Arc Studio | Top Interior Architects Bengaluru", settingsResp.getBody().metaTitleOverride());

        // 5. Public route BEFORE publish returns 404
        ResponseEntity<PublicStudioDto> prePublicResp = seoController.getPublicStudio(studioSlug);
        assertEquals(HttpStatus.NOT_FOUND, prePublicResp.getStatusCode());

        // 6. Publish Studio
        ResponseEntity<SeoStatusResponse> pubResp = seoController.publishStudio(authReq, studioId);
        assertEquals(HttpStatus.OK, pubResp.getStatusCode());
        assertEquals("PUBLISHED", pubResp.getBody().publicationStatus());

        // 7. Public route AFTER publish returns 200 with truthful DTO
        ResponseEntity<PublicStudioDto> publicResp = seoController.getPublicStudio(studioSlug);
        assertEquals(HttpStatus.OK, publicResp.getStatusCode());
        assertNotNull(publicResp.getBody());
        assertEquals("Arc Interior Studio", publicResp.getBody().name());
        assertEquals(studioSlug, publicResp.getBody().slug());
        assertEquals("Arc Studio | Top Interior Architects Bengaluru", publicResp.getBody().metaTitle());
        assertNotNull(publicResp.getBody().portfolio());
        assertEquals("MODERN", publicResp.getBody().portfolio().templateKey());

        // 8. Create a ready project story
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Koramangala Luxury Villa",
                ProjectCategory.LIVING_ROOM,
                "Private luxury villa interior project",
                "Detailed narrative",
                null,
                null,
                null,
                "Bengaluru",
                "East",
                "Karnataka",
                "IN",
                2025,
                BudgetVisibility.RANGE,
                new BigDecimal("5000000"),
                new BigDecimal("7500000"),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Dr. Varma",
                new BigDecimal("3200"),
                AreaUnit.SQ_FT,
                VisibilityStatus.PORTFOLIO,
                true,
                "Confidential execution notes"
        );
        var createProjResp = projectController.createProject(authReq, studioId.toString(), null, projReq);
        assertEquals(HttpStatus.CREATED, createProjResp.getStatusCode());
        String projectSlug = createProjResp.getBody().slug();

        // Mark project as READY
        jdbcTemplate.update("UPDATE studio_projects SET project_status = 'READY' WHERE id = ?", createProjResp.getBody().id());

        // 9. Query public project detail
        ResponseEntity<PublicProjectDetailDto> projectDetailResp = seoController.getPublicProject(studioSlug, projectSlug);
        assertEquals(HttpStatus.OK, projectDetailResp.getStatusCode());
        assertNotNull(projectDetailResp.getBody());
        assertEquals("Koramangala Luxury Villa", projectDetailResp.getBody().title());
        assertEquals("Bengaluru", projectDetailResp.getBody().city());

        // 10. Query sitemap entries
        ResponseEntity<List<SitemapItemDto>> sitemapResp = seoController.getSitemapEntries();
        assertEquals(HttpStatus.OK, sitemapResp.getStatusCode());
        assertNotNull(sitemapResp.getBody());
        assertTrue(sitemapResp.getBody().stream().anyMatch(s -> s.path().contains(studioSlug)));
        assertTrue(sitemapResp.getBody().stream().anyMatch(s -> s.path().contains(projectSlug)));

        // 11. Unpublish Studio
        ResponseEntity<SeoStatusResponse> unpubResp = seoController.unpublishStudio(authReq, studioId);
        assertEquals(HttpStatus.OK, unpubResp.getStatusCode());
        assertEquals("UNPUBLISHED", unpubResp.getBody().publicationStatus());

        // 12. Verification: public routes now return 404!
        ResponseEntity<PublicStudioDto> postUnpubStudioResp = seoController.getPublicStudio(studioSlug);
        assertEquals(HttpStatus.NOT_FOUND, postUnpubStudioResp.getStatusCode());

        ResponseEntity<PublicProjectDetailDto> postUnpubProjResp = seoController.getPublicProject(studioSlug, projectSlug);
        assertEquals(HttpStatus.NOT_FOUND, postUnpubProjResp.getStatusCode());
    }
}
