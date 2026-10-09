package com.interior.platform.security;

import com.interior.platform.admin.dto.UpdateUserStatusRequest;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.media.service.ImageProcessingService;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.OidcService;
import com.interior.platform.security.service.RateLimiterService;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Phase 6 Adversarial Security Audit & Penetration Tests")
class AdversarialSecurityAuditTest {

    private static final String COOKIE_NAME = "__Host-session";

    private SecurityRepository securityRepository;
    private SessionSecurityService sessionSecurityService;
    private AuthorizationService authorizationService;
    private RateLimiterService rateLimiterService;
    private AuditService auditService;
    private SecurityInterceptor securityInterceptor;
    private ImageProcessingService imageProcessingService;

    private final Instant now = Instant.parse("2026-10-09T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        securityRepository = mock(SecurityRepository.class);
        authorizationService = new AuthorizationService();
        rateLimiterService = new RateLimiterService(clock);
        auditService = mock(AuditService.class);
        imageProcessingService = new ImageProcessingService();

        var authProps = new AuthSecurityProperties();
        sessionSecurityService = new SessionSecurityService(authProps, securityRepository, clock);

        securityInterceptor = new SecurityInterceptor(sessionSecurityService);
    }

    @Nested
    @DisplayName("1. Authentication & Session Fail-Closed Invariants")
    class AuthenticationAndSessionTests {

        @Test
        @DisplayName("Unauthenticated request with missing session cookie resolves to anonymous actor")
        void unauthenticatedRequestResolvesAnonymous() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/profile");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertTrue(proceed);

            ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
            assertNotNull(actor);
            assertFalse(actor.isAuthenticated());
        }

        @Test
        @DisplayName("Manipulated or forged session token fails authentication")
        void forgedSessionTokenRejected() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, "forged_malicious_session_token_12345"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.empty());

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertTrue(proceed);

            ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
            assertNotNull(actor);
            assertFalse(actor.isAuthenticated());
        }

        @Test
        @DisplayName("Suspended or deleted account session is rejected and returns anonymous")
        void suspendedUserSessionBlocked() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String rawToken = "valid_looking_token_67890";

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], new byte[32],
                    now.minus(300, ChronoUnit.SECONDS), "PASSWORD", now,
                    now.plus(3600, ChronoUnit.SECONDS), now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
            when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                    userId, "Suspended User", "suspended@example.com", null, "SUSPENDED",
                    now.minus(10, ChronoUnit.DAYS), now, 1L, null, null, null
            )));

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertTrue(proceed);

            ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
            assertNotNull(actor);
            assertFalse(actor.isAuthenticated(), "Suspended account must never be authenticated");
        }

        @Test
        @DisplayName("Idle expired session fails authentication and actor remains anonymous")
        void idleExpiredSessionRejected() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String rawToken = "expired_idle_token";

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], new byte[32],
                    now.minus(7200, ChronoUnit.SECONDS), "PASSWORD", now.minus(3600, ChronoUnit.SECONDS),
                    now.minus(60, ChronoUnit.SECONDS), // idle expired 60s ago
                    now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertTrue(proceed);

            ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
            assertNotNull(actor);
            assertFalse(actor.isAuthenticated(), "Idle expired session must not authenticate actor");
        }
    }

    @Nested
    @DisplayName("2. CSRF Enforcement & Timing Attacks")
    class CsrfSecurityTests {

        @Test
        @DisplayName("Mutating POST request without CSRF header is rejected with 403 Forbidden")
        void mutatingPostWithoutCsrfHeaderBlocked() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String rawToken = "valid_session_token";
            String rawCsrf = "valid_csrf_token";
            byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], csrfHash,
                    now.minus(300, ChronoUnit.SECONDS), "PASSWORD", now,
                    now.plus(3600, ChronoUnit.SECONDS), now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
            when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                    userId, "Active User", "active@example.com", null, "ACTIVE",
                    now.minus(10, ChronoUnit.DAYS), now, 1L, null, null, null
            )));

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertFalse(proceed, "Mutating request without CSRF token must be blocked");
            assertEquals(403, response.getStatus());
        }

        @Test
        @DisplayName("Mutating POST request with invalid CSRF token is rejected with 403 Forbidden")
        void mutatingPostWithMismatchedCsrfTokenBlocked() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String rawToken = "valid_session_token";
            String rawCsrf = "legitimate_csrf_token";
            byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], csrfHash,
                    now.minus(300, ChronoUnit.SECONDS), "PASSWORD", now,
                    now.plus(3600, ChronoUnit.SECONDS), now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
            when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                    userId, "Active User", "active@example.com", null, "ACTIVE",
                    now.minus(10, ChronoUnit.DAYS), now, 1L, null, null, null
            )));

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            request.addHeader(SecurityInterceptor.CSRF_HEADER, "forged_csrf_attacker_token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertFalse(proceed, "Mutating request with mismatched CSRF token must be blocked");
            assertEquals(403, response.getStatus());
        }

        @Test
        @DisplayName("Valid CSRF token with authentic session passes validation")
        void validCsrfTokenPasses() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String rawToken = "valid_session_token";
            String rawCsrf = "valid_csrf_token_secret";
            byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], csrfHash,
                    now.minus(300, ChronoUnit.SECONDS), "PASSWORD", now,
                    now.plus(3600, ChronoUnit.SECONDS), now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
            when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                    userId, "Active User", "active@example.com", null, "ACTIVE",
                    now.minus(10, ChronoUnit.DAYS), now, 1L, null, null, null
            )));

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/account/profile");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            request.addHeader(SecurityInterceptor.CSRF_HEADER, rawCsrf);
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertTrue(proceed);
            assertEquals(200, response.getStatus());
        }
    }

    @Nested
    @DisplayName("3. Multi-Tenant Studio Isolation & IDOR Protection")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Accessing foreign studio via X-Studio-Id header is rejected with 403 Forbidden")
        void foreignStudioHeaderRejected() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID userStudioId = UUID.randomUUID();
            UUID foreignStudioId = UUID.randomUUID();
            String rawToken = "valid_session_token";

            SessionRecord session = new SessionRecord(
                    sessionId, userId, new byte[32], new byte[32],
                    now.minus(300, ChronoUnit.SECONDS), "PASSWORD", now,
                    now.plus(3600, ChronoUnit.SECONDS), now.plus(86400, ChronoUnit.SECONDS),
                    null, "Agent"
            );

            when(securityRepository.findSessionByTokenHash(any())).thenReturn(Optional.of(session));
            when(securityRepository.findUserById(userId)).thenReturn(Optional.of(new UserRecord(
                    userId, "Studio Member", "member@studio.com", null, "ACTIVE",
                    now.minus(10, ChronoUnit.DAYS), now, 1L, null, null, null
            )));
            when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(
                    new StudioMemberRecord(
                            UUID.randomUUID(), userStudioId, "User's Studio", "users-studio", userId, "DESIGNER", now
                    )
            ));

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/projects");
            request.setCookies(new Cookie(COOKIE_NAME, rawToken));
            request.addHeader("X-Studio-Id", foreignStudioId.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean proceed = securityInterceptor.preHandle(request, response, new Object());
            assertFalse(proceed, "User must be blocked from context-switching into a foreign studio");
            assertEquals(403, response.getStatus());
        }
    }

    @Nested
    @DisplayName("4. Open Redirect Prevention")
    class OpenRedirectTests {

        @Test
        @DisplayName("Dangerous external redirect targets are sanitized to root '/'")
        void maliciousRedirectUrlsSanitized() {
            assertEquals("/", OidcService.sanitizeReturnUrl("https://evil.com/phish"));
            assertEquals("/", OidcService.sanitizeReturnUrl("//evil.com"));
            assertEquals("/", OidcService.sanitizeReturnUrl("javascript:alert(1)"));
            assertEquals("/", OidcService.sanitizeReturnUrl("data:text/html;base64,PHNjcmlwdD4="));
            assertEquals("/", OidcService.sanitizeReturnUrl("http://attacker.org"));
            assertEquals("/", OidcService.sanitizeReturnUrl("/\\attacker.com"));
            assertEquals("/", OidcService.sanitizeReturnUrl("/%2fevil.com"));
            assertEquals("/", OidcService.sanitizeReturnUrl("/%5cevil.com"));
            assertEquals("/", OidcService.sanitizeReturnUrl("/test\r\nSet-Cookie: evil=true"));
        }

        @Test
        @DisplayName("Valid relative application URLs are preserved")
        void safeReturnUrlsPreserved() {
            assertEquals("/workspace/projects", OidcService.sanitizeReturnUrl("/workspace/projects"));
            assertEquals("/explore?style=modern", OidcService.sanitizeReturnUrl("/explore?style=modern"));
            assertEquals("/studios/luxe-interiors", OidcService.sanitizeReturnUrl("/studios/luxe-interiors"));
        }
    }

    @Nested
    @DisplayName("5. File Upload Magic Byte Enforcement")
    class UploadSecurityTests {

        @Test
        @DisplayName("Non-image and script polyglot byte payloads fail dimension validation")
        void rejectNonImagePayloads() {
            byte[] shellScript = "#!/bin/bash\necho 'pwnd'".getBytes(StandardCharsets.UTF_8);
            byte[] htmlXss = "<script>alert('xss')</script>".getBytes(StandardCharsets.UTF_8);
            byte[] randomGarbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            assertThrows(IllegalArgumentException.class, () -> imageProcessingService.validateAndGetDimensions(shellScript));
            assertThrows(IllegalArgumentException.class, () -> imageProcessingService.validateAndGetDimensions(htmlXss));
            assertThrows(IllegalArgumentException.class, () -> imageProcessingService.validateAndGetDimensions(randomGarbage));
            assertThrows(IllegalArgumentException.class, () -> imageProcessingService.validateAndGetDimensions(new byte[0]));
        }

        @Test
        @DisplayName("Valid PNG byte payload passes magic byte and dimension validation")
        void validPngSucceeds() {
            // Minimal valid 1x1 PNG
            byte[] valid1x1Png = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                    (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                    0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                    0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                    0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                    0x42, 0x60, (byte) 0x82
            };

            ImageProcessingService.ImageDimensions dims = imageProcessingService.validateAndGetDimensions(valid1x1Png);
            assertNotNull(dims);
            assertEquals(1, dims.width());
            assertEquals(1, dims.height());
            assertEquals("png", dims.format());
        }
    }

    @Nested
    @DisplayName("6. Role-Based Privilege Escalation Protection")
    class PrivilegeEscalationTests {

        @Test
        @DisplayName("Customer or Studio role cannot invoke AdminService operations")
        void nonAdminBlockedFromAdminService() {
            AdminService adminService = new AdminService(
                    mock(com.interior.platform.admin.repository.AdminRepository.class),
                    authorizationService,
                    auditService,
                    rateLimiterService
            );

            ActorContext customer = new ActorContext(
                    UUID.randomUUID(), "Customer", "c@test.com",
                    Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
            );

            ActorContext studioOwner = new ActorContext(
                    UUID.randomUUID(), "Studio Owner", "owner@studio.com",
                    Set.of("STUDIO_MEMBER"), Set.of(), UUID.randomUUID(), "OWNER", "PASSWORD", true
            );

            assertThrows(AccessDeniedException.class, () -> adminService.getDashboardMetrics(customer));
            assertThrows(AccessDeniedException.class, () -> adminService.listUsers(customer, 50, 0, null));
            assertThrows(AccessDeniedException.class, () -> adminService.updateUserStatus(studioOwner, UUID.randomUUID(), new UpdateUserStatusRequest("SUSPENDED", "Test"), "127.0.0.1", "Agent"));
        }
    }
}
