package com.interior.platform.workspace.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.workspace.dto.WorkspaceBusinessProfileResponse;
import com.interior.platform.workspace.dto.WorkspaceSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StudioRepository studioRepository;

    private AuthorizationService authorizationService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        workspaceService = new WorkspaceService(securityRepository, studioRepository, authorizationService);
    }

    private UserRecord createMockUser(UUID userId, String status) {
        return new UserRecord(
                userId,
                "Suresh Architect",
                "suresh@example.com",
                "+919876543210",
                status,
                Instant.now(),
                Instant.now(),
                0L
        );
    }

    private StudioDetailRecord createMockStudio(UUID studioId, UUID ownerId) {
        return new StudioDetailRecord(
                studioId,
                "Suresh Design Studio",
                "suresh-design",
                ownerId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Principal Architect",
                "Soulful living spaces",
                2018,
                "5-10",
                "15L-35L",
                "123 Lake Road",
                "Hyderabad",
                "Hyderabad",
                "Telangana",
                "500034",
                "IN",
                true,
                true,
                "36AAAAA0000A1Z5",
                "UNPUBLISHED",
                Instant.now().minusSeconds(86400),
                Instant.now().minusSeconds(86400),
                Instant.now(),
                List.of(
                        new StudioDetailRecord.StudioContactItem("PHONE", "+919876543210", true, 1),
                        new StudioDetailRecord.StudioContactItem("EMAIL", "contact@suresh.com", false, 2)
                ),
                List.of(
                        new StudioDetailRecord.StudioServiceItem("MODULAR_KITCHEN", "Modular Kitchen"),
                        new StudioDetailRecord.StudioServiceItem("FULL_HOME", "Full Home Interior")
                ),
                List.of(
                        new StudioDetailRecord.StudioSpecialtyItem("WARM_CONTEMPORARY", "Warm Contemporary"),
                        new StudioDetailRecord.StudioSpecialtyItem("MODERN_MINIMALIST", "Modern Minimalist")
                ),
                List.of(
                        new StudioDetailRecord.StudioServiceAreaItem("Hyderabad", "Banjara Hills"),
                        new StudioDetailRecord.StudioServiceAreaItem("Secunderabad", null)
                )
        );
    }

    @Test
    @DisplayName("1. Authenticated Designer Owner retrieves valid workspace summary")
    void testDesignerOwnerRetrievesWorkspaceSummary() {
        UUID userId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        UserRecord user = createMockUser(userId, "ACTIVE");
        StudioDetailRecord studio = createMockStudio(studioId, userId);

        StudioMemberRecord membership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Suresh Design Studio", "suresh-design", userId, "OWNER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                userId, user.displayName(), user.email(), Set.of("DESIGNER", "CUSTOMER"), studioId, "OWNER", true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(membership));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studio));

        WorkspaceSummaryResponse summary = workspaceService.getWorkspaceSummary(actor, null);

        assertNotNull(summary);
        assertEquals("Suresh Architect", summary.user().displayName());
        assertEquals("suresh@example.com", summary.user().email());
        assertEquals(studioId, summary.studio().id());
        assertEquals("suresh-design", summary.studio().slug());
        assertEquals("OWNER", summary.studio().role());
        assertEquals("ACTIVE", summary.studio().operationalStatus());
        assertEquals("UNPUBLISHED", summary.studio().publicationStatus());
        assertEquals(2, summary.studio().serviceCount());
        assertEquals(2, summary.studio().specialtyCount());
        assertEquals(2, summary.studio().serviceAreaCount());
        assertEquals(2, summary.studio().contactCount());

        // Completeness checks
        assertEquals(100, summary.completeness().profileCompletenessPercentage());
        assertEquals(50, summary.completeness().platformReadinessPercentage());
        assertEquals("PROFILE_COMPLETED", summary.completeness().status());

        // Setup checklist checks
        assertEquals(8, summary.setupChecklist().size());
        assertTrue(summary.setupChecklist().get(0).completed(), "Registration complete");
        assertFalse(summary.setupChecklist().get(6).completed(), "Portfolio setup pending");
        assertFalse(summary.setupChecklist().get(7).completed(), "Projects setup pending");

        // Module readiness checks: verify truthful semantics
        assertEquals(9, summary.modules().size());
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("portfolio") && m.status().equals("NOT_CONFIGURED")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("business") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("media") && m.status().equals("READY")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("ai") && m.status().equals("COMING_SOON")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("leads") && m.status().equals("COMING_SOON")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("analytics") && m.status().equals("COMING_SOON")));
        assertTrue(summary.modules().stream().anyMatch(m -> m.id().equals("notifications") && m.status().equals("COMING_SOON")));

        // Activity feed check: derived from studio timestamps, NOT raw audit records, with user-friendly copy
        assertFalse(summary.activityFeed().isEmpty());
        assertEquals("ONBOARDING", summary.activityFeed().get(0).type());
        assertTrue(summary.activityFeed().get(0).description().contains("Professional workspace enabled"));
    }

    @Test
    @DisplayName("2. DESIGNER_TEAM member accesses workspace strictly via studio membership without initial onboarding fallback")
    void testDesignerTeamMemberAccessViaMembership() {
        UUID teamUserId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        UserRecord user = createMockUser(teamUserId, "ACTIVE");
        StudioDetailRecord studio = createMockStudio(studioId, UuidV7.randomUuid()); // Owner is different user

        StudioMemberRecord membership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Suresh Design Studio", "suresh-design", teamUserId, "MEMBER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                teamUserId, user.displayName(), user.email(), Set.of("DESIGNER_TEAM"), studioId, "MEMBER", true
        );

        when(securityRepository.findUserById(teamUserId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(teamUserId)).thenReturn(List.of(membership));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studio));

        WorkspaceSummaryResponse summary = workspaceService.getWorkspaceSummary(actor, null);

        assertNotNull(summary);
        assertEquals(studioId, summary.studio().id());
        assertEquals("MEMBER", summary.studio().role());

        // Verify that findInitialOnboardingStudioId was NEVER called
        verify(studioRepository, never()).findInitialOnboardingStudioId(any());
    }

    @Test
    @DisplayName("3. CUSTOMER role without onboarding is denied access with onboarding guidance")
    void testCustomerRoleDeniedAccess() {
        UUID userId = UuidV7.randomUuid();
        UserRecord user = createMockUser(userId, "ACTIVE");

        ActorContext actor = new ActorContext(
                userId, user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                workspaceService.getWorkspaceSummary(actor, null)
        );
        assertTrue(ex.getMessage().contains("Professional onboarding required"));
    }

    @Test
    @DisplayName("4. Anonymous actor is denied with 401 Unauthorized")
    void testAnonymousActorDenied() {
        ActorContext actor = ActorContext.anonymous();

        assertThrows(UnauthorizedException.class, () ->
                workspaceService.getWorkspaceSummary(actor, null)
        );
    }

    @Test
    @DisplayName("5. Suspended user is rejected with AccessDeniedException")
    void testSuspendedUserRejected() {
        UUID userId = UuidV7.randomUuid();
        UserRecord suspendedUser = createMockUser(userId, "SUSPENDED");

        ActorContext actor = new ActorContext(
                userId, suspendedUser.displayName(), suspendedUser.email(), Set.of("DESIGNER"), null, null, true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(suspendedUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                workspaceService.getWorkspaceSummary(actor, null)
        );
        assertTrue(ex.getMessage().contains("suspended or inactive"));
    }

    @Test
    @DisplayName("6. Multi-studio user switches workspace safely, while unowned studio is rejected")
    void testMultiStudioSwitchingAndBoundaryEnforcement() {
        UUID userId = UuidV7.randomUuid();
        UUID studio1Id = UuidV7.randomUuid();
        UUID studio2Id = UuidV7.randomUuid();
        UUID unauthorizedStudioId = UuidV7.randomUuid();

        UserRecord user = createMockUser(userId, "ACTIVE");
        StudioDetailRecord studio2 = createMockStudio(studio2Id, userId);

        StudioMemberRecord mem1 = new StudioMemberRecord(
                UuidV7.randomUuid(), studio1Id, "Studio One", "studio-1", userId, "OWNER", Instant.now()
        );
        StudioMemberRecord mem2 = new StudioMemberRecord(
                UuidV7.randomUuid(), studio2Id, "Studio Two", "studio-2", userId, "MEMBER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                userId, user.displayName(), user.email(), Set.of("DESIGNER"), studio1Id, "OWNER", true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(mem1, mem2));
        when(studioRepository.findStudioById(studio2Id)).thenReturn(Optional.of(studio2));

        // 1. Explicitly requested studio 2 succeeds because user is a member
        WorkspaceSummaryResponse summary = workspaceService.getWorkspaceSummary(actor, studio2Id);
        assertEquals(studio2Id, summary.studio().id());
        assertEquals("MEMBER", summary.studio().role());
        assertEquals(2, summary.availableStudios().size());

        // 2. Explicitly requested unauthorized studio fails with 403
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                workspaceService.getWorkspaceSummary(actor, unauthorizedStudioId)
        );
        assertTrue(ex.getMessage().contains("not a member"));
    }

    @Test
    @DisplayName("7. Completeness calculations: missing dimensions lower score, optional fields do not penalize")
    void testCompletenessCalculations() {
        UUID studioId = UuidV7.randomUuid();
        UUID ownerId = UuidV7.randomUuid();

        // Studio with missing location coverage and missing services
        StudioDetailRecord partialStudio = new StudioDetailRecord(
                studioId, "Minimal Studio", "min-studio", ownerId, "ACTIVE",
                "INTERIOR_STUDIO", "Architect", null,
                null, null, null, null, // Optional fields null
                "Hyderabad", null, "Telangana", null, "IN",
                false, false, null, "UNPUBLISHED", null, Instant.now(), Instant.now(),
                List.of(new StudioDetailRecord.StudioContactItem("PHONE", "999", true, 1)),
                List.of(), // 0 services -> 0/20
                List.of(new StudioDetailRecord.StudioSpecialtyItem("MODERN", "Modern")), // 20/20
                List.of()  // 0 service areas -> 10/20 for location
        );

        var completeness = workspaceService.calculateCompleteness(partialStudio);

        // Identity = 20, Location = 10, Services = 0, Specialties = 20, Contacts = 20 -> Total 70%
        assertEquals(70, completeness.profileCompletenessPercentage());
        assertEquals(35, completeness.platformReadinessPercentage());
        assertEquals("IN_PROGRESS", completeness.status());
    }

    @Test
    @DisplayName("8. Business Profile endpoint distinguishes public and private contact visibility and includes GSTIN")
    void testBusinessProfileContactPrivacy() {
        UUID userId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        UserRecord user = createMockUser(userId, "ACTIVE");
        StudioDetailRecord studio = createMockStudio(studioId, userId);

        StudioMemberRecord membership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Suresh Design Studio", "suresh-design", userId, "OWNER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                userId, user.displayName(), user.email(), Set.of("DESIGNER"), studioId, "OWNER", true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(membership));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studio));

        WorkspaceBusinessProfileResponse profile = workspaceService.getBusinessProfile(actor, null);

        assertNotNull(profile);
        assertEquals("36AAAAA0000A1Z5", profile.gstNumber());
        assertEquals(2, profile.contacts().size());

        var phoneContact = profile.contacts().stream().filter(c -> c.kind().equals("PHONE")).findFirst().orElseThrow();
        assertTrue(phoneContact.publicConsent());
        assertEquals("Public when portfolio published", phoneContact.visibilityLabel());

        var emailContact = profile.contacts().stream().filter(c -> c.kind().equals("EMAIL")).findFirst().orElseThrow();
        assertFalse(emailContact.publicConsent());
        assertEquals("Private / Internal only", emailContact.visibilityLabel());
    }

    @Test
    @DisplayName("9. GSTIN is hidden from DESIGNER_TEAM non-owner members in Business Profile")
    void testBusinessProfileHidesGstNumberFromNonOwnerDesignerTeamMember() {
        UUID teamUserId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        UserRecord user = createMockUser(teamUserId, "ACTIVE");
        StudioDetailRecord studio = createMockStudio(studioId, UuidV7.randomUuid());

        StudioMemberRecord membership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Suresh Design Studio", "suresh-design", teamUserId, "MEMBER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                teamUserId, user.displayName(), user.email(), Set.of("DESIGNER_TEAM"), studioId, "MEMBER", true
        );

        when(securityRepository.findUserById(teamUserId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(teamUserId)).thenReturn(List.of(membership));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studio));

        WorkspaceBusinessProfileResponse profile = workspaceService.getBusinessProfile(actor, null);

        assertNotNull(profile);
        // Non-owner team member must NOT see GSTIN
        assertNull(profile.gstNumber(), "GSTIN must be null for non-owner members");
        assertEquals("MEMBER", profile.roleInStudio());
    }

    @Test
    @DisplayName("10. Module readiness semantics: unbuilt modules are COMING_SOON, business is READY, portfolio is NOT_CONFIGURED")
    void testModuleReadinessSemantics() {
        UUID userId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        UserRecord user = createMockUser(userId, "ACTIVE");
        StudioDetailRecord studio = createMockStudio(studioId, userId);

        StudioMemberRecord membership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Suresh Design Studio", "suresh-design", userId, "OWNER", Instant.now()
        );

        ActorContext actor = new ActorContext(
                userId, user.displayName(), user.email(), Set.of("DESIGNER"), studioId, "OWNER", true
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(membership));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studio));

        WorkspaceSummaryResponse summary = workspaceService.getWorkspaceSummary(actor, null);

        var moduleMap = summary.modules().stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkspaceSummaryResponse.ModuleReadinessDto::id,
                        WorkspaceSummaryResponse.ModuleReadinessDto::status
                ));

        assertEquals("READY", moduleMap.get("business"), "Business profile is implemented and usable");
        assertEquals("NOT_CONFIGURED", moduleMap.get("portfolio"), "Portfolio is not yet configured for this studio");
        assertEquals("READY", moduleMap.get("projects"), "Project CMS is now implemented in Phase 18");
        assertEquals("READY", moduleMap.get("media"), "Media engine is now implemented in Phase 19");
        assertEquals("COMING_SOON", moduleMap.get("ai"), "AI visualizer is not yet built");
        assertEquals("COMING_SOON", moduleMap.get("leads"), "Leads/CRM is not yet built");
        assertEquals("COMING_SOON", moduleMap.get("seo"), "SEO center is not yet built");
        assertEquals("COMING_SOON", moduleMap.get("analytics"), "Analytics engine is not yet built");
        assertEquals("COMING_SOON", moduleMap.get("notifications"), "Notification product model is not yet built");
    }
}
