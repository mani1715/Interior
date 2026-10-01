package com.interior.platform.admin;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.repository.AdminRepository;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
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

        adminService = new AdminService(adminRepository, authorizationService, auditService, rateLimiterService);

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
    @DisplayName("Admin can suspend user and audit event is recorded")
    void adminCanSuspendUserWithAudit() {
        UUID targetUserId = UUID.randomUUID();
        UpdateUserStatusRequest req = new UpdateUserStatusRequest("SUSPENDED", "Policy violation");

        adminService.updateUserStatus(adminActor, targetUserId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateUserStatus(targetUserId, "SUSPENDED");
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
    @DisplayName("Admin can suspend studio and audit event is recorded")
    void adminCanSuspendStudioWithAudit() {
        UUID targetStudioId = UUID.randomUUID();
        UpdateStudioStatusRequest req = new UpdateStudioStatusRequest("SUSPENDED", "Fraud investigation");

        adminService.updateStudioStatus(adminActor, targetStudioId, req, "127.0.0.1", "TestAgent");

        verify(adminRepository).updateStudioStatus(targetStudioId, "SUSPENDED");
        verify(auditService).record(
                eq(adminActor.userId()),
                eq(targetStudioId),
                eq("STUDIO_STATUS_CHANGED"),
                eq("STUDIO"),
                eq(targetStudioId.toString()),
                argThat(map -> "SUSPENDED".equals(map.get("newStatus"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Admin can moderate review and audit event is recorded")
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
                argThat(map -> "REMOVED".equals(map.get("newStatus"))),
                eq("127.0.0.1"),
                eq("TestAgent")
        );
    }

    @Test
    @DisplayName("Non-admin cannot suspend user or studio")
    void nonAdminCannotMutateAdminState() {
        UUID targetId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                adminService.updateUserStatus(studioOwnerActor, targetId, new UpdateUserStatusRequest("SUSPENDED", "test"), "1.1.1.1", "agent"));

        assertThrows(AccessDeniedException.class, () ->
                adminService.updateStudioStatus(customerActor, targetId, new UpdateStudioStatusRequest("SUSPENDED", "test"), "1.1.1.1", "agent"));
    }
}
