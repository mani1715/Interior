package com.interior.platform.workspace.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.workspace.dto.CreateStudioRequest;
import com.interior.platform.workspace.dto.CreateStudioResponse;
import com.interior.platform.workspace.dto.WorkspaceBusinessProfileResponse;
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
class WorkspaceIntegrationTest {

    @Autowired
    private WorkspaceController workspaceController;

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
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
        UUID userId = UuidV7.randomUuid();
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
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());
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
    @DisplayName("Workspace End-to-End: Onboarded designer fetches summary and business profile with private cache headers")
    void testWorkspaceSummaryAndBusinessProfileEndToEnd() {
        UserRecord user = createTestUser("Maneesh Rao", "maneesh@example.com");
        ActorContext initialActor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);

        // Complete onboarding -> promotes user to DESIGNER, creates studio and OWNER membership
        var onboardResult = onboardingService.completeOnboarding(initialActor, createValidRequest("Maneesh Interiors", "maneesh-interiors"), null, null);
        UUID studioId = onboardResult.studio().id();

        // Prepare authenticated request with newly promoted actor context
        ActorContext designerActor = new ActorContext(
                user.id(), user.displayName(), user.email(),
                Set.of("DESIGNER", "CUSTOMER"), studioId, "OWNER", true
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, designerActor);

        // 1. Fetch Workspace Summary
        ResponseEntity<WorkspaceSummaryResponse> summaryResp = workspaceController.getWorkspaceSummary(request, null, null);
        assertEquals(200, summaryResp.getStatusCode().value());

        // Assert Cache-Control is strictly private, no-store
        String cacheControl = summaryResp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        assertNotNull(cacheControl);
        assertTrue(cacheControl.contains("private"));
        assertTrue(cacheControl.contains("no-store"));

        WorkspaceSummaryResponse summary = summaryResp.getBody();
        assertNotNull(summary);
        assertEquals("Maneesh Rao", summary.user().displayName());
        assertEquals(studioId, summary.studio().id());
        assertEquals("maneesh-interiors", summary.studio().slug());
        assertEquals("ACTIVE", summary.studio().operationalStatus());
        assertEquals("UNPUBLISHED", summary.studio().publicationStatus());
        assertEquals("DESIGNER_ADMIN", summary.studio().role());
        assertEquals(2, summary.studio().serviceCount());
        assertEquals(2, summary.studio().specialtyCount());
        assertEquals(2, summary.studio().serviceAreaCount());
        assertEquals(5, summary.studio().contactCount());

        // Completeness assertions
        assertEquals(100, summary.completeness().profileCompletenessPercentage());
        assertEquals(50, summary.completeness().platformReadinessPercentage());
        assertEquals("PROFILE_COMPLETED", summary.completeness().status());

        // Checklist assertions
        assertEquals(8, summary.setupChecklist().size());
        assertTrue(summary.setupChecklist().stream().anyMatch(i -> i.id().equals("registration") && i.completed()));
        assertTrue(summary.setupChecklist().stream().anyMatch(i -> i.id().equals("identity") && i.completed()));
        assertFalse(summary.setupChecklist().stream().anyMatch(i -> i.id().equals("portfolio") && i.completed()));
        assertFalse(summary.setupChecklist().stream().anyMatch(i -> i.id().equals("projects") && i.completed()));

        // Module readiness assertions
        assertEquals(9, summary.modules().size());
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("portfolio") && m.status().equals("NOT_CONFIGURED")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("projects") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("business") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("media") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("ai") && m.status().equals("NOT_CONFIGURED")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("leads") && m.status().equals("COMING_SOON")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("seo") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("analytics") && m.status().equals("COMING_SOON")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("notifications") && m.status().equals("READY")));

        // 2. Fetch Business Profile
        ResponseEntity<WorkspaceBusinessProfileResponse> profileResp = workspaceController.getBusinessProfile(request, null, null);
        assertEquals(200, profileResp.getStatusCode().value());

        WorkspaceBusinessProfileResponse profile = profileResp.getBody();
        assertNotNull(profile);
        assertEquals("Maneesh Interiors", profile.name());
        assertEquals("37AAAAA0000A1Z5", profile.gstNumber());
        assertEquals(5, profile.contacts().size());
        assertTrue(profile.contacts().stream().anyMatch(c -> c.visibilityLabel().contains("Public")));
    }

    @Test
    @DisplayName("Workspace Security: Bare CUSTOMER without studio membership is blocked from workspace")
    void testCustomerWithoutStudioMembershipBlocked() {
        UserRecord customerUser = createTestUser("Customer Only", "customer@example.com");
        ActorContext customerActor = new ActorContext(customerUser.id(), customerUser.displayName(), customerUser.email(), Set.of("CUSTOMER"), null, null, true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, customerActor);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                workspaceController.getWorkspaceSummary(request, null, null)
        );
        assertTrue(ex.getMessage().contains("Professional onboarding required"));
    }

    @Test
    @DisplayName("Workspace Security: Anonymous request without session is rejected with 401")
    void testAnonymousAccessRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No actor set -> defaults to anonymous

        assertThrows(UnauthorizedException.class, () ->
                workspaceController.getWorkspaceSummary(request, null, null)
        );
    }

    @Test
    @DisplayName("Workspace Security: DESIGNER_TEAM non-owner member cannot view GSTIN in business profile")
    void testNonOwnerTeamMemberCannotViewGstNumber() {
        UserRecord owner = createTestUser("Owner User", "owner@example.com");
        UserRecord teamMember = createTestUser("Team User", "team@example.com");

        ActorContext ownerActor = new ActorContext(owner.id(), owner.displayName(), owner.email(), Set.of("CUSTOMER"), null, null, true);
        var onboardResult = onboardingService.completeOnboarding(ownerActor, createValidRequest("Team Studio", "team-studio"), null, null);
        UUID studioId = onboardResult.studio().id();

        // Add teamMember as DESIGNER_MEMBER (non-admin)
        jdbcTemplate.update(
                "INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, ?, ?)",
                UuidV7.randomUuid(), studioId, teamMember.id(), "DESIGNER_MEMBER", Instant.now()
        );

        ActorContext teamActor = new ActorContext(
                teamMember.id(), teamMember.displayName(), teamMember.email(), Set.of("CUSTOMER"), studioId, "DESIGNER_MEMBER", true
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, teamActor);

        ResponseEntity<WorkspaceBusinessProfileResponse> profileResp = workspaceController.getBusinessProfile(request, null, null);
        assertEquals(200, profileResp.getStatusCode().value());

        WorkspaceBusinessProfileResponse profile = profileResp.getBody();
        assertNotNull(profile);
        assertNull(profile.gstNumber(), "GSTIN must be null for non-owner members");
        assertEquals("DESIGNER_MEMBER", profile.roleInStudio());
    }

    @Test
    @DisplayName("Create Another Studio: Authenticated user creates second studio and can switch between both contexts")
    void testCreateAdditionalStudio_SuccessAndTenantIsolation() {
        UserRecord owner = createTestUser("Multi-Studio User", "multistudio@example.com");
        ActorContext initialActor = new ActorContext(owner.id(), owner.displayName(), owner.email(), Set.of("CUSTOMER"), null, null, true);

        // 1. Initial studio via onboarding
        var onboardResult = onboardingService.completeOnboarding(initialActor, createValidRequest("First Studio", "first-studio"), null, null);
        UUID firstStudioId = onboardResult.studio().id();

        // 2. Create second studio via WorkspaceController
        MockHttpServletRequest createReq = new MockHttpServletRequest();
        createReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, initialActor);

        CreateStudioRequest studio2Req = new CreateStudioRequest(
                "Second Studio Atelier",
                "ARCHITECTURE_STUDIO",
                "Hyderabad",
                "Telangana",
                null
        );

        ResponseEntity<CreateStudioResponse> createdResp = workspaceController.createStudio(studio2Req, createReq);
        assertEquals(201, createdResp.getStatusCode().value());
        assertNotNull(createdResp.getBody());
        UUID secondStudioId = createdResp.getBody().studioId();
        assertNotNull(secondStudioId);
        assertNotEquals(firstStudioId, secondStudioId);
        assertEquals("Second Studio Atelier", createdResp.getBody().name());
        assertEquals("ARCHITECTURE_STUDIO", createdResp.getBody().professionalType());
        assertEquals("DESIGNER_ADMIN", createdResp.getBody().role());

        // 3. User now has memberships in both studios
        var memberships = securityRepository.getStudioMemberships(owner.id());
        assertEquals(2, memberships.size());
        assertTrue(memberships.stream().anyMatch(m -> m.studioId().equals(firstStudioId)));
        assertTrue(memberships.stream().anyMatch(m -> m.studioId().equals(secondStudioId)));

        // 4. Global platform roles were NOT mutated (only contains CUSTOMER and DESIGNER from onboarding, no escalation)
        var globalRoles = securityRepository.getUserRoles(owner.id());
        assertFalse(globalRoles.contains("ADMIN"));
        assertFalse(globalRoles.contains("SUPER_ADMIN"));

        // 5. Query workspace for Studio 2
        ActorContext studio2Actor = new ActorContext(owner.id(), owner.displayName(), owner.email(), globalRoles, secondStudioId, "DESIGNER_ADMIN", true);
        MockHttpServletRequest wsReq2 = new MockHttpServletRequest();
        wsReq2.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio2Actor);

        ResponseEntity<WorkspaceSummaryResponse> summary2 = workspaceController.getWorkspaceSummary(wsReq2, secondStudioId.toString(), null);
        assertEquals(200, summary2.getStatusCode().value());
        assertEquals("Second Studio Atelier", summary2.getBody().studio().name());
        assertEquals(secondStudioId, summary2.getBody().studio().id());
        assertEquals(2, summary2.getBody().availableStudios().size());

        // 6. Query workspace for Studio 1
        ActorContext studio1Actor = new ActorContext(owner.id(), owner.displayName(), owner.email(), globalRoles, firstStudioId, "DESIGNER_ADMIN", true);
        MockHttpServletRequest wsReq1 = new MockHttpServletRequest();
        wsReq1.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio1Actor);

        ResponseEntity<WorkspaceSummaryResponse> summary1 = workspaceController.getWorkspaceSummary(wsReq1, firstStudioId.toString(), null);
        assertEquals(200, summary1.getStatusCode().value());
        assertEquals("First Studio", summary1.getBody().studio().name());
        assertEquals(firstStudioId, summary1.getBody().studio().id());
    }

    @Test
    @DisplayName("Create Another Studio: Validation fails on invalid input")
    void testCreateAdditionalStudio_ValidationFailure() {
        UserRecord owner = createTestUser("Invalid Payload User", "invalid@example.com");
        ActorContext actor = new ActorContext(owner.id(), owner.displayName(), owner.email(), Set.of("CUSTOMER"), null, null, true);

        MockHttpServletRequest createReq = new MockHttpServletRequest();
        createReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        CreateStudioRequest invalidReq = new CreateStudioRequest(
                "",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                null
        );

        assertThrows(Exception.class, () ->
                workspaceController.createStudio(invalidReq, createReq)
        );
    }
}
