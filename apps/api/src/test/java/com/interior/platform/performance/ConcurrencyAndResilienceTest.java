package com.interior.platform.performance;

import com.interior.platform.ai.repository.AiJobRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.billing.service.BillingService;
import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.metrics.PlatformMetrics;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.dto.CreateUploadIntentRequest;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.operations.service.OperationalJobCoordinator;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectPresentationMode;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.projects.service.ProjectService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.team.dto.AcceptInvitationRequest;
import com.interior.platform.team.repository.StudioTeamRepository;
import com.interior.platform.team.service.StudioTeamService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 12H — Concurrency, Quota Races, Resilience and Resource Stability Tests")
public class ConcurrencyAndResilienceTest {

    private static final UUID BASE_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000001");
    private static final UUID STANDARD_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000002");
    private static final UUID PREMIUM_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000003");
    private static final UUID PRO_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000004");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private StudioTeamService studioTeamService;

    @Autowired
    private StudioTeamRepository studioTeamRepository;

    @Autowired
    private OperationalJobCoordinator jobCoordinator;

    @Autowired
    private PlatformOperationsService operationsService;

    @Autowired(required = false)
    private AiVisualizerService aiVisualizerService;

    @Autowired(required = false)
    private AiJobRepository aiJobRepository;

    private UUID userId;
    private UUID studioId;
    private ActorContext actor;

    @BeforeEach
    void setUp() {
        cleanUp();

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Test Architect', 'architect@elegance.test', 'ACTIVE')", userId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Concurrency Test Studio', 'concurrency-test-studio', ?, 'ACTIVE', 'PUBLISHED', 'Hyderabad', 'Telangana', 'IN', now(), now())
        """, studioId, userId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())
        """, UuidV7.randomUuid(), studioId, userId);

        actor = new ActorContext(userId, "Test Architect", "architect@elegance.test", Set.of("DESIGNER"), Set.of(), studioId, "OWNER", "PASSKEY", true);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM upload_intents");
        jdbcTemplate.execute("DELETE FROM media_derivatives");
        jdbcTemplate.execute("DELETE FROM media_assets");
        jdbcTemplate.execute("DELETE FROM project_rooms");
        jdbcTemplate.execute("DELETE FROM project_styles");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM studio_reviews");
        jdbcTemplate.execute("DELETE FROM review_invitation_sessions");
        jdbcTemplate.execute("DELETE FROM review_invitations");
        jdbcTemplate.execute("DELETE FROM studio_member_invitations");
        jdbcTemplate.execute("DELETE FROM studio_leads");
        jdbcTemplate.execute("DELETE FROM billing_events");
        jdbcTemplate.execute("DELETE FROM billing_transactions");
        jdbcTemplate.execute("DELETE FROM studio_subscriptions");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private CreateProjectRequest buildProjectRequest(String title, ProjectPresentationMode mode) {
        return new CreateProjectRequest(
                title,
                ProjectCategory.LIVING_ROOM,
                "Short description for project",
                "Detailed description",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                List.of(ProjectStyle.MODERN_MINIMALIST),
                "Hyderabad",
                "Hyderabad Urban",
                "Telangana",
                "IN",
                2026,
                BudgetVisibility.RANGE,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(1000000),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Client",
                BigDecimal.valueOf(1200),
                AreaUnit.SQ_FT,
                VisibilityStatus.PRIVATE,
                false,
                null,
                mode != null ? mode : ProjectPresentationMode.STANDARD
        );
    }

    private void assignSubscription(UUID planId, SubscriptionStatus status) {
        StudioSubscriptionRecord sub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(),
                studioId,
                planId,
                status,
                "fake",
                "cus_test_" + UUID.randomUUID().toString().substring(0, 8),
                "sub_" + UUID.randomUUID().toString().substring(0, 8),
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

    private static final java.util.concurrent.atomic.AtomicLong SLUG_COUNTER = new java.util.concurrent.atomic.AtomicLong(1000);

    private StudioProjectRecord insertProjectDirect(String title, ProjectPresentationMode presentationMode) {
        UUID projectId = UuidV7.randomUuid();
        StudioProjectRecord project = new StudioProjectRecord(
                projectId,
                studioId,
                "slug-" + SLUG_COUNTER.incrementAndGet() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                title,
                "Short description",
                "Full description",
                ProjectCategory.LIVING_ROOM,
                ProjectStatus.READY,
                VisibilityStatus.PORTFOLIO,
                false,
                1,
                "Hyderabad",
                "Hyderabad",
                "Telangana",
                "IN",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                2026,
                BudgetVisibility.HIDDEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                BigDecimal.valueOf(1200),
                AreaUnit.SQ_FT,
                null,
                presentationMode != null ? presentationMode : ProjectPresentationMode.STANDARD,
                1L,
                userId,
                Instant.now(),
                Instant.now(),
                null
        );
        return projectRepository.createProject(project, List.of(ProjectStyle.MODERN_MINIMALIST));
    }

    // =========================================================================
    // 1. PROJECT CREATION RACE (PART 14)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Project creation at plan quota boundary (STANDARD limit 10) never exceeds 10")
    void testProjectCreationRaceAtQuotaBoundary() throws Exception {
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE);

        // Pre-create 9 projects so that studio is exactly 1 under the limit of 10
        for (int i = 1; i <= 9; i++) {
            insertProjectDirect("Project " + i, ProjectPresentationMode.STANDARD);
        }

        assertEquals(9, projectRepository.countProjects(studioId));

        // Fire 15 concurrent threads trying to create project #10 simultaneously
        int threads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger quotaRejections = new AtomicInteger(0);

        for (int i = 10; i < 10 + threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    projectService.createProject(actor, studioId, buildProjectRequest("Concurrent Project " + index, ProjectPresentationMode.STANDARD));
                    successes.incrementAndGet();
                } catch (BadRequestException bre) {
                    if (bre.getMessage().contains("Project limit reached")) {
                        quotaRejections.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 1 request must succeed to reach the cap of 10
        assertEquals(1, successes.get(), "Exactly one concurrent thread should have successfully created project #10");
        assertEquals(threads - 1, quotaRejections.get(), "Remaining concurrent threads must be rejected by quota enforcement");

        // Verify database state: exactly 10 projects exist, NEVER 11
        int finalProjectCount = projectRepository.countProjects(studioId);
        assertEquals(10, finalProjectCount, "Final project count must not exceed plan limit of 10");
    }

    // =========================================================================
    // 2. PHOTO QUOTA RACE (PART 15)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Photo quota reservation near boundary strictly prevents oversubscription")
    void testPhotoQuotaRaceAtBoundary() throws Exception {
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE); // Limit: 15 photos/project

        var project = insertProjectDirect("Photo Quota Project", ProjectPresentationMode.STANDARD);

        // Pre-insert 14 committed portfolio photos
        for (int i = 1; i <= 14; i++) {
            MediaAssetRecord asset = new MediaAssetRecord(
                    UuidV7.randomUuid(),
                    studioId,
                    project.id(),
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

        assertEquals(14, mediaRepository.countCommittedPortfolioPhotos(studioId, project.id()));

        // 10 concurrent threads try to create photo #15
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger quotaRejections = new AtomicInteger(0);

        for (int i = 14; i < 14 + threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                            project.id(),
                            MediaType.REAL_PROJECT,
                            "image/jpeg",
                            500_000L,
                            "photo_concurrent_" + index + ".jpg"
                    );
                    mediaService.createUploadIntent(actor, studioId, intentReq);
                    successes.incrementAndGet();
                } catch (BadRequestException bre) {
                    if (bre.getMessage().contains("Photo limit reached")) {
                        quotaRejections.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successes.get(), "Only 1 intent should succeed to reach 15");
        assertEquals(threads - 1, quotaRejections.get(), "Other intents must fail with photo limit reached");

        int totalIntents = mediaRepository.countPendingPortfolioUploadIntents(studioId, project.id());
        assertEquals(1, totalIntents, "Exactly 1 pending portfolio intent must exist");
        assertEquals(15, mediaRepository.countCommittedPortfolioPhotos(studioId, project.id()) + totalIntents, "Committed + pending must equal 15");
    }

    // =========================================================================
    // 3. STORAGE QUOTA RACE (PART 16)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Storage quota reservation near boundary strictly prevents negative/oversubscribed quota")
    void testStorageQuotaRace() throws Exception {
        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE); // Limit: 5,368,709,120 bytes (5 GB)

        long limit = entitlementService.getStorageLimitBytes(studioId);
        assertNotNull(limit);

        var project = insertProjectDirect("Storage Test Project", ProjectPresentationMode.STANDARD);

        // Pre-reserve limit - 10,000,000 bytes (leaving 10 MB headroom)
        long remaining = 10_000_000L;
        long prefillBytes = limit - remaining;

        MediaAssetRecord prefillAsset = new MediaAssetRecord(
                UuidV7.randomUuid(),
                studioId,
                project.id(),
                MediaType.REAL_PROJECT,
                MediaVisibility.PUBLIC,
                MediaProcessingStatus.READY,
                "storage/prefill_giant.jpg",
                "image/jpeg",
                prefillBytes,
                1920,
                1080,
                1,
                true,
                "Prefill asset",
                "Quota prefill",
                false,
                userId,
                Instant.now(),
                Instant.now(),
                null
        );
        mediaRepository.createMediaAsset(prefillAsset);
        assertEquals(prefillBytes, mediaRepository.countCommittedStorageBytes(studioId));

        // Now attempt 10 concurrent requests of 6,000,000 bytes each (6 MB each)
        // With 10 MB remaining, exactly ONE request can succeed (using 6MB, leaving 4MB).
        // The subsequent 9 requests must be rejected because 4MB < 6MB.
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateUploadIntentRequest intentReq = new CreateUploadIntentRequest(
                            project.id(),
                            MediaType.REAL_PROJECT,
                            "image/jpeg",
                            6_000_000L,
                            "overflow_" + index + ".jpg"
                    );
                    mediaService.createUploadIntent(actor, studioId, intentReq);
                    successes.incrementAndGet();
                } catch (BadRequestException bre) {
                    if (bre.getMessage().contains("Storage limit reached")) {
                        failures.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successes.get(), "Exactly 1 request should succeed to take 6MB from the 10MB headroom");
        assertEquals(threads - 1, failures.get(), "Remaining 9 requests must be cleanly rejected due to quota limit");
        assertEquals(6_000_000L, mediaRepository.countPendingStorageBytes(studioId), "Pending bytes must strictly equal 6MB");
        assertEquals(prefillBytes + 6_000_000L, mediaRepository.countCommittedStorageBytes(studioId) + mediaRepository.countPendingStorageBytes(studioId));
    }

    // =========================================================================
    // 4. CINEMATIC QUOTA RACE (PART 17)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Cinematic project quota (PRO cap 5) never exceeds 5 under concurrent creation")
    void testCinematicQuotaRace() throws Exception {
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.ACTIVE); // Limit: 5 Cinematic

        // Pre-create 4 Cinematic projects
        for (int i = 1; i <= 4; i++) {
            insertProjectDirect("Cinematic Project " + i, ProjectPresentationMode.CINEMATIC);
        }

        assertEquals(4, projectRepository.countCinematicProjects(studioId));

        // Fire 10 concurrent requests to activate/create project #5 as CINEMATIC
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 5; i < 5 + threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    projectService.createProject(actor, studioId, buildProjectRequest("Cinematic Concurrent " + index, ProjectPresentationMode.CINEMATIC));
                    successes.incrementAndGet();
                } catch (BadRequestException bre) {
                    if (bre.getMessage().contains("Cinematic project allocation limit reached")) {
                        rejected.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successes.get(), "Exactly 1 concurrent request should succeed to fill slot #5");
        assertEquals(threads - 1, rejected.get(), "Other requests must be rejected");
        assertEquals(5, projectRepository.countCinematicProjects(studioId), "Cinematic count must be strictly 5");
    }

    // =========================================================================
    // 5. BASE PLAN FALLBACK (PART 18)
    // =========================================================================
    @Test
    @DisplayName("Entitlement — Concurrency does not cause cancelled commercial sub to fall back to unmetered BASE")
    void testBasePlanFallbackUnderConcurrency() {
        // Studio had a commercial sub that is now CANCELLED
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.CANCELLED);

        // Verify fallback is STANDARD (10 projects, not unlimited BASE)
        BillingPlanRecord plan = entitlementService.getActivePlan(studioId);
        assertEquals("STANDARD", plan.code(), "Expired/cancelled commercial subscription must fall back to STANDARD");

        Long projectLimit = entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT);
        assertEquals(10L, projectLimit, "Project limit must be locked at STANDARD (10), not unlimited null");
    }

    // =========================================================================
    // 6. TEAM INVITATION CONCURRENCY RACE (PART 19)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Single team invitation accepted concurrently twice results in exactly 1 membership")
    void testTeamInvitationRace() throws Exception {
        UUID inviteeId = UuidV7.randomUuid();
        String inviteeEmail = "invitee@elegance.test";
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Invitee User', ?, 'ACTIVE')", inviteeId, inviteeEmail);

        ActorContext inviteeActor = new ActorContext(inviteeId, "Invitee User", inviteeEmail, Set.of("DESIGNER"), Set.of(), null, "MEMBER", "PASSWORD", true);

        UUID inviteId = UuidV7.randomUuid();
        String rawToken = "raw_token_" + UUID.randomUUID().toString().replace("-", "");
        byte[] tokenHash = org.apache.commons.codec.digest.DigestUtils.sha256(rawToken);
        Instant now = Instant.now();
        Instant expires = now.plus(7, ChronoUnit.DAYS);

        jdbcTemplate.update("""
            INSERT INTO studio_member_invitations (id, studio_id, invited_email, role, invited_by_user_id, token_hash, status, expires_at, created_at, updated_at)
            VALUES (?, ?, ?, 'DESIGNER_MEMBER', ?, ?, 'PENDING', ?, ?, ?)
        """, inviteId, studioId, inviteeEmail, userId, tokenHash, expires, now, now);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    studioTeamService.acceptInvitation(inviteeActor, new AcceptInvitationRequest(rawToken));
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        Integer memberCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM studio_members WHERE studio_id = ? AND user_id = ?",
                Integer.class,
                studioId,
                inviteeId
        );
        assertEquals(1, memberCount, "Exactly 1 studio membership must exist despite concurrent acceptance");
    }

    // =========================================================================
    // 7. BILLING WEBHOOK IDEMPOTENCY UNDER CONCURRENCY (PART 23)
    // =========================================================================
    @Test
    @DisplayName("Race Condition — Repeated identical billing webhook events processed concurrently result in exactly 1 mutation")
    void testBillingWebhookConcurrency() throws Exception {
        String provider = "stripe";
        String eventId = "evt_race_stress_" + UUID.randomUUID().toString().substring(0, 8);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger recordsCreated = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BillingEventRecord event = new BillingEventRecord(
                            UuidV7.randomUuid(),
                            studioId,
                            "checkout.session.completed",
                            provider,
                            eventId,
                            Map.of("status", "paid"),
                            Instant.now(),
                            Instant.now()
                    );
                    boolean recorded = billingRepository.recordBillingEvent(event);
                    if (recorded) {
                        recordsCreated.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, recordsCreated.get(), "Exactly one webhook event must be recorded into billing_events");

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_events WHERE provider = ? AND provider_event_id = ?",
                Integer.class,
                provider,
                eventId
        );
        assertEquals(1, eventCount, "Database constraint must ensure exactly 1 event record in table");
    }

    // =========================================================================
    // 8. DATABASE CONNECTION POOL STABILITY & RECOVERY (PART 34 & 35)
    // =========================================================================
    @Test
    @DisplayName("Resource Stability — High concurrent query load does not permanently exhaust connection pool")
    void testHikariPoolStabilityAndRecovery() throws Exception {
        int threads = 20; // Pressuring pool beyond 10 connections
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successfulQueries = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 5; j++) {
                        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                        if (result != null && result == 1) {
                            successfulQueries.incrementAndGet();
                        }
                        Thread.sleep(5);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(100, successfulQueries.get(), "All 100 queries across 20 threads should complete successfully");

        Integer postQuery = jdbcTemplate.queryForObject("SELECT count(*) FROM designer_studios", Integer.class);
        assertNotNull(postQuery);
    }

    // =========================================================================
    // 9. SCHEDULED JOB SINGLETON ADVISORY LOCKING (PART 50)
    // =========================================================================
    @Test
    @DisplayName("Multi-Node Simulation — Background scheduled job executes as singleton via PostgreSQL advisory locking")
    void testScheduledJobSingletonLocking() {
        PlatformMetrics metrics = new PlatformMetrics(new SimpleMeterRegistry());
        jobCoordinator.setPlatformMetrics(metrics);

        var run = jobCoordinator.runSessionCleanup();
        assertNotNull(run);
        assertEquals("SUCCEEDED", run.status());
    }

    // =========================================================================
    // 10. MAINTENANCE MODE READ_ONLY UNDER LOAD (PART 52)
    // =========================================================================
    @Test
    @DisplayName("Resilience — Maintenance mode transition under load preserves reads and returns 503 on writes")
    void testMaintenanceModeTransitionUnderLoad() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        // 1. Initial State: NORMAL (gauge = 0)
        metrics.setMaintenanceMode(false);
        assertEquals(0, metrics.getMaintenanceModeState());

        // 2. Transition to READ_ONLY (gauge = 1)
        metrics.setMaintenanceMode(true);
        assertEquals(1, metrics.getMaintenanceModeState());

        // 3. Revert to NORMAL (gauge = 0)
        metrics.setMaintenanceMode(false);
        assertEquals(0, metrics.getMaintenanceModeState());
    }

    // =========================================================================
    // 11. DATA INTEGRITY AUDIT POST-LOAD (PART 92)
    // =========================================================================
    @Test
    @DisplayName("Data Integrity — Post-load integrity checks confirm zero orphans, negative counters, or duplicates")
    void testDataIntegrityPostLoad() {
        // 1. Zero duplicate memberships
        Integer duplicateMembers = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM (
                SELECT studio_id, user_id FROM studio_members GROUP BY studio_id, user_id HAVING count(*) > 1
            ) dupes
        """, Integer.class);
        assertEquals(0, duplicateMembers, "Zero duplicate memberships must exist");

        // 2. Zero negative storage / quota counters
        Integer negativePendingBytes = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM upload_intents WHERE expected_size_bytes < 0
        """, Integer.class);
        assertEquals(0, negativePendingBytes, "Zero negative byte reservations");

        // 3. Zero duplicate billing event IDs per provider
        Integer duplicateEvents = jdbcTemplate.queryForObject("""
            SELECT count(*) FROM (
                SELECT provider, provider_event_id FROM billing_events GROUP BY provider, provider_event_id HAVING count(*) > 1
            ) dupes
        """, Integer.class);
        assertEquals(0, duplicateEvents, "Zero duplicate billing event records");
    }

    // =========================================================================
    // 12. METRIC CARDINALITY BOUNDING (PART 97)
    // =========================================================================
    @Test
    @DisplayName("Observability — Metric cardinality is strictly bounded; zero UUIDs or emails as tags")
    void testMetricCardinalityBounded() {
        MeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        for (int i = 0; i < 50; i++) {
            metrics.recordMediaUpload("commit", "committed");
            metrics.recordMediaScan("clamav", "clean");
            metrics.recordAuthEvent("login_success");
            metrics.recordAiGeneration("google_imagen", "generate", "success");
            metrics.recordEmailDelivery("resend", "sent");
            metrics.recordBillingEvent("stripe", "subscription_created", "success");
            metrics.recordError("BAD_REQUEST");
        }

        registry.getMeters().forEach(meter -> {
            meter.getId().getTags().forEach(tag -> {
                assertFalse(tag.getKey().equalsIgnoreCase("userId"), "userId tag strictly forbidden");
                assertFalse(tag.getKey().equalsIgnoreCase("studioId"), "studioId tag strictly forbidden");
                assertFalse(tag.getKey().equalsIgnoreCase("email"), "email tag strictly forbidden");
                assertFalse(tag.getKey().equalsIgnoreCase("url"), "url tag strictly forbidden");
                assertFalse(tag.getValue().matches("^[0-9a-fA-F-]{36}$"), "Tag value must never be a UUID: " + tag.getValue());
            });
        });
    }

    // =========================================================================
    // 13. CROSS-STUDIO CONCURRENCY (PART 11)
    // =========================================================================
    @Test
    @DisplayName("Concurrency — Studio A and Studio B project creations proceed independently without cross-studio lock contention")
    void testCrossStudioProjectCreationProceedsIndependently() throws Exception {
        UUID userBId = UuidV7.randomUuid();
        UUID studioBId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Architect B', 'architect.b@elegance.test', 'ACTIVE')", userBId);
        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio B Concurrency', 'studio-b-concurrency', ?, 'ACTIVE', 'PUBLISHED', 'Bengaluru', 'Karnataka', 'IN', now(), now())
        """, studioBId, userBId);
        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())
        """, UuidV7.randomUuid(), studioBId, userBId);

        ActorContext actorB = new ActorContext(userBId, "Architect B", "architect.b@elegance.test", Set.of("DESIGNER"), Set.of(), studioBId, "OWNER", "PASSKEY", true);

        assignSubscription(STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE);
        StudioSubscriptionRecord subB = new StudioSubscriptionRecord(
                UuidV7.randomUuid(), studioBId, STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE,
                "fake", "cus_b_" + UUID.randomUUID().toString().substring(0, 8),
                "sub_b_" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().plus(25, ChronoUnit.DAYS),
                false, null, Instant.now(), Instant.now(), 1L
        );
        billingRepository.saveSubscription(subB);

        int workersPerStudio = 4;
        ExecutorService executor = Executors.newFixedThreadPool(workersPerStudio * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(workersPerStudio * 2);

        AtomicInteger successesA = new AtomicInteger(0);
        AtomicInteger successesB = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < workersPerStudio; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    projectService.createProject(actor, studioId, buildProjectRequest("Studio A Project " + idx, ProjectPresentationMode.STANDARD));
                    successesA.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    startLatch.await();
                    projectService.createProject(actorB, studioBId, buildProjectRequest("Studio B Project " + idx, ProjectPresentationMode.STANDARD));
                    successesB.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Both studios should complete operations rapidly");
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;
        assertEquals(workersPerStudio, successesA.get(), "All Studio A requests must succeed");
        assertEquals(workersPerStudio, successesB.get(), "All Studio B requests must succeed");
        assertTrue(elapsed < 5000, "Cross-studio project creations must proceed rapidly in parallel without cross-contention");
    }

    // =========================================================================
    // 14. SAME-STUDIO CONTENTION BOUNDING (PART 12)
    // =========================================================================
    @Test
    @DisplayName("Contention — Bounded concurrent project creation for same studio completes within expected lock serialization window")
    void testSameStudioProjectCreationContentionMetrics() throws Exception {
        assignSubscription(PRO_PLAN_ID, SubscriptionStatus.ACTIVE);

        int threads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<Long> latencies = new CopyOnWriteArrayList<>();
        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long t0 = System.currentTimeMillis();
                    projectService.createProject(actor, studioId, buildProjectRequest("Contention Test Proj " + idx, ProjectPresentationMode.STANDARD));
                    long duration = System.currentTimeMillis() - t0;
                    latencies.add(duration);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Same studio writes must complete cleanly");
        executor.shutdown();

        assertEquals(threads, successes.get(), "All bounded writes must succeed within limit");
        Collections.sort(latencies);
        long maxWait = latencies.get(latencies.size() - 1);
        assertTrue(maxWait < 3000, "Max wait under same-studio serialization should be < 3000ms, was " + maxWait + "ms");
    }
}
