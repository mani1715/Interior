package com.interior.platform.operations;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.operations.domain.PlatformOperationalStateRecord;
import com.interior.platform.operations.interceptor.MaintenanceModeInterceptor;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9G — Maintenance Mode Interceptor Unit Tests")
class MaintenanceModeInterceptorTest {

    @Mock private PlatformOperationsService operationsService;

    private MaintenanceModeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new MaintenanceModeInterceptor(operationsService);
    }

    @Test
    @DisplayName("Safe GET requests are allowed even in READ_ONLY maintenance mode")
    void preHandle_safeGet_allowedInReadOnly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Mutating POST requests by regular users are rejected with 503 in READ_ONLY mode")
    void preHandle_mutatingPost_rejectedInReadOnly() throws Exception {
        when(operationsService.isMaintenanceReadOnly()).thenReturn(true);
        when(operationsService.getOperationalState()).thenReturn(
                new PlatformOperationalStateRecord("GLOBAL", "READ_ONLY", "Scheduled maintenance", false, false, false, null, Instant.now())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects");
        ActorContext userActor = new ActorContext(UuidV7.randomUuid(), "User", "user@test.com", "ACTIVE", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true, null);
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, userActor);

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("MAINTENANCE_MODE_ACTIVE"));
    }

    @Test
    @DisplayName("Mutating POST requests by ADMIN are allowed through in READ_ONLY mode")
    void preHandle_mutatingPost_adminAllowedInReadOnly() throws Exception {
        when(operationsService.isMaintenanceReadOnly()).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/operations/state");
        ActorContext adminActor = new ActorContext(UuidV7.randomUuid(), "Admin", "admin@test.com", "ACTIVE", Set.of("ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, adminActor);

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    @DisplayName("Ordinary product mutations by SUPER_ADMIN are blocked in READ_ONLY mode")
    void preHandle_ordinaryProductMutationBySuperAdmin_blockedInReadOnly() throws Exception {
        when(operationsService.isMaintenanceReadOnly()).thenReturn(true);
        when(operationsService.getOperationalState()).thenReturn(
                new PlatformOperationalStateRecord("GLOBAL", "READ_ONLY", "Scheduled maintenance", false, false, false, null, Instant.now())
        );

        String[] blockedEndpoints = {
                "/api/v1/projects",
                "/api/v1/projects/01a12b11-48ba-7617-8fb3-07a741349f8d",
                "/api/v1/projects/01a12b11-48ba-7617-8fb3-07a741349f8d/publish",
                "/api/v1/media/upload-intent",
                "/api/v1/media/upload-commit",
                "/api/v1/ai/generate",
                "/api/v1/leads",
                "/api/v1/studios/team/invitations",
                "/api/v1/billing/checkout",
                "/api/v1/support/tickets"
        };

        ActorContext superAdminActor = new ActorContext(UuidV7.randomUuid(), "SuperAdmin", "super@test.com", "ACTIVE", Set.of("SUPER_ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());

        for (String endpoint : blockedEndpoints) {
            String method = endpoint.contains("01a12b11-48ba-7617-8fb3-07a741349f8d") && !endpoint.endsWith("publish") ? "PATCH" : "POST";
            MockHttpServletRequest request = new MockHttpServletRequest(method, endpoint);
            request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, superAdminActor);
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());
            assertFalse(result, "Super Admin mutation should be blocked at: " + endpoint);
            assertEquals(503, response.getStatus(), "Should return 503 for: " + endpoint);
            assertTrue(response.getContentAsString().contains("MAINTENANCE_MODE_ACTIVE"), "Should return MAINTENANCE_MODE_ACTIVE for: " + endpoint);
        }
    }

    @Test
    @DisplayName("Allowed operational control-plane paths pass for ADMIN / SUPER_ADMIN in READ_ONLY mode")
    void preHandle_allowedControlPlanePaths_passInReadOnly() throws Exception {
        when(operationsService.isMaintenanceReadOnly()).thenReturn(true);

        String[] allowedPaths = {
                "/api/v1/admin/operations/maintenance-mode",
                "/api/v1/admin/operations/state",
                "/api/v1/admin/mfa/reset",
                "/api/v1/admin/audit/events",
                "/api/v1/admin/users/01a12b11-48ba-7617-8fb3-07a741349f8d/status",
                "/api/v1/admin/studios/01a12b11-48ba-7617-8fb3-07a741349f8d/moderation",
                "/api/v1/auth/logout",
                "/api/v1/public/webhooks/billing",
                "/actuator/health"
        };

        ActorContext superAdminActor = new ActorContext(UuidV7.randomUuid(), "SuperAdmin", "super@test.com", "ACTIVE", Set.of("SUPER_ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());

        for (String path : allowedPaths) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, superAdminActor);
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());
            assertTrue(result, "Control-plane or emergency operational path should be permitted: " + path);
        }
    }

    @Test
    @DisplayName("Logout request passes in READ_ONLY mode so users can safely terminate session")
    void preHandle_logout_allowedInReadOnly() throws Exception {
        when(operationsService.isMaintenanceReadOnly()).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        ActorContext userActor = new ActorContext(UuidV7.randomUuid(), "User", "user@test.com", "ACTIVE", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true, null);
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, userActor);

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }
}
