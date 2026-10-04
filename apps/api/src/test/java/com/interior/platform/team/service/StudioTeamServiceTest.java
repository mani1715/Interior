package com.interior.platform.team.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.notifications.service.NotificationService;
import com.interior.platform.realtime.service.RealtimeEventPublisher;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import com.interior.platform.team.domain.StudioMemberDetails;
import com.interior.platform.team.domain.StudioMemberInvitationRecord;
import com.interior.platform.team.dto.*;
import com.interior.platform.team.repository.StudioTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudioTeamServiceTest {

    @Mock private StudioTeamRepository teamRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;

    private AuthorizationService authorizationService;
    private RateLimiterService rateLimiterService;
    private StudioTeamService teamService;

    private UUID studioId;
    private UUID adminUserId;
    private UUID memberUserId;
    private ActorContext adminActor;
    private ActorContext memberActor;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        teamService = new StudioTeamService(
                teamRepository,
                authorizationService,
                rateLimiterService,
                auditService,
                notificationService,
                realtimeEventPublisher
        );

        studioId = UuidV7.randomUuid();
        adminUserId = UuidV7.randomUuid();
        memberUserId = UuidV7.randomUuid();

        adminActor = new ActorContext(
                adminUserId,
                "Admin User",
                "admin@studio.com",
                Set.of("DESIGNER"),
                Set.of("studio:read", "studio:write"),
                studioId,
                "OWNER",
                "PASSKEY",
                true
        );

        memberActor = new ActorContext(
                memberUserId,
                "Member User",
                "member@studio.com",
                Set.of("DESIGNER_TEAM"),
                Set.of("project:read", "project:write"),
                studioId,
                "MEMBER",
                "PASSKEY",
                true
        );
    }

    @Test
    @DisplayName("getTeamOverview returns members and pending invitations for admin")
    void testGetTeamOverview_Admin() {
        when(teamRepository.findStudioName(studioId)).thenReturn("Studio Atelier");
        when(teamRepository.countAdmins(studioId)).thenReturn(1);
        when(teamRepository.getStudioMembersWithUserDetails(studioId)).thenReturn(List.of(
                new StudioMemberDetails(UuidV7.randomUuid(), studioId, adminUserId, "Admin User", "admin@studio.com", "OWNER", Instant.now())
        ));
        when(teamRepository.findPendingInvitations(eq(studioId), any())).thenReturn(List.of(
                new StudioMemberInvitationRecord(
                        UuidV7.randomUuid(), studioId, "invitee@example.com", "MEMBER", new byte[32],
                        adminUserId, "PENDING", Instant.now().plus(7, ChronoUnit.DAYS), null, null, null, Instant.now(), Instant.now()
                )
        ));

        TeamOverviewResponse response = teamService.getTeamOverview(adminActor, studioId);

        assertNotNull(response);
        assertEquals("Studio Atelier", response.studioName());
        assertTrue(response.isCurrentUserAdmin());
        assertEquals(1, response.members().size());
        assertEquals(1, response.pendingInvitations().size());
        assertFalse(response.members().get(0).canRemove(), "Sole admin cannot be removed");
    }

    @Test
    @DisplayName("createInvitation generates 256-bit token hash and persists invitation")
    void testCreateInvitation_Success() {
        when(teamRepository.getStudioMembersWithUserDetails(studioId)).thenReturn(List.of());
        when(teamRepository.findPendingInvitationByEmail(eq(studioId), eq("new@example.com"), any()))
                .thenReturn(Optional.empty());

        CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", "MEMBER");
        CreateInvitationResponse response = teamService.createInvitation(adminActor, studioId, request);

        assertNotNull(response);
        assertEquals("new@example.com", response.invitedEmail());
        assertEquals("Member", response.role());
        assertNotNull(response.rawToken());
        assertTrue(response.rawToken().length() >= 64, "Raw token must be at least 256 bits hex");
        assertTrue(response.invitationUrl().contains(response.rawToken()));

        ArgumentCaptor<StudioMemberInvitationRecord> captor = ArgumentCaptor.forClass(StudioMemberInvitationRecord.class);
        verify(teamRepository).saveInvitation(captor.capture());
        StudioMemberInvitationRecord saved = captor.getValue();
        assertEquals(studioId, saved.studioId());
        assertEquals("new@example.com", saved.invitedEmail());
        assertEquals("MEMBER", saved.role());
        assertEquals("PENDING", saved.status());
        assertNotNull(saved.tokenHash());
    }

    @Test
    @DisplayName("createInvitation rejects if email is already an active member")
    void testCreateInvitation_DuplicateMember_ThrowsConflict() {
        when(teamRepository.getStudioMembersWithUserDetails(studioId)).thenReturn(List.of(
                new StudioMemberDetails(UuidV7.randomUuid(), studioId, memberUserId, "Member User", "member@studio.com", "MEMBER", Instant.now())
        ));

        CreateInvitationRequest request = new CreateInvitationRequest("member@studio.com", "MEMBER");
        assertThrows(ConflictException.class, () -> teamService.createInvitation(adminActor, studioId, request));
    }

    @Test
    @DisplayName("createInvitation rejects if caller is non-admin member")
    void testCreateInvitation_NonAdmin_ThrowsAccessDenied() {
        CreateInvitationRequest request = new CreateInvitationRequest("test@example.com", "MEMBER");
        assertThrows(AccessDeniedException.class, () -> teamService.createInvitation(memberActor, studioId, request));
    }

    @Test
    @DisplayName("updateMemberRole succeeds when multiple admins exist")
    void testUpdateMemberRole_Success() {
        UUID membershipId = UuidV7.randomUuid();
        when(teamRepository.findMembershipById(membershipId)).thenReturn(Optional.of(
                new StudioMemberDetails(membershipId, studioId, memberUserId, "Member User", "member@studio.com", "MEMBER", Instant.now())
        ));

        teamService.updateMemberRole(adminActor, studioId, membershipId, new UpdateMemberRoleRequest("ADMIN"));

        verify(teamRepository).updateMemberRole(membershipId, "ADMIN");
        verify(notificationService).dispatchNotification(eq(memberUserId), eq(studioId), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateMemberRole rejects demoting last remaining admin")
    void testUpdateMemberRole_DemoteLastAdmin_ThrowsBadRequest() {
        UUID membershipId = UuidV7.randomUuid();
        when(teamRepository.findMembershipById(membershipId)).thenReturn(Optional.of(
                new StudioMemberDetails(membershipId, studioId, adminUserId, "Admin User", "admin@studio.com", "OWNER", Instant.now())
        ));
        when(teamRepository.countAdmins(studioId)).thenReturn(1);

        assertThrows(BadRequestException.class, () ->
                teamService.updateMemberRole(adminActor, studioId, membershipId, new UpdateMemberRoleRequest("MEMBER"))
        );
    }

    @Test
    @DisplayName("removeMember rejects removing last remaining admin")
    void testRemoveMember_LastAdmin_ThrowsBadRequest() {
        UUID membershipId = UuidV7.randomUuid();
        when(teamRepository.findMembershipById(membershipId)).thenReturn(Optional.of(
                new StudioMemberDetails(membershipId, studioId, adminUserId, "Admin User", "admin@studio.com", "ADMIN", Instant.now())
        ));
        when(teamRepository.countAdmins(studioId)).thenReturn(1);

        assertThrows(BadRequestException.class, () ->
                teamService.removeMember(adminActor, studioId, membershipId)
        );
    }

    @Test
    @DisplayName("leaveStudio rejects when sole admin attempts to leave")
    void testLeaveStudio_SoleAdmin_ThrowsBadRequest() {
        when(teamRepository.findMembershipByStudioAndUser(studioId, adminUserId)).thenReturn(Optional.of(
                new StudioMemberDetails(UuidV7.randomUuid(), studioId, adminUserId, "Admin User", "admin@studio.com", "OWNER", Instant.now())
        ));
        when(teamRepository.countAdmins(studioId)).thenReturn(1);

        assertThrows(BadRequestException.class, () -> teamService.leaveStudio(adminActor, studioId));
    }

    @Test
    @DisplayName("acceptInvitation enforces email matching and creates membership")
    void testAcceptInvitation_Success() {
        UUID inviteId = UuidV7.randomUuid();
        ActorContext inviteeActor = new ActorContext(
                UuidV7.randomUuid(), "Invited Person", "invited@example.com", Set.of("CUSTOMER"), null, null, true
        );

        StudioMemberInvitationRecord inv = new StudioMemberInvitationRecord(
                inviteId, studioId, "invited@example.com", "MEMBER", new byte[32],
                adminUserId, "PENDING", Instant.now().plus(7, ChronoUnit.DAYS), null, null, null, Instant.now(), Instant.now()
        );

        when(teamRepository.findInvitationByTokenHash(any())).thenReturn(Optional.of(inv));
        when(teamRepository.findMembershipByStudioAndUser(studioId, inviteeActor.userId())).thenReturn(Optional.empty());
        when(teamRepository.findStudioName(studioId)).thenReturn("Design Co");

        teamService.acceptInvitation(inviteeActor, new AcceptInvitationRequest("raw-test-token-12345678901234567890"));

        verify(teamRepository).addStudioMember(any(), eq(studioId), eq(inviteeActor.userId()), eq("MEMBER"));
        verify(teamRepository).updateInvitationStatus(eq(inviteId), eq("ACCEPTED"), any(), eq(inviteeActor.userId()));
        verify(teamRepository).assignPlatformRole(eq(inviteeActor.userId()), eq("DESIGNER_TEAM"));
    }

    @Test
    @DisplayName("acceptInvitation rejects when authenticated email does not match invitation")
    void testAcceptInvitation_EmailMismatch_ThrowsAccessDenied() {
        UUID inviteId = UuidV7.randomUuid();
        ActorContext wrongUserActor = new ActorContext(
                UuidV7.randomUuid(), "Wrong Person", "wrong@example.com", Set.of("CUSTOMER"), null, null, true
        );

        StudioMemberInvitationRecord inv = new StudioMemberInvitationRecord(
                inviteId, studioId, "invited@example.com", "MEMBER", new byte[32],
                adminUserId, "PENDING", Instant.now().plus(7, ChronoUnit.DAYS), null, null, null, Instant.now(), Instant.now()
        );

        when(teamRepository.findInvitationByTokenHash(any())).thenReturn(Optional.of(inv));

        assertThrows(AccessDeniedException.class, () ->
                teamService.acceptInvitation(wrongUserActor, new AcceptInvitationRequest("raw-test-token"))
        );
    }
}
