package com.interior.platform.projects;

import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.web.PortfolioController;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectActionRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.dto.ProjectSummaryResponse;
import com.interior.platform.projects.dto.ReorderProjectsRequest;
import com.interior.platform.projects.dto.UpdateProjectRequest;
import com.interior.platform.projects.web.ProjectController;
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
class ProjectIntegrationTest {

    @Autowired
    private ProjectController projectController;

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

    private UUID userId;
    private UUID studioId;
    private ActorContext designerActor;

    @BeforeEach
    void setUp() {
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
                userId, "Priya Interior Design", "priya@studio.in", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId, "Priya Interior Design", "priya@studio.in",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        OnboardingCompletionRequest req = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Priya Atelier",
                "priya-atelier",
                "Principal Designer",
                "Modern luxury residential design in Mumbai",
                2018,
                "STUDIO_2_5",
                "LUXURY",
                "Bandra West",
                "Mumbai",
                "Mumbai Suburban",
                "Maharashtra",
                "400050",
                "IN",
                true,
                false,
                null,
                List.of("Modular Kitchen", "Living Room"),
                List.of("Modern Minimalist", "Warm Contemporary"),
                List.of("Mumbai", "Bandra"),
                "+919876543210",
                "+919876543210",
                "hello@priya.in",
                "https://priya.in",
                "https://instagram.com/priya",
                true,
                true
        );

        var completion = onboardingService.completeOnboarding(preActor, req, null, null);
        studioId = completion.studio().id();

        designerActor = new ActorContext(
                userId, "Priya Interior Design", "priya@studio.in",
                Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioId, "OWNER", "PASSWORD", true
        );
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }

    @Test
    @DisplayName("Complete Project Lifecycle: Create -> List -> Update -> Reorder -> Archive -> Restore -> Portfolio Integration")
    void testCompleteProjectLifecycle() {
        MockHttpServletRequest req = createMockRequest(designerActor);

        // 1. Initially 0 projects
        ResponseEntity<List<ProjectSummaryResponse>> initialList = projectController.listProjects(
                req, null, studioId, null, null, null, null, false
        );
        assertEquals(HttpStatus.OK, initialList.getStatusCode());
        assertTrue(initialList.getBody().isEmpty());

        // 2. Create first project (Complete details -> READY)
        CreateProjectRequest createReq1 = new CreateProjectRequest(
                "Sea Breeze Penthouse",
                ProjectCategory.COMPLETE_HOME_INTERIOR,
                "Contemporary 4BHK penthouse facing the Arabian Sea.",
                "Detailed architectural interior overhaul including customized teak cabinetry and Italian marble.",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                List.of(ProjectStyle.MODERN_MINIMALIST, ProjectStyle.WARM_CONTEMPORARY),
                "Mumbai",
                "Mumbai Suburban",
                "Maharashtra",
                "IN",
                2025,
                BudgetVisibility.RANGE,
                new BigDecimal("4500000"),
                new BigDecimal("6000000"),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Mehta Family",
                new BigDecimal("3500"),
                AreaUnit.SQ_FT,
                VisibilityStatus.PORTFOLIO,
                true,
                "Published in design magazine"
        );

        ResponseEntity<ProjectDetailResponse> createRes1 = projectController.createProject(req, null, studioId, createReq1);
        assertEquals(HttpStatus.CREATED, createRes1.getStatusCode());
        ProjectDetailResponse project1 = createRes1.getBody();
        assertNotNull(project1);
        assertEquals("sea-breeze-penthouse", project1.slug());
        assertEquals(ProjectStatus.READY, project1.projectStatus());
        assertTrue(project1.isReady());
        assertTrue(project1.featured());
        assertEquals(2, project1.styleCodes().size());

        // 3. Create second project (Incomplete details -> DRAFT)
        CreateProjectRequest createReq2 = new CreateProjectRequest(
                "Urban Loft",
                ProjectCategory.LIVING_ROOM,
                null, // missing shortDescription
                null,
                PropertyType.APARTMENT,
                ProjectScope.PARTIAL_INTERIOR,
                List.of(ProjectStyle.INDUSTRIAL),
                null, // missing city
                null,
                null, // missing state
                "IN",
                null,
                BudgetVisibility.HIDDEN,
                null,
                null,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                null,
                null,
                VisibilityStatus.PRIVATE,
                false,
                null
        );

        ResponseEntity<ProjectDetailResponse> createRes2 = projectController.createProject(req, null, studioId, createReq2);
        assertEquals(HttpStatus.CREATED, createRes2.getStatusCode());
        ProjectDetailResponse project2 = createRes2.getBody();
        assertNotNull(project2);
        assertEquals("urban-loft", project2.slug());
        assertEquals(ProjectStatus.DRAFT, project2.projectStatus());
        assertFalse(project2.isReady());

        // 4. List projects
        ResponseEntity<List<ProjectSummaryResponse>> listRes = projectController.listProjects(
                req, null, studioId, null, null, null, null, false
        );
        assertEquals(2, listRes.getBody().size());

        // 5. Update Project 2 to become READY
        UpdateProjectRequest updateReq2 = new UpdateProjectRequest(
                project2.version(),
                "Urban Loft Bandra",
                ProjectCategory.LIVING_ROOM,
                "Chic industrial living room with exposed brick and fluted glass.",
                "Complete living space overhaul.",
                PropertyType.APARTMENT,
                ProjectScope.PARTIAL_INTERIOR,
                List.of(ProjectStyle.INDUSTRIAL),
                "Mumbai",
                null,
                "Maharashtra",
                "IN",
                2024,
                BudgetVisibility.STARTING_FROM,
                new BigDecimal("1200000"),
                null,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                new BigDecimal("850"),
                AreaUnit.SQ_FT,
                VisibilityStatus.PORTFOLIO,
                false,
                null
        );

        ResponseEntity<ProjectDetailResponse> updateRes2 = projectController.updateProject(
                req, null, studioId, project2.id(), updateReq2
        );
        assertEquals(HttpStatus.OK, updateRes2.getStatusCode());
        ProjectDetailResponse updatedProject2 = updateRes2.getBody();
        assertEquals("urban-loft-bandra", updatedProject2.slug());
        assertEquals(ProjectStatus.READY, updatedProject2.projectStatus());
        assertTrue(updatedProject2.isReady());
        assertEquals(2L, updatedProject2.version());

        // 6. Optimistic Locking: Attempt update with stale version
        UpdateProjectRequest staleReq = new UpdateProjectRequest(
                1L, // stale version
                "Urban Loft Stale",
                ProjectCategory.LIVING_ROOM,
                "Stale description here...",
                null, null, null, null,
                "Mumbai", null, "Maharashtra", "IN", null,
                null, null, null, null, null, null, null, null, null, null, null
        );

        assertThrows(ConflictException.class, () ->
                projectController.updateProject(req, null, studioId, project2.id(), staleReq)
        );

        // 7. Reorder Projects
        ReorderProjectsRequest reorderReq = new ReorderProjectsRequest(List.of(project2.id(), project1.id()));
        ResponseEntity<List<ProjectSummaryResponse>> reorderRes = projectController.reorderProjects(
                req, null, studioId, reorderReq
        );
        assertEquals(2, reorderRes.getBody().size());
        assertEquals(project2.id(), reorderRes.getBody().get(0).id());

        // 8. Workspace summary reflects readiness and checklist
        ResponseEntity<WorkspaceSummaryResponse> wsSummary = workspaceController.getWorkspaceSummary(req, null, studioId);
        assertEquals(HttpStatus.OK, wsSummary.getStatusCode());
        WorkspaceSummaryResponse summary = wsSummary.getBody();
        assertNotNull(summary);

        // Verify module status is READY
        var projectsModule = summary.modules().stream().filter(m -> m.id().equals("projects")).findFirst().orElseThrow();
        assertEquals("READY", projectsModule.status());
        assertEquals("Manage Projects", projectsModule.ctaLabel());

        // Verify checklist item is completed
        var projectsChecklist = summary.setupChecklist().stream().filter(c -> c.id().equals("projects")).findFirst().orElseThrow();
        assertTrue(projectsChecklist.completed());

        // 9. Portfolio Preview Integration: Real projects are included
        portfolioController.initializePortfolio(req, null, studioId, new InitializePortfolioRequest(null));
        ResponseEntity<PortfolioPreviewResponse> previewRes = portfolioController.getPortfolioPreview(req, null, studioId);
        assertEquals(HttpStatus.OK, previewRes.getStatusCode());
        PortfolioPreviewResponse preview = previewRes.getBody();
        assertNotNull(preview);
        assertNotNull(preview.portfolioProjects());
        assertEquals(2, preview.portfolioProjects().size());

        // Mehta family client name is displayed for project 1 because visibility is DISPLAY
        var previewP1 = preview.portfolioProjects().stream().filter(p -> p.id().equals(project1.id())).findFirst().orElseThrow();
        assertEquals("Mehta Family", previewP1.clientName());
        assertEquals("₹45.0 L - ₹60.0 L", previewP1.budgetFormatted());

        // Project 2 client name is hidden
        var previewP2 = preview.portfolioProjects().stream().filter(p -> p.id().equals(project2.id())).findFirst().orElseThrow();
        assertNull(previewP2.clientName());
        assertEquals("From ₹12.0 L", previewP2.budgetFormatted());

        // 10. Archive project 2
        ResponseEntity<ProjectDetailResponse> archiveRes = projectController.archiveProject(
                req, null, studioId, project2.id(), new ProjectActionRequest(updatedProject2.version())
        );
        assertEquals(HttpStatus.OK, archiveRes.getStatusCode());
        assertEquals(ProjectStatus.ARCHIVED, archiveRes.getBody().projectStatus());
        assertEquals(VisibilityStatus.PRIVATE, archiveRes.getBody().visibilityStatus());

        // Only 1 project remains in portfolio preview
        ResponseEntity<PortfolioPreviewResponse> previewResAfterArchive = portfolioController.getPortfolioPreview(req, null, studioId);
        assertEquals(1, previewResAfterArchive.getBody().portfolioProjects().size());

        // 11. Restore project 2
        ResponseEntity<ProjectDetailResponse> restoreRes = projectController.restoreProject(
                req, null, studioId, project2.id(), new ProjectActionRequest(archiveRes.getBody().version())
        );
        assertEquals(HttpStatus.OK, restoreRes.getStatusCode());
        assertEquals(ProjectStatus.READY, restoreRes.getBody().projectStatus());
    }
}
