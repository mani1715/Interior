package com.interior.platform.security.service;

import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CsrfProtectionTest {

    private SessionSecurityService sessionSecurityService;
    private SecurityRepository securityRepository;
    private SecurityInterceptor interceptor;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);
        securityRepository = mock(SecurityRepository.class);
        var props = new com.interior.platform.security.config.AuthSecurityProperties();
        sessionSecurityService = new SessionSecurityService(props, securityRepository, clock);
        interceptor = new SecurityInterceptor(sessionSecurityService);
        ReflectionTestUtils.setField(interceptor, "sessionCookieName", "__Host-session");
    }

    @Test
    @DisplayName("GET request does not require CSRF token")
    void testGetWithoutCsrfAllowed() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    @DisplayName("Authenticated POST without CSRF header is rejected with 403")
    void testAuthenticatedPostWithoutCsrfRejected() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        String rawCsrf = sessionSecurityService.generateCsrfToken();
        byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

        Cookie sessionCookie = new Cookie("__Host-session", rawToken);
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/account/update");
        when(request.getHeader("X-CSRF-Token")).thenReturn(null);

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of());

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token Missing");
    }

    @Test
    @DisplayName("Authenticated POST with valid CSRF header succeeds")
    void testAuthenticatedPostWithValidCsrfAllowed() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        String rawCsrf = sessionSecurityService.generateCsrfToken();
        byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

        Cookie sessionCookie = new Cookie("__Host-session", rawToken);
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/account/update");
        when(request.getHeader("X-CSRF-Token")).thenReturn(rawCsrf);

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of());

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    @DisplayName("Cross-session forgery rejection: CSRF token from Session A cannot validate on Session B")
    void testCrossSessionCsrfRejection() {
        String csrfTokenSessionA = sessionSecurityService.generateCsrfToken();
        byte[] csrfHashSessionA = sessionSecurityService.hashToken(csrfTokenSessionA);

        String csrfTokenSessionB = sessionSecurityService.generateCsrfToken();
        byte[] csrfHashSessionB = sessionSecurityService.hashToken(csrfTokenSessionB);

        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        SessionRecord sessionA = new SessionRecord(UUID.randomUUID(), userA, new byte[32], csrfHashSessionA,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");

        SessionRecord sessionB = new SessionRecord(UUID.randomUUID(), userB, new byte[32], csrfHashSessionB,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");

        // Session A token validates on Session A
        assertTrue(sessionSecurityService.verifyCsrfToken(csrfTokenSessionA, sessionA));

        // Session A token FAILS on Session B
        assertFalse(sessionSecurityService.verifyCsrfToken(csrfTokenSessionA, sessionB));
    }

    @Test
    @DisplayName("POST /auth/logout requires valid CSRF when authenticated")
    void testLogoutRequiresValidCsrf() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        String rawCsrf = sessionSecurityService.generateCsrfToken();
        byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

        Cookie sessionCookie = new Cookie("__Host-session", rawToken);
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/logout");

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of());

        // Attempt 1: No CSRF header on POST /auth/logout
        when(request.getHeader("X-CSRF-Token")).thenReturn(null);
        boolean result1 = interceptor.preHandle(request, response, new Object());
        assertFalse(result1);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token Missing");

        // Attempt 2: Valid CSRF header on POST /auth/logout
        when(request.getHeader("X-CSRF-Token")).thenReturn(rawCsrf);
        boolean result2 = interceptor.preHandle(request, response, new Object());
        assertTrue(result2);
    }

    @Test
    @DisplayName("POST /auth/revoke-all requires valid CSRF when authenticated")
    void testRevokeAllRequiresValidCsrf() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        String rawCsrf = sessionSecurityService.generateCsrfToken();
        byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

        Cookie sessionCookie = new Cookie("__Host-session", rawToken);
        when(request.getCookies()).thenReturn(new Cookie[]{sessionCookie});
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/revoke-all");

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                clock.instant(), "PASSWORD", clock.instant(), clock.instant().plusSeconds(1800), clock.instant().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of());

        // Attempt 1: Wrong CSRF token
        when(request.getHeader("X-CSRF-Token")).thenReturn("forged-csrf-token");
        boolean result1 = interceptor.preHandle(request, response, new Object());
        assertFalse(result1);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");

        // Attempt 2: Correct CSRF token
        when(request.getHeader("X-CSRF-Token")).thenReturn(rawCsrf);
        boolean result2 = interceptor.preHandle(request, response, new Object());
        assertTrue(result2);
    }
}
