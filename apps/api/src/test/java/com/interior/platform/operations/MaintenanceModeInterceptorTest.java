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
