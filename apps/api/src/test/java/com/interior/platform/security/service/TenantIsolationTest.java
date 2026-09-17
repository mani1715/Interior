package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantIsolationTest {

    private AuthorizationService authorizationService;

    private UUID studioAId;
    private UUID studioBId;
    private ActorContext actorStudioA;
    private ActorContext actorStudioB;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();

        studioAId = UUID.randomUUID();
        studioBId = UUID.randomUUID();

        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();

        actorStudioA = new ActorContext(
            userAId,
            "Designer Studio A",
            "designerA@studioA.com",
            Set.of("DESIGNER"),
            studioAId,
            "OWNER",
            true
        );

        actorStudioB = new ActorContext(
            userBId,
            "Designer Studio B",
            "designerB@studioB.com",
            Set.of("DESIGNER"),
            studioBId,
            "OWNER",
            true
        );
    }

    @Test
    @DisplayName("Studio A actor can access Studio A resources")
    void testStudioAAccessOwnResourceAllowed() {
        assertDoesNotThrow(() -> authorizationService.requireStudioAccess(actorStudioA, studioAId));
    }

    @Test
    @DisplayName("Studio A actor attempting to access Studio B resource MUST be DENIED")
    void testStudioAAccessStudioBResourceDenied() {
        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireStudioAccess(actorStudioA, studioBId)
        );
        assertTrue(exception.getMessage().contains("tenant isolation violation"));
    }

    @Test
    @DisplayName("Studio B actor attempting to access Studio A resource MUST be DENIED")
    void testStudioBAccessStudioAResourceDenied() {
        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireStudioAccess(actorStudioB, studioAId)
        );
        assertTrue(exception.getMessage().contains("tenant isolation violation"));
    }

    @Test
    @DisplayName("Unauthenticated request accessing tenant resource MUST be UNAUTHORIZED")
    void testUnauthenticatedAccessDenied() {
        ActorContext anon = ActorContext.anonymous();
        assertThrows(
            RuntimeException.class,
            () -> authorizationService.requireStudioAccess(anon, studioAId)
        );
    }
}
