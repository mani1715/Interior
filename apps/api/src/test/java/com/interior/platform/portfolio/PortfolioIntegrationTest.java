package com.interior.platform.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.dto.CreateVersionSnapshotRequest;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioDetailResponse;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.dto.PortfolioSectionDto;
import com.interior.platform.portfolio.dto.ReorderSectionsRequest;
import com.interior.platform.portfolio.dto.RestoreVersionRequest;
import com.interior.platform.portfolio.dto.SwitchTemplateRequest;
import com.interior.platform.portfolio.dto.UpdatePortfolioRequest;
import com.interior.platform.portfolio.dto.UpdateSectionRequest;
import com.interior.platform.portfolio.web.PortfolioController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.workspace.dto.WorkspaceSummaryResponse;
import com.interior.platform.workspace.web.WorkspaceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PortfolioIntegrationTest {

    @Autowired
    private PortfolioController portfolioController;

    @Autowired
    private WorkspaceController workspaceController;

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UUID studioId;
    private ActorContext designerActor;

    @BeforeEach
    void setUp() {
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
                userId, "Aarav Studio", "aarav@studio.in", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId, "Aarav Studio", "aarav@studio.in",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        OnboardingCompletionRequest req = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Aarav Design Atelier",
                "aarav-atelier",
                "Lead Architect",
                "Bespoke residential architectural interiors",
                2016,
                "STUDIO_2_5",
                "PREMIUM",
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
                List.of("Warm Contemporary", "Indian Traditional"),
                List.of("Bengaluru", "Indiranagar"),
                "+919876543210",
                "+919876543210",
                "hello@aarav.in",
                "https://aarav.in",
                "https://instagram.com/aarav",
                true,
                true
        );

        var completion = onboardingService.completeOnboarding(preActor, req, null, null);
        studioId = completion.studio().id();

        designerActor = new ActorContext(
                userId, "Aarav Studio", "aarav@studio.in",
                Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioId, "OWNER", "PASSWORD", true
        );
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }

    @Test
    @DisplayName("End-to-end Portfolio Engine Lifecycle: Init -> Update -> Sections -> Reorder -> Switch -> Versions -> Preview -> Workspace Readiness")
    void testPortfolioEngineEndToEnd() {
        MockHttpServletRequest req = createMockRequest(designerActor);

        // 1. Initialize Portfolio (POST /api/v1/portfolio)
        ResponseEntity<PortfolioDetailResponse> initResp = portfolioController.initializePortfolio(
                req, null, studioId, new InitializePortfolioRequest(PortfolioTemplateKey.BASIC)
        );

        assertEquals(200, initResp.getStatusCode().value());
        PortfolioDetailResponse portfolio = initResp.getBody();
        assertNotNull(portfolio);
        assertEquals(PortfolioTemplateKey.BASIC, portfolio.templateKey());
        assertEquals(PortfolioStatus.DRAFT, portfolio.status());
        assertEquals(1L, portfolio.version());
        assertEquals(4, portfolio.sections().size());
        assertEquals("private, no-store, max-age=0, must-revalidate", initResp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));

        // 2. Publication is explicitly rejected (PUT with PUBLISHED status)
        UpdatePortfolioRequest pubAttempt = new UpdatePortfolioRequest(
                "Headline", "Subheadline", "Bio", "Philosophy", 8,
                "#1A252F", "#E5D9C5", "#C5A059", FontPairing.CLASSIC_SERIF,
                "PUBLISHED", 1L
        );
        assertThrows(BadRequestException.class, () ->
                portfolioController.updatePortfolio(req, null, studioId, pubAttempt)
        );

        // 3. Stale version update is rejected (409 Conflict)
        UpdatePortfolioRequest staleUpdate = new UpdatePortfolioRequest(
                "Headline", "Subheadline", "Bio", "Philosophy", 8,
                "#1A252F", "#E5D9C5", "#C5A059", FontPairing.CLASSIC_SERIF,
                "DRAFT", 999L
        );
        assertThrows(ConflictException.class, () ->
                portfolioController.updatePortfolio(req, null, studioId, staleUpdate)
        );

        // 4. Valid settings update increments aggregate version to 2
        UpdatePortfolioRequest validUpdate = new UpdatePortfolioRequest(
                "Mastery of Spatial Elegance", "Crafting homes that inspire.", "Over a decade of experience.",
                "Form follows harmony.", 10,
                "#1A252F", "#E5D9C5", "#C5A059", FontPairing.CLASSIC_SERIF,
                "DRAFT", 1L
        );
        ResponseEntity<PortfolioDetailResponse> updateResp = portfolioController.updatePortfolio(
                req, null, studioId, validUpdate
        );
        assertEquals(200, updateResp.getStatusCode().value());
        PortfolioDetailResponse updated = updateResp.getBody();
        assertNotNull(updated);
        assertEquals(2L, updated.version());
        assertEquals("Mastery of Spatial Elegance", updated.headline());
        assertEquals(FontPairing.CLASSIC_SERIF, updated.fontPairing());

        // 5. Update Section (PUT /api/v1/portfolio/sections/{id})
        PortfolioSectionDto heroSection = updated.sections().stream()
                .filter(s -> s.sectionType().name().equals("HERO"))
                .findFirst().orElseThrow();

        ObjectNode heroContent = objectMapper.createObjectNode();
        heroContent.put("badgeText", "Award-Winning Spatial Architecture");
        heroContent.put("ctaText", "Schedule Private Consultation");

        UpdateSectionRequest sectionReq = new UpdateSectionRequest(true, heroContent, 2L);
        ResponseEntity<PortfolioDetailResponse> secResp = portfolioController.updateSection(
                req, null, studioId, heroSection.id(), sectionReq
        );
        assertEquals(200, secResp.getStatusCode().value());
        PortfolioDetailResponse secUpdated = secResp.getBody();
        assertNotNull(secUpdated);
        assertEquals(3L, secUpdated.version());

        // 6. Reorder sections (POST /api/v1/portfolio/sections/reorder)
        List<UUID> reversedSectionIds = secUpdated.sections().stream()
                .map(PortfolioSectionDto::id)
                .collect(java.util.stream.Collectors.toList());
        java.util.Collections.reverse(reversedSectionIds);

        ReorderSectionsRequest reorderReq = new ReorderSectionsRequest(reversedSectionIds, 3L);
        ResponseEntity<PortfolioDetailResponse> reorderResp = portfolioController.reorderSections(
                req, null, studioId, reorderReq
        );
        assertEquals(200, reorderResp.getStatusCode().value());
        PortfolioDetailResponse reordered = reorderResp.getBody();
        assertNotNull(reordered);
        assertEquals(4L, reordered.version());
        assertEquals(reversedSectionIds.get(0), reordered.sections().get(0).id());

        // 7. Switch Template (POST /api/v1/portfolio/switch-template) -> content preserved!
        SwitchTemplateRequest switchReq = new SwitchTemplateRequest(PortfolioTemplateKey.LUXURY, 4L);
        ResponseEntity<PortfolioDetailResponse> switchResp = portfolioController.switchTemplate(
                req, null, studioId, switchReq
        );
        assertEquals(200, switchResp.getStatusCode().value());
        PortfolioDetailResponse switched = switchResp.getBody();
        assertNotNull(switched);
        assertEquals(5L, switched.version());
        assertEquals(PortfolioTemplateKey.LUXURY, switched.templateKey());
        assertEquals(4, switched.sections().size());

        // 8. Create version snapshot (POST /api/v1/portfolio/versions)
        CreateVersionSnapshotRequest snapReq = new CreateVersionSnapshotRequest("Initial Milestone V1", 5L);
        ResponseEntity<PortfolioDetailResponse> snapResp = portfolioController.createVersion(
                req, null, studioId, snapReq
        );
        assertEquals(200, snapResp.getStatusCode().value());
        PortfolioDetailResponse withSnap = snapResp.getBody();
        assertNotNull(withSnap);
        assertTrue(withSnap.recentVersions().size() >= 2);

        // 9. Restore historical version (POST /api/v1/portfolio/versions/1/restore)
        RestoreVersionRequest restoreReq = new RestoreVersionRequest(1, 5L);
        ResponseEntity<PortfolioDetailResponse> restoreResp = portfolioController.restoreVersion(
                req, null, studioId, 1, restoreReq
        );
        assertEquals(200, restoreResp.getStatusCode().value());
        PortfolioDetailResponse restored = restoreResp.getBody();
        assertNotNull(restored);
        assertEquals(PortfolioTemplateKey.BASIC, restored.templateKey());

        // Add an explicitly private internal contact to test privacy filtering
        jdbcTemplate.update("INSERT INTO studio_contacts (studio_id, kind, contact_value, public_consent, sort_order) VALUES (?, ?, ?, ?, ?)",
                studioId, "PHONE", "+919999900000", false, 99);

        // 10. Private preview (GET /api/v1/portfolio/preview) -> Privacy verified!
        ResponseEntity<PortfolioPreviewResponse> prevResp = portfolioController.getPortfolioPreview(req, null, studioId);
        assertEquals(200, prevResp.getStatusCode().value());
        PortfolioPreviewResponse preview = prevResp.getBody();
        assertNotNull(preview);
        assertEquals("Aarav Design Atelier", preview.studioName());
        // Public contact hello@aarav.in present
        assertTrue(preview.publicContacts().stream().anyMatch(c -> c.contactValue().equals("hello@aarav.in")));
        // Private phone +919999900000 omitted!
        assertTrue(preview.publicContacts().stream().noneMatch(c -> c.contactValue().contains("+919999900000")));

        // 11. Workspace Integration: module readiness & 25% launch score contribution
        ResponseEntity<WorkspaceSummaryResponse> wsResp = workspaceController.getWorkspaceSummary(req, null, studioId);
        assertEquals(200, wsResp.getStatusCode().value());
        WorkspaceSummaryResponse wsSummary = wsResp.getBody();
        assertNotNull(wsSummary);

        // PORTFOLIO module readiness should now be READY
        WorkspaceSummaryResponse.ModuleReadinessDto portfolioModule = wsSummary.modules().stream()
                .filter(m -> m.id().equals("portfolio"))
                .findFirst().orElseThrow();
        assertEquals("READY", portfolioModule.status());

        // Launch score includes 25 pts for Portfolio
        assertEquals(25, wsSummary.completeness().breakdown().portfolioScore());
        assertTrue(wsSummary.completeness().platformReadinessPercentage() >= 75);
    }
}
