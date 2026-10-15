package com.interior.platform.security.service;

import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OidcDestinationRoutingTest {

    private AuthSecurityProperties properties;
    private SecurityRepository securityRepository;
    private Clock clock;
    private SessionSecurityService sessionSecurityService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
        properties = new AuthSecurityProperties();
        properties.setSessionIdleTimeoutSeconds(1800);
        properties.setSessionAbsoluteTimeoutSeconds(43200);
        properties.setSessionLastSeenUpdateIntervalSeconds(300);
        properties.setSessionCookieSecure(true);

        securityRepository = mock(SecurityRepository.class);
        sessionSecurityService = new SessionSecurityService(properties, securityRepository, clock);
    }

    @Test
    @DisplayName("Session creation establishes HttpOnly, Secure, SameSite=Lax cookie and persistent record")
    void testSessionCreation() {
        UUID userId = UUID.randomUUID();
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = sessionSecurityService.createAndPersistSession(userId, "PASSWORD", "Test Device", response);

        assertNotNull(result);
        assertNotNull(result.rawSessionToken());
        assertNotNull(result.rawCsrfToken());
        assertEquals(userId, result.sessionRecord().userId());

        // Verify Set-Cookie header
        String cookieHeader = response.getHeader("Set-Cookie");
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("__Host-session="));
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertTrue(cookieHeader.contains("SameSite=Lax"));
        assertTrue(cookieHeader.contains("Secure"));

        verify(securityRepository).createSession(any(SessionRecord.class));
    }

    @Test
    @DisplayName("Session validation rejects inactive or suspended accounts")
    void testSessionRejectsSuspendedUser() {
        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(
                UUID.randomUUID(),
                userId,
                new byte[32],
                new byte[32],
                clock.instant(),
                "PASSWORD",
                clock.instant(),
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                null,
                "Device"
        );

        UserRecord suspendedUser = new UserRecord(
                userId,
                "Suspended User",
                "suspended@example.com",
                null,
                "SUSPENDED",
                clock.instant(),
                clock.instant(),
                0L
        );

        when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(suspendedUser));

        Optional<SessionSecurityService.ValidatedSession> validated =
                sessionSecurityService.validateSession("raw-token-123");

        assertTrue(validated.isEmpty(), "Suspended user session must not validate");
    }

    @Test
    @DisplayName("Session validation rejects expired session (idle timeout)")
    void testSessionRejectsIdleExpiredSession() {
        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(
                UUID.randomUUID(),
                userId,
                new byte[32],
                new byte[32],
                clock.instant().minusSeconds(3600),
                "PASSWORD",
                clock.instant().minusSeconds(1900),
                clock.instant().minusSeconds(100), // Idle expired 100s ago
                clock.instant().plusSeconds(30000),
                null,
                "Device"
        );

        when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));

        Optional<SessionSecurityService.ValidatedSession> validated =
                sessionSecurityService.validateSession("raw-token-123");

        assertTrue(validated.isEmpty(), "Idle expired session must not validate");
    }

    @Test
    @DisplayName("Session validation rejects absolute expired session")
    void testSessionRejectsAbsoluteExpiredSession() {
        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(
                UUID.randomUUID(),
                userId,
                new byte[32],
                new byte[32],
                clock.instant().minusSeconds(50000),
                "PASSWORD",
                clock.instant(),
                clock.instant().plusSeconds(1800),
                clock.instant().minusSeconds(1), // Absolute expired!
                null,
                "Device"
        );

        when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));

        Optional<SessionSecurityService.ValidatedSession> validated =
                sessionSecurityService.validateSession("raw-token-123");

        assertTrue(validated.isEmpty(), "Absolute expired session must not validate");
    }

    @Test
    @DisplayName("Throttled last_seen update: Updates idle timeout only after interval threshold")
    void testThrottledLastSeenUpdate() {
        UUID userId = UUID.randomUUID();
        UserRecord activeUser = new UserRecord(userId, "Active", "active@example.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);

        // Last seen 350 seconds ago (interval is 300s -> should update)
        SessionRecord session = new SessionRecord(
                UUID.randomUUID(),
                userId,
                new byte[32],
                new byte[32],
                clock.instant().minusSeconds(1000),
                "PASSWORD",
                clock.instant().minusSeconds(350),
                clock.instant().plusSeconds(1450),
                clock.instant().plusSeconds(40000),
                null,
                "Device"
        );

        when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));

        sessionSecurityService.validateSession("raw-token-123");

        // Verify update called
        verify(securityRepository).updateSessionLastSeen(eq(session.id()), eq(clock.instant()), any());
    }

    @Test
    @DisplayName("Logout revokes session in database and clears session cookie")
    void testLogoutRevocation() {
        UUID sessionId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(
                sessionId,
                UUID.randomUUID(),
                new byte[32],
                new byte[32],
                clock.instant(),
                "PASSWORD",
                clock.instant(),
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                null,
                "Device"
        );

        when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));

        MockHttpServletResponse response = new MockHttpServletResponse();
        sessionSecurityService.logout("raw-token-logout", response);

        verify(securityRepository).revokeSession(eq(sessionId), any());

        // Verify cookie cleared with Max-Age=0
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(c -> c.contains("__Host-session=") && c.contains("Max-Age=0")));
    }
}
