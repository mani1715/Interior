package com.interior.platform.billing;

import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.media.domain.*;
import com.interior.platform.media.dto.CreateUploadIntentRequest;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.projects.domain.*;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.UpdateProjectRequest;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.projects.service.ProjectService;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 30 — Entitlement & Capacity Quota Integration Tests")
class EntitlementQuotaIntegrationTest {

    private static final UUID BASE_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000001");
    private static final UUID STANDARD_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000002");
    private static final UUID PREMIUM_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000003");
    private static final UUID PRO_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    private UUID userId;
    private UUID studioId;
    private ActorContext actor;

    @BeforeEach
    void setUp() {
        cleanUp();

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Test Designer', 'designer@test.local', 'ACTIVE')", userId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Quota', 'studio-quota', ?, 'ACTIVE', 'PUBLISHED', 'Bengaluru', 'Karnataka', 'IN', now(), now())
        """, studioId, userId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())
        """, UuidV7.randomUuid(), studioId, userId);

        actor = new ActorContext(userId, "Test Designer", "designer@test.local", Set.of("DESIGNER"), Set.of(), studioId, "OWNER", "PASSKEY", true);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM upload_intents");
        jdbcTemplate.execute("DELETE FROM media_derivatives");
        jdbcTemplate.execute("DELETE FROM media_assets");
        jdbcTemplate.execute("DELETE FROM project_rooms");
        jdbcTemplate.execute("DELETE FROM project_styles");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM billing_events");
        jdbcTemplate.execute("DELETE FROM billing_transactions");
        jdbcTemplate.execute("DELETE FROM studio_subscriptions");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private void assignSubscription(UUID planId, SubscriptionStatus status) {
        StudioSubscriptionRecord sub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(),
                studioId,
                planId,
                status,
                "TEST_GATEWAY",
                "cus_test",
                "sub_" + UUID.randomUUID(),
                Instant.now().minus(5, ChronoUnit.DAYS),
                Instant.now().plus(25, ChronoUnit.DAYS),
                false,
                null,
                Instant.now(),
                Instant.now(),
                1L
        );
        billingRepository.saveSubscription(sub);
    }

    private StudioProjectRecord insertProjectDirect(String title, ProjectPresentationMode presentationMode, ProjectStatus status) {
        UUID projectId = UuidV7.randomUuid();
        StudioProjectRecord project = new StudioProjectRecord(
                projectId,
                studioId,
                "slug-" + projectId,
                title,
                "Short description for " + title,
                "Detailed description",
                ProjectCategory.LIVING_ROOM,
                status,
                VisibilityStatus.PORTFOLIO,
                false,
                1,
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "IN",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                2026,
                BudgetVisibility.RANGE,
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(2500000),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Mr. Test",
                BigDecimal.valueOf(1200),
                AreaUnit.SQ_FT,
                null,
                presentationMode,
                1L,
                userId,
                Instant.now(),
                Instant.now(),
                status == ProjectStatus.ARCHIVED ? Instant.now() : null
        );
        return projectRepository.createProject(project, List.of(ProjectStyle.MODERN_MINIMALIST));
    }

    @Test
    @DisplayName("Project Quota: STANDARD plan limits projects to 10 (including drafts & archived)")
    void testProjectCreationLimitOnStandardPlan() {
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE);

        assertEquals(10L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT));

        // Insert 9 active projects and 1 archived project (total 10 retained)
        for (int i = 1; i <= 9; i++) {
            insertProjectDirect("Project " + i, ProjectPresentationMode.STANDARD, ProjectStatus.READY);
        }
        insertProjectDirect("Archived Project", ProjectPresentationMode.STANDARD, ProjectStatus.ARCHIVED);

        assertEquals(10, projectRepository.countProjects(studioId));

        // Attempting to create the 11th project via entitlement assertion should fail
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                entitlementService.assertProjectCreationAllowed(studioId)
        );
        assertTrue(ex.getMessage().contains("Project limit reached for your current plan (10/10)"));

        // Attempting via projectService.createProject should also fail
        CreateProjectRequest createReq = new CreateProjectRequest(
                "Project Eleven",
                ProjectCategory.LIVING_ROOM,
                "Short description",
                "Detailed description",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                List.of(ProjectStyle.MODERN_MINIMALIST),
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "IN",
                2026,
                BudgetVisibility.RANGE,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(1000000),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Client",
                BigDecimal.valueOf(1000),
                AreaUnit.SQ_FT,
                VisibilityStatus.PRIVATE,
                false,
                null,
                ProjectPresentationMode.STANDARD
        );

        BadRequestException serviceEx = assertThrows(BadRequestException.class, () ->
                projectService.createProject(actor, studioId, createReq)
        );
        assertTrue(serviceEx.getMessage().contains("Project limit reached for your current plan (10/10)"));
    }

    @Test
    @DisplayName("Project Quota: PRO plan allows up to 20 total projects")
    void testProjectCreationLimitOnProPlan() {
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.ACTIVE);

        assertEquals(20L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT));

        // Insert 20 projects
        for (int i = 1; i <= 20; i++) {
            insertProjectDirect("Pro Project " + i, ProjectPresentationMode.STANDARD, ProjectStatus.READY);
        }

        assertEquals(20, projectRepository.countProjects(studioId));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                entitlementService.assertProjectCreationAllowed(studioId)
        );
        assertTrue(ex.getMessage().contains("Project limit reached for your current plan (20/20)"));
    }

    @Test
    @DisplayName("Photo Quota: STANDARD plan enforces 15 photos/project with pending upload intent reservations")
    void testProjectPhotoQuotaAndUploadIntentReservations() {
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE);

        StudioProjectRecord project = insertProjectDirect("Photo Quota Project", ProjectPresentationMode.STANDARD, ProjectStatus.DRAFT);
        UUID projId = project.id();

        assertEquals(15L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_PHOTO_LIMIT));

        // Insert 14 committed portfolio photos
        for (int i = 1; i <= 14; i++) {
            MediaAssetRecord asset = new MediaAssetRecord(
                    UuidV7.randomUuid(),
                    studioId,
                    projId,
                    MediaType.REAL_PROJECT,
                    MediaVisibility.PUBLIC,
                    MediaProcessingStatus.READY,
                    "storage/key_" + i + ".jpg",
                    "image/jpeg",
                    500_000L,
                    1920,
                    1080,
                    i,
                    i == 1,
                    "Alt " + i,
                    "Caption " + i,
                    false,
                    userId,
                    Instant.now(),
                    Instant.now(),
                    null
            );
            mediaRepository.createMediaAsset(asset);
        }

        // Add 1 unenrolled REFERENCE media asset — must NOT count towards the 15 limit
        MediaAssetRecord refAsset = new MediaAssetRecord(
                UuidV7.randomUuid(),
                studioId,
                projId,
                MediaType.REFERENCE,
                MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY,
                "storage/ref.jpg",
                "image/jpeg",
                200_000L,
                800,
                600,
                99,
                false,
                "Ref",
                "Ref",
                false,
                userId,
                Instant.now(),
                Instant.now(),
                null
        );
        mediaRepository.createMediaAsset(refAsset);

        assertEquals(14, mediaRepository.countCommittedPortfolioPhotos(studioId, projId));

        // 1 photo remaining: reservation of 1 photo succeeds
        assertDoesNotThrow(() -> entitlementService.assertProjectPhotoQuotaAllowed(studioId, projId, 1));

        // Create 1 pending upload intent (reserves 1 slot -> total 14 + 1 = 15)
        CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                projId,
                MediaType.REAL_PROJECT,
                "image/jpeg",
                1_000_000L,
                "living-room.jpg"
        );
        var intentRes = mediaService.createUploadIntent(actor, studioId, intentReq);
        assertNotNull(intentRes);

        assertEquals(1, mediaRepository.countPendingPortfolioUploadIntents(studioId, projId));

        // With 14 committed + 1 pending = 15, attempting to reserve another photo fails
        BadRequestException quotaEx = assertThrows(BadRequestException.class, () ->
                entitlementService.assertProjectPhotoQuotaAllowed(studioId, projId, 1)
        );
        assertTrue(quotaEx.getMessage().contains("Photo limit reached for this project (15/15)"));

        // Creating another upload intent via mediaService is blocked
        assertThrows(BadRequestException.class, () ->
                mediaService.createUploadIntent(actor, studioId, intentReq)
        );

        // Cancel / expire the pending intent -> reservation freed
        mediaRepository.updateUploadIntentStatus(intentRes.uploadIntentId(), UploadIntentStatus.EXPIRED);
        assertEquals(0, mediaRepository.countPendingPortfolioUploadIntents(studioId, projId));

        // Now creating upload intent succeeds again
        assertDoesNotThrow(() -> mediaService.createUploadIntent(actor, studioId, intentReq));
    }

    @Test
    @DisplayName("Cinematic Entitlement: STANDARD plan rejects Cinematic; PRO plan allows up to 5 Cinematic projects")
    void testCinematicPresentationEnforcement() {
        // 1. STANDARD plan: Cinematic not allowed
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE);
        assertFalse(entitlementService.hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO));

        StudioProjectRecord standardProj = insertProjectDirect("Standard Project", ProjectPresentationMode.STANDARD, ProjectStatus.READY);

        UpdateProjectRequest updateReqCinematic = new UpdateProjectRequest(
                standardProj.version(),
                standardProj.title(),
                standardProj.categoryCode(),
                standardProj.shortDescription(),
                standardProj.fullDescription(),
                standardProj.propertyType(),
                standardProj.projectScope(),
                List.of(ProjectStyle.MODERN_MINIMALIST),
                standardProj.city(),
                standardProj.district(),
                standardProj.state(),
                standardProj.country(),
                standardProj.completionYear(),
                standardProj.budgetVisibility(),
                standardProj.budgetMin(),
                standardProj.budgetMax(),
                standardProj.currency(),
                standardProj.clientNameVisibility(),
                standardProj.clientDisplayName(),
                standardProj.areaValue(),
                standardProj.areaUnit(),
                standardProj.visibilityStatus(),
                standardProj.featured(),
                null,
                ProjectPresentationMode.CINEMATIC
        );

        BadRequestException stdEx = assertThrows(BadRequestException.class, () ->
                projectService.updateProject(actor, studioId, standardProj.id(), updateReqCinematic)
        );
        assertTrue(stdEx.getMessage().contains("Cinematic project presentation is not included in your current plan"));

        // 2. PRO plan: allows up to 5 Cinematic projects
        cleanUp();
        setUp();
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.ACTIVE);
        assertTrue(entitlementService.hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO));
        assertEquals(5L, entitlementService.getNumericLimit(studioId, EntitlementKey.CINEMATIC_PROJECT_LIMIT));

        // Insert 5 cinematic projects
        for (int i = 1; i <= 5; i++) {
            insertProjectDirect("Cinematic Pro " + i, ProjectPresentationMode.CINEMATIC, ProjectStatus.READY);
        }
        assertEquals(5, projectRepository.countCinematicProjects(studioId));

        // 6th project in STANDARD mode
        StudioProjectRecord proj6 = insertProjectDirect("Sixth Project", ProjectPresentationMode.STANDARD, ProjectStatus.READY);

        // Attempting to switch the 6th project to CINEMATIC fails with quota error
        UpdateProjectRequest proj6ToCinematic = new UpdateProjectRequest(
                proj6.version(),
                proj6.title(),
                proj6.categoryCode(),
                proj6.shortDescription(),
                proj6.fullDescription(),
                proj6.propertyType(),
                proj6.projectScope(),
                List.of(ProjectStyle.MODERN_MINIMALIST),
                proj6.city(),
                proj6.district(),
                proj6.state(),
                proj6.country(),
                proj6.completionYear(),
                proj6.budgetVisibility(),
                proj6.budgetMin(),
                proj6.budgetMax(),
                proj6.currency(),
                proj6.clientNameVisibility(),
                proj6.clientDisplayName(),
                proj6.areaValue(),
                proj6.areaUnit(),
                proj6.visibilityStatus(),
                proj6.featured(),
                null,
                ProjectPresentationMode.CINEMATIC
        );

        BadRequestException cinematicQuotaEx = assertThrows(BadRequestException.class, () ->
                projectService.updateProject(actor, studioId, proj6.id(), proj6ToCinematic)
        );
        assertTrue(cinematicQuotaEx.getMessage().contains("Cinematic project allocation limit reached (5/5 projects)"));
    }

    @Test
    @DisplayName("Safe Fallback: Expired/Cancelled subscription falls back to STANDARD, NOT unlimited BASE")
    void testSubscriptionExpirationFallbackToStandard() {
        // Assign a CANCELLED subscription to PRO
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.CANCELLED);

        BillingPlanRecord effectivePlan = entitlementService.getActivePlan(studioId);
        assertEquals("STANDARD", effectivePlan.code(), "Expired/Cancelled subscription must fall back to STANDARD tier");
        assertEquals(10L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT));
        assertEquals(15L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_PHOTO_LIMIT));
        assertFalse(entitlementService.hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO));

        // Studio with no prior subscriptions falls back to legacy BASE
        UUID freshStudioId = UuidV7.randomUuid();
        BillingPlanRecord freshStudioPlan = entitlementService.getActivePlan(freshStudioId);
        assertEquals("BASE", freshStudioPlan.code(), "Unmanaged studio without subscription history retains BASE access");
        assertNull(entitlementService.getNumericLimit(freshStudioId, EntitlementKey.PROJECT_LIMIT));
    }

    @Test
    @DisplayName("Billing Summary: usage breakdown reflects accurate live metrics and quota thresholds")
    void testBillingSummaryUsageReporting() throws Exception {
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.ACTIVE);

        // Insert 3 projects: 1 cinematic, 2 standard
        insertProjectDirect("Pro Project 1", ProjectPresentationMode.CINEMATIC, ProjectStatus.READY);
        insertProjectDirect("Pro Project 2", ProjectPresentationMode.STANDARD, ProjectStatus.READY);
        insertProjectDirect("Pro Project 3", ProjectPresentationMode.STANDARD, ProjectStatus.READY);

        mockMvc.perform(get("/studio/billing")
                        .header("X-Studio-Id", studioId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE, actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlan.code").value("PRO"))
                .andExpect(jsonPath("$.usage.projectCount").value(3))
                .andExpect(jsonPath("$.usage.projectLimit").value(20))
                .andExpect(jsonPath("$.usage.cinematicProjectCount").value(1))
                .andExpect(jsonPath("$.usage.cinematicProjectLimit").value(5))
                .andExpect(jsonPath("$.usage.cinematicPortfolioAllowed").value(true));
    }
}
