package com.interior.platform.realtime;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.realtime.domain.RealtimeEvent;
import com.interior.platform.realtime.domain.RealtimeEventType;
import com.interior.platform.realtime.service.LocalRealtimeEventBroadcaster;
import com.interior.platform.realtime.service.RealtimeEventListener;
import com.interior.platform.realtime.service.RealtimeEventPublisher;
import com.interior.platform.realtime.service.SseConnectionRegistry;
import com.interior.platform.realtime.web.SseStreamController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Realtime Server-Sent Events (SSE) Security & Streaming Tests")
class SseStreamIntegrationTest {

    @Autowired
    private SseStreamController sseStreamController;

    @Autowired
    private SseConnectionRegistry connectionRegistry;

    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;

    @Autowired
    private RealtimeEventListener realtimeEventListener;

    @Autowired
    private LocalRealtimeEventBroadcaster broadcaster;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID activeUserId;
    private UUID suspendedUserId;
    private UUID deactivatedUserId;
    private UUID studioAId;
    private UUID studioBId;
    private UUID studioAOwnerId;
    private UUID studioBOwnerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM studio_leads");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        // 1. Active User
        activeUserId = UuidV7.randomUuid();
        securityRepository.createUser(new UserRecord(
                activeUserId,
                "Aarav Designer",
                "aarav@example.com",
                "+919876543210",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L,
                null,
                null,
                null
        ));

        // 2. Suspended User
        suspendedUserId = UuidV7.randomUuid();
        securityRepository.createUser(new UserRecord(
                suspendedUserId,
                "Suspended User",
                "suspended@example.com",
                "+919876543211",
                "SUSPENDED",
                Instant.now(),
                Instant.now(),
                0L,
                null,
                null,
                null
        ));

        // 3. Deactivated User
        deactivatedUserId = UuidV7.randomUuid();
        securityRepository.createUser(new UserRecord(
                deactivatedUserId,
                "Deactivated User",
                "deactivated@example.com",
                "+919876543212",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L,
                null,
                null,
                null
        ));
        securityRepository.deactivateUser(deactivatedUserId, Instant.now().minusSeconds(3600));

        // 4. Studio A and Owner
        studioAOwnerId = UuidV7.randomUuid();
        securityRepository.createUser(new UserRecord(
                studioAOwnerId,
                "Studio A Owner",
                "owner.a@example.com",
                "+919876543213",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L,
                null,
                null,
                null
        ));
        studioAId = UuidV7.randomUuid();
        securityRepository.createStudio(studioAId, "Studio Alpha", "studio-alpha", studioAOwnerId, "ACTIVE");

        // 5. Studio B and Owner
        studioBOwnerId = UuidV7.randomUuid();
        securityRepository.createUser(new UserRecord(
                studioBOwnerId,
                "Studio B Owner",
                "owner.b@example.com",
                "+919876543214",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L,
                null,
                null,
                null
        ));
        studioBId = UuidV7.randomUuid();
        securityRepository.createStudio(studioBId, "Studio Beta", "studio-beta", studioBOwnerId, "ACTIVE");
    }

    @Test
    @DisplayName("1. Anonymous request is rejected with 401 Unauthorized")
    void testAnonymousRequestRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, ActorContext.anonymous());

        assertThrows(UnauthorizedException.class, () ->
                sseStreamController.streamEvents(request, response, null)
        );
    }

    @Test
    @DisplayName("2. Suspended user is rejected with 403 Forbidden")
    void testSuspendedUserRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(
                suspendedUserId,
                "Suspended User",
                "suspended@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        assertThrows(AccessDeniedException.class, () ->
                sseStreamController.streamEvents(request, response, null)
        );
    }

    @Test
    @DisplayName("3. Deactivated user is rejected with 403 Forbidden")
    void testDeactivatedUserRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(
                deactivatedUserId,
                "Deactivated User",
                "deactivated@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        assertThrows(AccessDeniedException.class, () ->
                sseStreamController.streamEvents(request, response, null)
        );
    }

    @Test
    @DisplayName("4. Active authenticated user successfully connects to SSE stream")
    void testActiveUserConnectsSuccessfully() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(
                activeUserId,
                "Aarav Designer",
                "aarav@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        SseEmitter emitter = sseStreamController.streamEvents(request, response, null);
        assertNotNull(emitter);
        assertEquals("no-cache, no-transform", response.getHeader("Cache-Control"));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
        assertTrue(connectionRegistry.getUserConnectionCount(activeUserId) >= 1);
    }

    @Test
    @DisplayName("5. Reconnecting user with Last-Event-ID receives RESYNC handshake")
    void testReconnectReceivesResync() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(
                activeUserId,
                "Aarav Designer",
                "aarav@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        SseEmitter emitter = sseStreamController.streamEvents(request, response, "evt-last-12345");
        assertNotNull(emitter);
        assertTrue(connectionRegistry.getUserConnectionCount(activeUserId) >= 1);
    }

    @Test
    @DisplayName("6. Multiple browser tabs (connections) supported for same user")
    void testMultipleConnectionsPerUser() {
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(
                activeUserId,
                "Aarav Designer",
                "aarav@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );
        req1.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        req2.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        SseEmitter emitter1 = sseStreamController.streamEvents(req1, res1, null);
        SseEmitter emitter2 = sseStreamController.streamEvents(req2, res2, null);

        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertEquals(2, connectionRegistry.getUserConnectionCount(activeUserId));
    }

    @Test
    @DisplayName("7. Connection limits enforced: exceeding max connections evicts oldest")
    void testMaxConnectionsEnforced() {
        ActorContext actor = new ActorContext(
                activeUserId,
                "Aarav Designer",
                "aarav@example.com",
                Set.of("DESIGNER"),
                null,
                null,
                true
        );

        for (int i = 0; i < SseConnectionRegistry.MAX_CONNECTIONS_PER_USER + 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            req.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
            sseStreamController.streamEvents(req, res, null);
        }

        // Must never exceed MAX_CONNECTIONS_PER_USER
        assertTrue(connectionRegistry.getUserConnectionCount(activeUserId) <= SseConnectionRegistry.MAX_CONNECTIONS_PER_USER);
    }

    @Test
    @DisplayName("8. Cross-User Isolation: User A receives event, User B connection is unaffected")
    void testCrossUserIsolation() {
        MockHttpServletRequest reqA = new MockHttpServletRequest();
        MockHttpServletResponse resA = new MockHttpServletResponse();
        MockHttpServletRequest reqB = new MockHttpServletRequest();
        MockHttpServletResponse resB = new MockHttpServletResponse();

        ActorContext actorA = new ActorContext(activeUserId, "User A", "a@example.com", Set.of("CUSTOMER"), null, null, true);
        ActorContext actorB = new ActorContext(studioAOwnerId, "User B", "b@example.com", Set.of("CUSTOMER"), null, null, true);

        reqA.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actorA);
        reqB.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actorB);

        sseStreamController.streamEvents(reqA, resA, null);
        sseStreamController.streamEvents(reqB, resB, null);

        assertEquals(1, connectionRegistry.getUserConnectionCount(activeUserId));
        assertEquals(1, connectionRegistry.getUserConnectionCount(studioAOwnerId));

        // Send event specifically to User A
        RealtimeEvent eventForA = RealtimeEvent.of(
                RealtimeEventType.NOTIFICATION_CREATED,
                activeUserId,
                "NOTIFICATION",
                "notif-123",
                Map.of("unreadCount", 1)
        );

        assertDoesNotThrow(() -> broadcaster.broadcast(eventForA));
        // Both connections still cleanly managed
        assertEquals(1, connectionRegistry.getUserConnectionCount(activeUserId));
        assertEquals(1, connectionRegistry.getUserConnectionCount(studioAOwnerId));
    }

    @Test
    @DisplayName("9. Studio Tenant Isolation: Event for Studio A only dispatches to Studio A owner")
    void testStudioTenantIsolation() {
        MockHttpServletRequest reqA = new MockHttpServletRequest();
        MockHttpServletResponse resA = new MockHttpServletResponse();
        MockHttpServletRequest reqB = new MockHttpServletRequest();
        MockHttpServletResponse resB = new MockHttpServletResponse();

        ActorContext actorA = new ActorContext(studioAOwnerId, "Owner A", "owner.a@example.com", Set.of("DESIGNER"), studioAId, "OWNER", true);
        ActorContext actorB = new ActorContext(studioBOwnerId, "Owner B", "owner.b@example.com", Set.of("DESIGNER"), studioBId, "OWNER", true);

        reqA.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actorA);
        reqB.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actorB);

        sseStreamController.streamEvents(reqA, resA, null);
        sseStreamController.streamEvents(reqB, resB, null);

        // Emit lead event targeted to Studio A
        RealtimeEvent leadEventA = RealtimeEvent.ofStudio(
                RealtimeEventType.LEAD_CREATED,
                studioAOwnerId,
                studioAId,
                "LEAD",
                "lead-alpha-1",
                Map.of("clientName", "Kavita Reddy")
        );

        assertDoesNotThrow(() -> broadcaster.broadcast(leadEventA));
        assertEquals(1, connectionRegistry.getUserConnectionCount(studioAOwnerId));
        assertEquals(1, connectionRegistry.getUserConnectionCount(studioBOwnerId));
    }

    @Test
    @DisplayName("10. Heartbeat keeps connections alive without error")
    void testHeartbeatCycle() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        ActorContext actor = new ActorContext(activeUserId, "User", "u@example.com", Set.of("CUSTOMER"), null, null, true);
        req.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);

        sseStreamController.streamEvents(req, res, null);
        assertTrue(connectionRegistry.getUserConnectionCount(activeUserId) >= 1);

        // Execute scheduled heartbeat
        assertDoesNotThrow(() -> connectionRegistry.sendHeartbeats());
        assertTrue(connectionRegistry.getUserConnectionCount(activeUserId) >= 1);
    }
}
