package com.interior.platform.admin;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.repository.AdminRepository;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.media.dto.StorageReconciliationReport;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminSecurityTest {

    private AdminRepository adminRepository;
    private AuthorizationService authorizationService;
    private AuditService auditService;
    private RateLimiterService rateLimiterService;
    private SecurityRepository securityRepository;
    private MediaService mediaService;
    private AiVisualizerService aiVisualizerService;
    private CommunicationDeliveryRepository communicationDeliveryRepository;
    private TransactionalEmailService transactionalEmailService;
    private JdbcTemplate jdbcTemplate;

    private AdminService adminService;

    private ActorContext anonymousActor;
    private ActorContext customerActor;
    private ActorContext studioOwnerActor;
    private ActorContext adminActor;
    private ActorContext superAdminActor;

    @BeforeEach
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        authorizationService = new AuthorizationService();
        auditService = mock(AuditService.class);
        rateLimiterService = new RateLimiterService(Clock.systemUTC());
        securityRepository = mock(SecurityRepository.class);
        mediaService = mock(MediaService.class);
        aiVisualizerService = mock(AiVisualizerService.class);
        communicationDeliveryRepository = mock(CommunicationDeliveryRepository.class);
        transactionalEmailService = mock(TransactionalEmailService.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        adminService = new AdminService(
                adminRepository,
                authorizationService,
                auditService,
                rateLimiterService,
                securityRepository,
                mediaService,
                aiVisualizerService,
                communicationDeliveryRepository,
                transactionalEmailService,
                jdbcTemplate
        );

        anonymousActor = ActorContext.anonymous();

        customerActor = new ActorContext(
                UUID.randomUUID(), "Customer User", "customer@example.com",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        UUID studioId = UUID.randomUUID();
        studioOwnerActor = new ActorContext(
                UUID.randomUUID(), "Designer Owner", "designer@example.com",
                Set.of("DESIGNER"), Set.of(), studioId, "OWNER", "MFA", true
        );

        adminActor = new ActorContext(
                UUID.randomUUID(), "Platform Admin", "admin@platform.local",
                Set.of("ADMIN"), Set.of(), null, null, "MFA", true
        );

        superAdminActor = new ActorContext(
                UUID.randomUUID(), "Super Admin", "superadmin@platform.local",
                Set.of("SUPER_ADMIN"), Set.of(), null, null, "WEBAUTHN", true
        );
    }

    @Test
    @DisplayName("Anonymous users cannot access admin dashboard")
    void anonymousCannotAccessAdmin() {
        assertThrows(UnauthorizedException.class, () -> adminService.getDashboardMetrics(anonymousActor));
    }

    @Test
    @DisplayName("Normal customers cannot access admin dashboard")
    void customerCannotAccessAdmin() {
        assertThrows(AccessDeniedException.class, () -> adminService.getDashboardMetrics(customerActor));
    }

    @Test
    @DisplayName("Studio owners are NOT platform admins and cannot access admin dashboard")
    void studioOwnerCannotAccessAdmin() {
        assertThrows(AccessDeniedException.class, () -> adminService.getDashboardMetrics(studioOwnerActor));
    }

    @Test
    @DisplayName("Platform Admin can access admin dashboard")
    void platformAdminCanAccessDashboard() {
        when(adminRepository.getDashboardMetrics()).thenReturn(new AdminDashboardMetrics(
                10, 9, 1, 8, 50, 48, 2, 30, 25, 100, 3, 2, 200, 150, List.of()
        ));

        AdminDashboardMetrics metrics = adminService.getDashboardMetrics(adminActor);
        assertNotNull(metrics);
        assertEquals(10, metrics.totalStudios());
        assertEquals(50, metrics.totalUsers());
    }

    @Test
    @DisplayName("Super Admin can access admin dashboard")
    void superAdminCanAccessDashboard() {
        when(adminRepository.getDashboardMetrics()).thenReturn(new AdminDashboardMetrics(
                5, 5, 0, 4, 20, 20, 0, 10, 10, 30, 1, 0, 50, 20, List.of()
        ));

        AdminDashboardMetrics metrics = adminService.getDashboardMetrics(superAdminActor);
        assertNotNull(metrics);
        assertEquals(5, metrics.totalStudios());
    }

    @Test
    @DisplayName("Admin can suspend user, invalidate sessions, and record audit event")
    void adminCanSuspendUserWithAudit() {
        UUID targetUserId = UUID.randomUUID();
        when(adminRepository.getUserDetail(targetUserId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetUserId, "Normal User", "user@test.local", null, "ACTIVE", null, Instant.now(), Set.of("CUSTOMER"), List.of(), 1
        )));

        UpdateUserStatusRequest req = new UpdateUserStatusRequest("SUSPENDED", "Policy violation");

        adminService.updateUserStatus(adminActor, targetUserId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateUserStatus(targetUserId, "SUSPENDED", "Policy violation");
        verify(securityRepository).revokeAllUserSessions(eq(targetUserId), any(Instant.class));
        verify(auditService).record(
                eq(adminActor.userId()),
                isNull(),
                eq("USER_STATUS_CHANGED"),
                eq("USER"),
                eq(targetUserId.toString()),
                argThat(map -> "SUSPENDED".equals(map.get("newStatus")) && "Policy violation".equals(map.get("reason"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Admin cannot suspend self")
    void adminCannotSuspendSelf() {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest("SUSPENDED", "Self suspension attempt");
        when(adminRepository.getUserDetail(adminActor.userId())).thenReturn(Optional.of(new AdminUserDetailDto(
                adminActor.userId(), "Admin User", "admin@platform.local", null, "ACTIVE", null, Instant.now(), Set.of("ADMIN"), List.of(), 1
        )));

        assertThrows(IllegalArgumentException.class, () ->
                adminService.updateUserStatus(adminActor, adminActor.userId(), req, "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Normal Admin cannot suspend Super Admin")
    void normalAdminCannotSuspendSuperAdmin() {
        UUID targetId = superAdminActor.userId();
        when(adminRepository.getUserDetail(targetId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetId, "Super Admin", "super@platform.local", null, "ACTIVE", null, Instant.now(), Set.of("SUPER_ADMIN"), List.of(), 1
        )));

        assertThrows(AccessDeniedException.class, () ->
                adminService.updateUserStatus(adminActor, targetId, new UpdateUserStatusRequest("SUSPENDED", "Reason"), "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Super Admin cannot suspend the final active Super Admin (last admin lockout defense)")
    void cannotSuspendLastSuperAdmin() {
        UUID targetId = UUID.randomUUID();
        when(adminRepository.getUserDetail(targetId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetId, "Super Admin 1", "super1@platform.local", null, "ACTIVE", null, Instant.now(), Set.of("SUPER_ADMIN"), List.of(), 1
        )));
        when(adminRepository.countActiveSuperAdmins()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
                adminService.updateUserStatus(superAdminActor, targetId, new UpdateUserStatusRequest("SUSPENDED", "Reason"), "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Admin can suspend studio and audit event is recorded")
    void adminCanSuspendStudioWithAudit() {
        UUID targetStudioId = UUID.randomUUID();
        when(adminRepository.getStudioDetail(targetStudioId)).thenReturn(Optional.of(new AdminStudioDetailDto(
                targetStudioId, "Studio A", "studio-a", UUID.randomUUID(), "owner@a.local", "ACTIVE", null, "PUBLISHED", "VERIFIED", "STANDARD", 2, 2, 10, 5000000L, 0, 0, Instant.now()
        )));

        UpdateStudioStatusRequest req = new UpdateStudioStatusRequest("SUSPENDED", "Fraud investigation");

        adminService.updateStudioStatus(adminActor, targetStudioId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateStudioStatus(targetStudioId, "SUSPENDED", "Fraud investigation");
        verify(auditService).record(
                eq(adminActor.userId()),
                eq(targetStudioId),
                eq("STUDIO_STATUS_CHANGED"),
                eq("STUDIO"),
                eq(targetStudioId.toString()),
                argThat(map -> "SUSPENDED".equals(map.get("newStatus")) && "Fraud investigation".equals(map.get("reason"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Super Admin can update user role, revoking sessions with audit")
    void superAdminCanUpdateUserRole() {
        UUID targetUserId = UUID.randomUUID();
        when(adminRepository.getUserDetail(targetUserId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetUserId, "User B", "b@test.local", null, "ACTIVE", null, Instant.now(), Set.of("DESIGNER"), List.of(), 1
        )));

        UpdateUserRoleRequest req = new UpdateUserRoleRequest("ADMIN");
        adminService.updateUserRole(superAdminActor, targetUserId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateUserRole(targetUserId, "ADMIN");
        verify(securityRepository).revokeAllUserSessions(eq(targetUserId), any(Instant.class));
        verify(auditService).record(
                eq(superAdminActor.userId()),
                isNull(),
                eq("ADMIN_ROLE_CHANGED"),
                eq("USER"),
                eq(targetUserId.toString()),
                argThat(map -> "ADMIN".equals(map.get("newRole"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Normal Admin cannot promote user to Super Admin (self-escalation or peer escalation blocked)")
    void normalAdminCannotModifyRoles() {
        UUID targetUserId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                adminService.updateUserRole(adminActor, targetUserId, new UpdateUserRoleRequest("SUPER_ADMIN"), "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Super Admin cannot modify own role (self-demotion or self-change blocked)")
    void superAdminCannotModifyOwnRole() {
        assertThrows(IllegalArgumentException.class, () ->
                adminService.updateUserRole(superAdminActor, superAdminActor.userId(), new UpdateUserRoleRequest("ADMIN"), "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Super Admin cannot demote last active Super Admin")
    void cannotDemoteLastSuperAdmin() {
        UUID targetUserId = UUID.randomUUID();
        when(adminRepository.getUserDetail(targetUserId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetUserId, "Sole Super Admin", "sole@test.local", null, "ACTIVE", null, Instant.now(), Set.of("SUPER_ADMIN"), List.of(), 1
        )));
        when(adminRepository.countActiveSuperAdmins()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
                adminService.updateUserRole(superAdminActor, targetUserId, new UpdateUserRoleRequest("ADMIN"), "127.0.0.1", "Agent"));
    }

    @Test
    @DisplayName("Super Admin can revoke user sessions with audit")
    void superAdminCanRevokeUserSessions() {
        UUID targetUserId = UUID.randomUUID();
        when(adminRepository.getUserDetail(targetUserId)).thenReturn(Optional.of(new AdminUserDetailDto(
                targetUserId, "User C", "c@test.local", null, "ACTIVE", null, Instant.now(), Set.of("CUSTOMER"), List.of(), 2
        )));

        adminService.revokeUserSessions(superAdminActor, targetUserId, "127.0.0.1", "TestAgent");

        verify(securityRepository).revokeAllUserSessions(eq(targetUserId), any(Instant.class));
        verify(auditService).record(
                eq(superAdminActor.userId()),
                isNull(),
                eq("SESSION_REVOKED"),
                eq("USER"),
                eq(targetUserId.toString()),
                anyMap(),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Admin can moderate review with reason and audit")
    void adminCanModerateReviewWithAudit() {
        UUID targetReviewId = UUID.randomUUID();
        ModerateReviewRequest req = new ModerateReviewRequest("REMOVED", "Contains offensive language");

        adminService.moderateReview(adminActor, targetReviewId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateReviewStatus(targetReviewId, "REMOVED");
        verify(auditService).record(
                eq(adminActor.userId()),
                isNull(),
                eq("REVIEW_MODERATION"),
                eq("REVIEW"),
                eq(targetReviewId.toString()),
                argThat(map -> "REMOVED".equals(map.get("newStatus")) && "Contains offensive language".equals(map.get("reason"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Admin removing a review must provide a reason")
    void removeReviewRequiresReason() {
        UUID targetReviewId = UUID.randomUUID();
        ModerateReviewRequest req = new ModerateReviewRequest("REMOVED", "   ");

        assertThrows(IllegalArgumentException.class, () ->
                adminService.moderateReview(adminActor, targetReviewId, req, "127.0.0.1", "TestAgent"));
    }

    @Test
    @DisplayName("Admin can moderate project status and audit event is recorded")
    void adminCanModerateProjectWithAudit() {
        UUID targetProjectId = UUID.randomUUID();
        ModerateProjectRequest req = new ModerateProjectRequest("HIDDEN", "Copyright claim received");

        adminService.moderateProject(adminActor, targetProjectId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateProjectModerationStatus(targetProjectId, "HIDDEN", "Copyright claim received");
        verify(auditService).record(
                eq(adminActor.userId()),
                isNull(),
                eq("PROJECT_MODERATION"),
                eq("STUDIO_PROJECT"),
                eq(targetProjectId.toString()),
                argThat(map -> "HIDDEN".equals(map.get("newStatus"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Admin cannot retry delivery when email provider is NOT_CONFIGURED")
    void cannotRetryWhenEmailProviderUnconfigured() {
        UUID deliveryId = UUID.randomUUID();
        when(communicationDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(new CommunicationDeliveryRecord(
                deliveryId, UUID.randomUUID(), UUID.randomUUID(), DeliveryChannel.EMAIL, "NEW_LEAD", "recipient@test.com", "Lead",
                DeliveryStatus.NOT_CONFIGURED, "DisabledEmailProvider", null, 0, 3, "Provider disabled", null, null,
                Instant.now(), Instant.now(), null
        )));
        when(transactionalEmailService.isConfigured()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminService.retryCommunicationDelivery(adminActor, deliveryId, "127.0.0.1", "Agent"));
        assertTrue(ex.getMessage().contains("NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("Admin can reconcile media storage with audit")
    void adminCanReconcileMediaStorageWithAudit() {
        UUID studioId = UUID.randomUUID();
        when(mediaService.reconcileStorageForAdmin(adminActor, studioId)).thenReturn(new StorageReconciliationReport(
                studioId, 5, 0, 1000000L, 0L, 1000000L, 2, 5
        ));

        StorageReconciliationReport report = adminService.reconcileMediaStorage(adminActor, studioId, "127.0.0.1", "Agent");
        assertNotNull(report);
        assertEquals(2, report.expiredIntentsCount());

        verify(auditService).record(
                eq(adminActor.userId()),
                eq(studioId),
                eq("MEDIA_RECONCILIATION"),
                eq("MEDIA_STORAGE"),
                eq(studioId.toString()),
                anyMap(),
                eq("127.0.0.1"),
                eq("Agent")
        );
    }

    @Test
    @DisplayName("Admin can reconcile stuck AI jobs with audit")
    void adminCanReconcileAiJobsWithAudit() {
        when(aiVisualizerService.reconcileStuckProcessingJobs(any())).thenReturn(3);

        int count = adminService.reconcileStuckAiJobs(adminActor, "127.0.0.1", "Agent");
        assertEquals(3, count);

        verify(auditService).record(
                eq(adminActor.userId()),
                isNull(),
                eq("AI_RECONCILIATION"),
                eq("AI_JOBS"),
                eq("PLATFORM"),
                argThat(map -> Integer.valueOf(3).equals(map.get("recoveredCount"))),
                eq("127.0.0.1"),
                eq("Agent")
        );
    }

    @Test
    @DisplayName("Operational health reports truth without exposing secrets")
    void operationalHealthReportsTruthfulStatus() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(transactionalEmailService.isConfigured()).thenReturn(false);

        OperationalHealthDto health = adminService.getOperationalHealth(adminActor);
        assertNotNull(health);
        assertEquals("UP", health.database());
        assertEquals("NOT_CONFIGURED", health.emailProvider());
        assertEquals("DIRECT_WAME", health.whatsappMode());
    }
}
