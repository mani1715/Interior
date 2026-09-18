package com.interior.platform.designers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.SessionSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OnboardingClosureIntegrationTest {

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private SlugValidationService slugValidationService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private SessionSecurityService sessionSecurityService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        // Clean up tables between test runs in referential order to ensure complete isolation
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
    }

    private UserRecord createTestUser(String name, String email) {
        UUID userId = UUID.randomUUID();
        UserRecord user = new UserRecord(
                userId,
                name,
                email,
                "+919876543210",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UUID.randomUUID(), userId, "CUSTOMER", Instant.now());
        return user;
    }

    private OnboardingCompletionRequest createValidRequest(String studioName, String slug) {
        return new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                studioName,
                slug,
                "Principal Architect",
                "Crafting soulful spaces",
                2018,
                "5-10",
                "15L-35L",
                "Brodipet 4th Line",
                "Guntur",
                "Guntur",
                "Andhra Pradesh",
                "522002",
                "IN",
                true,
                true,
                "37AAAAA0000A1Z5",
                List.of("Modular Kitchen", "Living Room"),
                List.of("Warm Contemporary", "Indian Traditional"),
                List.of("Guntur", "Vijayawada"),
                "+919876543210",
                "+919876543210",
                "contact@" + slug + ".com",
                "https://" + slug + ".com",
                "https://instagram.com/" + slug,
                true,
                true
        );
    }

    @Test
    @DisplayName("Invariant 2: Canonical initial onboarding idempotency - 2 calls produce exactly 1 studio and 1 completion record")
    void testCanonicalInitialOnboardingIdempotency() {
        UserRecord user = createTestUser("Ramesh Kumar", "ramesh@example.com");
        ActorContext actor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        // Call 1: Completes onboarding
        var result1 = onboardingService.completeOnboarding(actor, createValidRequest("Ramesh Studio", "ramesh-studio"), null, null);
        assertNotNull(result1);
        assertEquals("ramesh-studio", result1.studio().slug());
        assertEquals("ACTIVE", result1.studio().status());
        assertEquals("UNPUBLISHED", result1.studio().publicationStatus());

        // Call 2: Second completion attempt for the same user
        var result2 = onboardingService.completeOnboarding(actor, createValidRequest("Ramesh Studio Renamed", "ramesh-studio-2"), null, null);
        assertNotNull(result2);
        // Must return existing studio without error
        assertEquals(result1.studio().id(), result2.studio().id());
        assertEquals("ramesh-studio", result2.studio().slug());
        assertTrue(result2.message().contains("already completed"));

        // Verify database counts
        Integer studioCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_studios WHERE owner_id = ?", Integer.class, user.id());
        Integer slugCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_slug_claims WHERE studio_id = ?", Integer.class, result1.studio().id());
        Integer memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_members WHERE user_id = ?", Integer.class, user.id());
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM identity_user_roles ur JOIN identity_roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.code = 'DESIGNER'", Integer.class, user.id());
        Integer completionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_onboarding_completions WHERE user_id = ?", Integer.class, user.id());

        assertEquals(1, studioCount, "Exactly 1 studio must exist for this owner");
        assertEquals(1, slugCount, "Exactly 1 slug claim must exist");
        assertEquals(1, memberCount, "Exactly 1 studio membership must exist");
        assertEquals(1, roleCount, "Exactly 1 DESIGNER role must exist");
        assertEquals(1, completionCount, "Exactly 1 initial onboarding completion record must exist");

        // Canonical UUIDv7 Invariant Check: Verify that all newly created entities have version 7 UUIDs
        UUID studioId = result1.studio().id();
        assertTrue(UuidV7.isUuidV7(studioId), "Root studio ID must be canonical UUIDv7");
        assertEquals(7, studioId.version());
        assertEquals(2, studioId.variant());

        List<UUID> contactIds = jdbcTemplate.query("SELECT id FROM studio_contacts WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertFalse(contactIds.isEmpty());
        contactIds.forEach(id -> assertTrue(UuidV7.isUuidV7(id), "studio_contacts ID must be canonical UUIDv7"));

        List<UUID> serviceIds = jdbcTemplate.query("SELECT id FROM studio_services WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertFalse(serviceIds.isEmpty());
        serviceIds.forEach(id -> assertTrue(UuidV7.isUuidV7(id), "studio_services ID must be canonical UUIDv7"));

        List<UUID> specialtyIds = jdbcTemplate.query("SELECT id FROM studio_specialties WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertFalse(specialtyIds.isEmpty());
        specialtyIds.forEach(id -> assertTrue(UuidV7.isUuidV7(id), "studio_specialties ID must be canonical UUIDv7"));

        List<UUID> areaIds = jdbcTemplate.query("SELECT id FROM studio_service_areas WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertFalse(areaIds.isEmpty());
        areaIds.forEach(id -> assertTrue(UuidV7.isUuidV7(id), "studio_service_areas ID must be canonical UUIDv7"));

        UUID slugClaimId = jdbcTemplate.queryForObject("SELECT id FROM studio_slug_claims WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertNotNull(slugClaimId);
        assertTrue(UuidV7.isUuidV7(slugClaimId), "studio_slug_claims ID must be canonical UUIDv7");

        UUID memberId = jdbcTemplate.queryForObject("SELECT id FROM studio_members WHERE studio_id = ?",
                (rs, rowNum) -> UUID.fromString(rs.getString("id")), studioId);
        assertNotNull(memberId);
        assertTrue(UuidV7.isUuidV7(memberId), "studio_members ID must be canonical UUIDv7");
    }

    @Test
    @DisplayName("Invariant 3: Concurrency test - 2 near-simultaneous completions produce exactly 1 studio, 0 orphans")
    void testConcurrentOnboardingRequests() throws Exception {
        UserRecord user = createTestUser("Concurrent User", "concurrent@example.com");
        ActorContext actor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger expectedConflictCount = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    onboardingService.completeOnboarding(actor, createValidRequest("Concurrent Studio", "concurrent-studio"), null, null);
                    successCount.incrementAndGet();
                } catch (ConflictException | BadRequestException e) {
                    expectedConflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Database duplicate key constraint exception rolled back cleanly
                    expectedConflictCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Concurrent executions timed out");
        executor.shutdown();

        // Database MUST contain EXACTLY 1 studio, 1 completion record, 1 slug claim, 1 member, 1 DESIGNER role
        Integer studioCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_studios WHERE owner_id = ?", Integer.class, user.id());
        Integer completionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_onboarding_completions WHERE user_id = ?", Integer.class, user.id());
        Integer slugCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_slug_claims WHERE slug = 'concurrent-studio'", Integer.class);
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM identity_user_roles ur JOIN identity_roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.code = 'DESIGNER'", Integer.class, user.id());

        assertEquals(1, studioCount, "Database must have exactly 1 studio");
        assertEquals(1, completionCount, "Database must have exactly 1 completion record");
        assertEquals(1, slugCount, "Database must have exactly 1 slug claim");
        assertEquals(1, roleCount, "Database must have exactly 1 DESIGNER role");
        assertTrue(successCount.get() >= 1, "At least one call must succeed");
    }

    @Test
    @DisplayName("Invariant 4: Transaction rollback test - simulated failure leaves 0 orphan rows across all tables")
    void testTransactionRollbackOnFailure() {
        UserRecord user = createTestUser("Rollback User", "rollback@example.com");
        ActorContext actor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        // Request with invalid specialty that will fail during processing
        OnboardingCompletionRequest failingReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Rollback Studio",
                "rollback-studio",
                "Lead Architect",
                "Tagline",
                2020,
                "1-5",
                "5L-15L",
                "Address",
                "Hyderabad",
                "Hyderabad",
                "Telangana",
                "500001",
                "IN",
                true,
                false,
                null,
                List.of("Modular Kitchen"),
                List.of("INVALID_NONEXISTENT_SPECIALTY"),
                List.of("Hyderabad"),
                "+919876543210",
                null,
                "test@rollback.com",
                null,
                null,
                true,
                true
        );

        assertThrows(BadRequestException.class, () -> onboardingService.completeOnboarding(actor, failingReq, null, null));

        // Verify 0 rows in any table for this studio / user
        Integer studioCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_studios WHERE owner_id = ?", Integer.class, user.id());
        Integer slugCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_slug_claims WHERE slug = 'rollback-studio'", Integer.class);
        Integer contactCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_contacts", Integer.class);
        Integer serviceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_services", Integer.class);
        Integer specialtyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_specialties", Integer.class);
        Integer areaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_service_areas", Integer.class);
        Integer memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM studio_members WHERE user_id = ?", Integer.class, user.id());
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM identity_user_roles ur JOIN identity_roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.code = 'DESIGNER'", Integer.class, user.id());
        Integer completionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_onboarding_completions WHERE user_id = ?", Integer.class, user.id());

        assertEquals(0, studioCount, "0 studios created");
        assertEquals(0, slugCount, "0 slug claims created");
        assertEquals(0, contactCount, "0 contacts created");
        assertEquals(0, serviceCount, "0 services created");
        assertEquals(0, specialtyCount, "0 specialties created");
        assertEquals(0, areaCount, "0 areas created");
        assertEquals(0, memberCount, "0 studio memberships created");
        assertEquals(0, roleCount, "0 DESIGNER roles granted");
        assertEquals(0, completionCount, "0 completions recorded");
    }

    @Test
    @DisplayName("Invariant 5: Tenant bootstrap & application authorization test - creator has OWNER access; User B is DENIED (403)")
    void testTenantBootstrapAndIsolation() {
        UserRecord userA = createTestUser("User A", "userA@example.com");
        UserRecord userB = createTestUser("User B", "userB@example.com");

        ActorContext actorA = new ActorContext(userA.id(), userA.displayName(), userA.email(), Set.of("CUSTOMER"), null, null, true);
        var result = onboardingService.completeOnboarding(actorA, createValidRequest("Studio Alpha", "studio-alpha"), null, null);
        UUID studioAId = result.studio().id();

        // User A context with newly assigned studio membership
        ActorContext actorAWithStudio = new ActorContext(
                userA.id(), userA.displayName(), userA.email(),
                Set.of("CUSTOMER", "DESIGNER"), studioAId, "OWNER", true
        );

        // User B context (different user, no membership in Studio Alpha)
        ActorContext actorB = new ActorContext(
                userB.id(), userB.displayName(), userB.email(),
                Set.of("CUSTOMER"), null, null, true
        );

        // User A can access Studio Alpha
        assertDoesNotThrow(() -> authorizationService.requireStudioAccess(actorAWithStudio, studioAId));

        // User B MUST be DENIED access to Studio Alpha with AccessDeniedException
        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.requireStudioAccess(actorB, studioAId)
        );
        assertTrue(ex.getMessage().contains("tenant isolation violation"));
    }

    @Test
    @DisplayName("Invariant 6: Mass assignment expansion audit - injected security fields are completely ignored")
    void testMassAssignmentExpansionIgnored() throws Exception {
        UserRecord user = createTestUser("Victim User", "victim@example.com");
        ActorContext actor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        UUID injectedOwnerId = UUID.randomUUID();
        UUID injectedStudioId = UUID.randomUUID();

        String evilJson = String.format("""
            {
                "studioName": "Injected Studio",
                "slug": "injected-studio",
                "professionalType": "INTERIOR_STUDIO",
                "professionalTitle": "Architect",
                "city": "Hyderabad",
                "state": "Telangana",
                "businessPhone": "+919876543210",
                "businessEmail": "injected@example.com",
                "services": ["Modular Kitchen"],
                "specialties": ["Modern Minimalist"],
                "confirmedAccuracy": true,
                "confirmedContentOwnership": true,
                "userId": "%s",
                "ownerUserId": "%s",
                "studioId": "%s",
                "tenantId": "%s",
                "role": "ADMIN",
                "roles": ["ADMIN"],
                "publicationStatus": "PUBLISHED",
                "status": "VERIFIED",
                "verified": true,
                "isAdmin": true
            }
            """, injectedOwnerId, injectedOwnerId, injectedStudioId, injectedStudioId);

        OnboardingCompletionRequest deserializedReq = objectMapper.readValue(evilJson, OnboardingCompletionRequest.class);

        var result = onboardingService.completeOnboarding(actor, deserializedReq, null, null);

        // Verify server-enforced invariants
        assertNotEquals(injectedStudioId, result.studio().id(), "Studio ID must be server-generated, not client-injected");
        assertEquals("ACTIVE", result.studio().status(), "Operational status must be ACTIVE");
        assertEquals("UNPUBLISHED", result.studio().publicationStatus(), "Publication status must be UNPUBLISHED");
        assertEquals("OWNER", result.studio().role(), "Studio role must be OWNER");

        // Verify DB records
        var studio = studioRepository.findStudioById(result.studio().id()).orElseThrow();
        assertEquals(user.id(), studio.ownerId(), "Owner MUST be authenticated actor, NOT injected ownerUserId");
        assertNotEquals(injectedOwnerId, studio.ownerId());
        assertEquals("UNPUBLISHED", studio.publicationStatus());

        // Platform roles in DB must NOT include ADMIN
        Set<String> roles = securityRepository.getUserRoles(user.id());
        assertTrue(roles.contains("DESIGNER"), "DESIGNER role granted");
        assertFalse(roles.contains("ADMIN"), "ADMIN role must NOT be granted");
    }

    @Test
    @DisplayName("Invariant 7: Session promotion proof - old session revoked, new session valid with DESIGNER role and CSRF updated")
    void testSessionPromotionProof() {
        UserRecord user = createTestUser("Session User", "session@example.com");
        ActorContext actor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        MockHttpServletResponse initialResponse = new MockHttpServletResponse();
        var sessionCreation = sessionSecurityService.createAndPersistSession(user.id(), "PASSWORD", "Test Device", initialResponse);
        String oldRawToken = sessionCreation.rawSessionToken();
        var oldSessionRecord = sessionCreation.sessionRecord();

        // Validate session BEFORE onboarding: roles = [CUSTOMER], memberships = []
        var validSessionBefore = sessionSecurityService.validateSession(oldRawToken);
        assertTrue(validSessionBefore.isPresent());
        assertEquals(Set.of("CUSTOMER"), validSessionBefore.get().roles());
        assertTrue(validSessionBefore.get().studioMemberships().isEmpty());

        // Mock HTTP request holding the active session in SecurityInterceptor.SESSION_ATTRIBUTE
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.SESSION_ATTRIBUTE, oldSessionRecord);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Complete onboarding
        var result = onboardingService.completeOnboarding(actor, createValidRequest("Session Studio", "session-studio"), request, response);

        assertNotNull(result.newCsrfToken(), "New CSRF token must be returned on session rotation");

        // 1. Old session MUST now be invalidated (revoked in DB)
        var validOldSessionAfter = sessionSecurityService.validateSession(oldRawToken);
        assertTrue(validOldSessionAfter.isEmpty(), "Old session MUST be rejected after rotation");

        // 2. Extract new session token from Set-Cookie header
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("__Host-session="));
        String newRawToken = setCookieHeader.split("__Host-session=")[1].split(";")[0];

        // 3. Validate new session: MUST reflect DESIGNER role and studio OWNER membership
        var validNewSession = sessionSecurityService.validateSession(newRawToken);
        assertTrue(validNewSession.isPresent(), "New session MUST be valid");
        assertTrue(validNewSession.get().roles().contains("DESIGNER"), "New session must contain DESIGNER role");
        assertEquals(1, validNewSession.get().studioMemberships().size(), "New session must contain studio membership");
        assertEquals("OWNER", validNewSession.get().studioMemberships().get(0).role(), "Studio membership role must be OWNER");
        assertEquals(result.studio().id(), validNewSession.get().studioMemberships().get(0).studioId());
    }

    @Test
    @DisplayName("Invariant 13: Slug claim invariants - duplicate slug rejected with ConflictException")
    void testDuplicateSlugRejected() {
        UserRecord user1 = createTestUser("User 1", "user1@example.com");
        UserRecord user2 = createTestUser("User 2", "user2@example.com");

        ActorContext actor1 = new ActorContext(user1.id(), user1.displayName(), user1.email(), Set.of("CUSTOMER"), null, null, true);
        ActorContext actor2 = new ActorContext(user2.id(), user2.displayName(), user2.email(), Set.of("CUSTOMER"), null, null, true);

        // User 1 claims slug 'elite-interiors'
        onboardingService.completeOnboarding(actor1, createValidRequest("Elite Interiors", "elite-interiors"), null, null);

        // User 2 attempts to claim the same slug -> ConflictException (409)
        assertThrows(ConflictException.class, () ->
                onboardingService.completeOnboarding(actor2, createValidRequest("Another Elite", "elite-interiors"), null, null)
        );
    }

    @Test
    @DisplayName("Phase 08.2: Reserved slug validation operates globally without requiring studio/tenant context")
    void testReservedSlugValidationGlobalAccess() {
        // Validation for reserved slugs must succeed in rejecting without tenant context
        var reservedCheck = slugValidationService.checkAvailability("admin");
        assertFalse(reservedCheck.available(), "Reserved slug 'admin' must not be available");
        assertTrue(reservedCheck.reason().contains("reserved"), "Must indicate keyword is reserved");
        assertEquals("admin-studio", reservedCheck.suggestedSlug());

        var availableCheck = slugValidationService.checkAvailability("custom-unique-studio");
        assertTrue(availableCheck.available(), "Unique slug must be available globally");
    }

    @Test
    @DisplayName("Phase 08.2: designer_onboarding_completions is strictly user-scoped")
    void testOnboardingCompletionIsStrictlyUserScoped() {
        UserRecord userA = createTestUser("User Alpha", "alpha@example.com");
        UserRecord userB = createTestUser("User Beta", "beta@example.com");

        ActorContext actorA = new ActorContext(userA.id(), userA.displayName(), userA.email(), Set.of("CUSTOMER"), null, null, true);

        // Initially neither user has completed onboarding
        assertFalse(studioRepository.hasCompletedOnboarding(userA.id()));
        assertFalse(studioRepository.hasCompletedOnboarding(userB.id()));
        assertTrue(studioRepository.findInitialOnboardingStudioId(userA.id()).isEmpty());
        assertTrue(studioRepository.findInitialOnboardingStudioId(userB.id()).isEmpty());

        // User A completes onboarding
        var resultA = onboardingService.completeOnboarding(actorA, createValidRequest("Alpha Studio", "alpha-studio"), null, null);
        UUID studioAId = resultA.studio().id();

        // User A completion is recorded
        assertTrue(studioRepository.hasCompletedOnboarding(userA.id()));
        assertEquals(Optional.of(studioAId), studioRepository.findInitialOnboardingStudioId(userA.id()));

        // User B's state remains completely uncompleted and unaffected
        assertFalse(studioRepository.hasCompletedOnboarding(userB.id()), "User B must not be marked as completed");
        assertTrue(studioRepository.findInitialOnboardingStudioId(userB.id()).isEmpty(), "User B must have no initial studio ID");

        // Database completion table verification
        Integer countA = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_onboarding_completions WHERE user_id = ?", Integer.class, userA.id());
        Integer countB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM designer_onboarding_completions WHERE user_id = ?", Integer.class, userB.id());
        assertEquals(1, countA);
        assertEquals(0, countB);
    }
}

