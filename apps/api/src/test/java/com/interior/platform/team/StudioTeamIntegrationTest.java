package com.interior.platform.team;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.team.dto.*;
import com.interior.platform.team.web.StudioTeamController;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StudioTeamIntegrationTest {

    @Autowired
    private StudioTeamController teamController;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID studioAId;
    private UUID studioBId;
    private UUID adminAId;
    private UUID memberAId;
    private UUID adminBId;
    private UUID userCandidateId;

    private ActorContext adminAActor;
    private ActorContext memberAActor;
    private ActorContext adminBActor;
    private ActorContext candidateActor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM studio_member_invitations");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test-team.com')");
        jdbcTemplate.execute("DELETE FROM users WHERE email LIKE '%@test-team.com'");

        adminAId = UuidV7.randomUuid();
        memberAId = UuidV7.randomUuid();
        adminBId = UuidV7.randomUuid();
        userCandidateId = UuidV7.randomUuid();

        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();

        Instant now = Instant.now();
        // Seed users
        securityRepository.createUser(new com.interior.platform.security.domain.UserRecord(adminAId, "Admin A", "adminA@test-team.com", "+919876543210", "ACTIVE", now, now, 0L));
        securityRepository.createUser(new com.interior.platform.security.domain.UserRecord(memberAId, "Member A", "memberA@test-team.com", "+919876543211", "ACTIVE", now, now, 0L));
        securityRepository.createUser(new com.interior.platform.security.domain.UserRecord(adminBId, "Admin B", "adminB@test-team.com", "+919876543212", "ACTIVE", now, now, 0L));
        securityRepository.createUser(new com.interior.platform.security.domain.UserRecord(userCandidateId, "Candidate User", "candidate@test-team.com", "+919876543213", "ACTIVE", now, now, 0L));

        // Seed studios
        securityRepository.createStudio(studioAId, "Studio Alpha", "studio-alpha", adminAId, "ACTIVE");
        securityRepository.createStudio(studioBId, "Studio Beta", "studio-beta", adminBId, "ACTIVE");

        // Seed memberships
        securityRepository.addStudioMember(UuidV7.randomUuid(), studioAId, adminAId, "DESIGNER_ADMIN");
        securityRepository.addStudioMember(UuidV7.randomUuid(), studioAId, memberAId, "DESIGNER_MEMBER");
        securityRepository.addStudioMember(UuidV7.randomUuid(), studioBId, adminBId, "DESIGNER_ADMIN");

        adminAActor = new ActorContext(
                adminAId, "Admin A", "adminA@test-team.com",
                Set.of("DESIGNER"), Set.of("studio:read", "studio:write"),
                studioAId, "DESIGNER_ADMIN", "PASSKEY", true
        );

        memberAActor = new ActorContext(
                memberAId, "Member A", "memberA@test-team.com",
                Set.of("DESIGNER_TEAM"), Set.of("project:read"),
                studioAId, "DESIGNER_MEMBER", "PASSKEY", true
        );

        adminBActor = new ActorContext(
                adminBId, "Admin B", "adminB@test-team.com",
                Set.of("DESIGNER"), Set.of("studio:read", "studio:write"),
                studioBId, "DESIGNER_ADMIN", "PASSKEY", true
        );

        candidateActor = new ActorContext(
                userCandidateId, "Candidate User", "candidate@test-team.com",
                Set.of("CUSTOMER"), Set.of(),
                null, null, "PASSWORD", true
        );
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }

    @Test
    @DisplayName("Flow A: Complete team invitation, acceptance, role change flow")
    void testFlowA_Invite_Accept_Promote() {
        MockHttpServletRequest adminReq = createMockRequest(adminAActor);

        // 1. Admin A invites candidate as Member
        CreateInvitationRequest inviteReq = new CreateInvitationRequest("candidate@test-team.com", "MEMBER");
        ResponseEntity<CreateInvitationResponse> inviteRes = teamController.createInvitation(adminReq, null, studioAId, inviteReq);
        assertEquals(HttpStatus.CREATED, inviteRes.getStatusCode());
        assertNotNull(inviteRes.getBody());
        String rawToken = inviteRes.getBody().rawToken();
        assertNotNull(rawToken);

        // 2. Candidate validates invitation
        MockHttpServletRequest pubReq = new MockHttpServletRequest();
        ResponseEntity<ValidateInvitationResponse> valRes = teamController.validateInvitation(pubReq, rawToken);
        assertEquals(HttpStatus.OK, valRes.getStatusCode());
        assertTrue(valRes.getBody().valid());
        assertEquals("Studio Alpha", valRes.getBody().studioName());
        assertEquals("c***e@test-team.com", valRes.getBody().invitedEmail());
        assertEquals("c***e@test-team.com", valRes.getBody().maskedEmail());

        // 2b. Token exchange flow (scrub raw token to ephemeral cookie session)
        ResponseEntity<ExchangeInvitationResponse> exchRes = teamController.exchangeInvitation(
                pubReq, new ExchangeInvitationRequest(rawToken)
        );
        assertEquals(HttpStatus.OK, exchRes.getStatusCode());
        assertTrue(exchRes.getBody().valid());
        assertEquals("c***e@test-team.com", exchRes.getBody().maskedEmail());

        // 3. Candidate accepts invitation
        MockHttpServletRequest candReq = createMockRequest(candidateActor);
        ResponseEntity<Void> acceptRes = teamController.acceptInvitation(candReq, new AcceptInvitationRequest(rawToken));
        assertEquals(HttpStatus.OK, acceptRes.getStatusCode());

        // 4. Verify candidate is now a member of Studio Alpha
        ResponseEntity<TeamOverviewResponse> overviewRes = teamController.getTeamOverview(adminReq, null, studioAId);
        assertEquals(3, overviewRes.getBody().totalMembers());
        TeamMemberDto candMember = overviewRes.getBody().members().stream()
                .filter(m -> m.userId().equals(userCandidateId))
                .findFirst()
                .orElse(null);
        assertNotNull(candMember);
        assertEquals("Team Member", candMember.role());

        // 5. Admin promotes candidate to Admin
        ResponseEntity<Void> roleRes = teamController.updateMemberRole(
                adminReq, candMember.id(), null, studioAId, new UpdateMemberRoleRequest("ADMIN")
        );
        assertEquals(HttpStatus.NO_CONTENT, roleRes.getStatusCode());

        // 6. Verify role updated
        overviewRes = teamController.getTeamOverview(adminReq, null, studioAId);
        TeamMemberDto candUpdated = overviewRes.getBody().members().stream()
                .filter(m -> m.userId().equals(userCandidateId))
                .findFirst()
                .orElseThrow();
        assertEquals("Studio Admin", candUpdated.role());
    }

    @Test
    @DisplayName("Flow B: Invitation revoked cannot be accepted")
    void testFlowB_RevokeInvitation() {
        MockHttpServletRequest adminReq = createMockRequest(adminAActor);

        // 1. Create invitation
        CreateInvitationRequest inviteReq = new CreateInvitationRequest("candidate@test-team.com", "MEMBER");
        CreateInvitationResponse invite = teamController.createInvitation(adminReq, null, studioAId, inviteReq).getBody();

        // 2. Revoke invitation
        ResponseEntity<Void> revokeRes = teamController.revokeInvitation(adminReq, invite.invitationId(), null, studioAId);
        assertEquals(HttpStatus.NO_CONTENT, revokeRes.getStatusCode());

        // 3. Candidate validation shows REVOKED
        MockHttpServletRequest pubReq = new MockHttpServletRequest();
        ValidateInvitationResponse val = teamController.validateInvitation(pubReq, invite.rawToken()).getBody();
        assertFalse(val.valid());
        assertEquals("REVOKED", val.status());

        // 4. Candidate attempt to accept is rejected
        MockHttpServletRequest candReq = createMockRequest(candidateActor);
        assertThrows(BadRequestException.class, () ->
                teamController.acceptInvitation(candReq, new AcceptInvitationRequest(invite.rawToken()))
        );
    }

    @Test
    @DisplayName("Flow C: Member removal removes studio access immediately")
    void testFlowC_MemberRemoval() {
        MockHttpServletRequest adminReq = createMockRequest(adminAActor);

        TeamOverviewResponse overview = teamController.getTeamOverview(adminReq, null, studioAId).getBody();
        TeamMemberDto member = overview.members().stream()
                .filter(m -> m.userId().equals(memberAId))
                .findFirst()
                .orElseThrow();

        // Remove Member A
        ResponseEntity<Void> removeRes = teamController.removeMember(adminReq, member.id(), null, studioAId);
        assertEquals(HttpStatus.NO_CONTENT, removeRes.getStatusCode());

        // Verify Member A is no longer in team
        overview = teamController.getTeamOverview(adminReq, null, studioAId).getBody();
        assertEquals(1, overview.totalMembers());
        assertFalse(overview.members().stream().anyMatch(m -> m.userId().equals(memberAId)));
    }

    @Test
    @DisplayName("Flow D: Sole admin cannot demote, remove, or leave studio")
    void testFlowD_LastAdminLockoutProtection() {
        MockHttpServletRequest adminReq = createMockRequest(adminAActor);

        TeamOverviewResponse overview = teamController.getTeamOverview(adminReq, null, studioAId).getBody();
        TeamMemberDto adminMember = overview.members().stream()
                .filter(m -> m.userId().equals(adminAId))
                .findFirst()
                .orElseThrow();

        // 1. Demoting sole admin is rejected
        assertThrows(BadRequestException.class, () ->
                teamController.updateMemberRole(adminReq, adminMember.id(), null, studioAId, new UpdateMemberRoleRequest("MEMBER"))
        );

        // 2. Removing sole admin is rejected
        assertThrows(BadRequestException.class, () ->
                teamController.removeMember(adminReq, adminMember.id(), null, studioAId)
        );

        // 3. Sole admin leaving studio is rejected
        assertThrows(BadRequestException.class, () ->
                teamController.leaveStudio(adminReq, null, studioAId)
        );
    }

    @Test
    @DisplayName("Cross-studio isolation: Studio A admin cannot access or mutate Studio B team")
    void testCrossStudioIsolation() {
        MockHttpServletRequest adminAReq = createMockRequest(adminAActor);

        // 1. Admin A accessing Studio B overview -> 403
        assertThrows(AccessDeniedException.class, () ->
                teamController.getTeamOverview(adminAReq, null, studioBId)
        );

        // 2. Admin A inviting to Studio B -> 403
        assertThrows(AccessDeniedException.class, () ->
                teamController.createInvitation(adminAReq, null, studioBId, new CreateInvitationRequest("test@test-team.com", "MEMBER"))
        );
    }
}
