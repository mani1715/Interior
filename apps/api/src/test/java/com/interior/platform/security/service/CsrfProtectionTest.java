package com.interior.platform.security.service;

import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CsrfProtectionTest {

    private SessionSecurityService sessionSecurityService;
    private com.interior.platform.security.repository.SecurityRepository securityRepository;
    private SecurityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        securityRepository = mock(com.interior.platform.security.repository.SecurityRepository.class);
        var props = new com.interior.platform.security.config.AuthSecurityProperties();
        sessionSecurityService = new SessionSecurityService(props, securityRepository, java.time.Clock.systemUTC());
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
        when(request.getHeader("X-CSRF-Token")).thenReturn(null);

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                Instant.now(), "PASSWORD", Instant.now(), Instant.now().plusSeconds(1800), Instant.now().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", Instant.now(), Instant.now(), 0L);

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
        when(request.getHeader("X-CSRF-Token")).thenReturn(rawCsrf);

        UUID userId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), userId, tokenHash, csrfHash,
                Instant.now(), "PASSWORD", Instant.now(), Instant.now().plusSeconds(1800), Instant.now().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(userId, "Test User", "test@example.com", null, "ACTIVE", Instant.now(), Instant.now(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of());

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }
}
